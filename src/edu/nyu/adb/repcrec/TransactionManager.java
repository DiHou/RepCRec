package edu.nyu.adb.repcrec;

import java.util.HashMap;
import java.util.List;

class TransactionManager {
  HashMap<String, Transaction> transactionMapping;
  SimulatedSite[] sites;

  void initialize() {
    transactionMapping = new HashMap<String, Transaction>();
    sites = new SimulatedSite[10];
    
    for (int i = 0; i < 10; i++) {
      sites[i] = new SimulatedSite(i + 1, this);
      
      for (int j = 1; j <= 20; j++) {
        String item = "x" + j;
        ItemInfo itemInfo = new ItemInfo(item, 10 * j);
        
        if (j % 2 == 0) {
          sites[i].database.put(item, itemInfo);
        } else if (j % 10 == i) {
          sites[i].database.put(item, itemInfo);
        }
      }
    }
  }

  boolean isReplicated(String name) {
    int index = Integer.parseInt(name.substring(1, name.length()));
    return index % 2 == 0 ? true : false;
  }

  void begin(String name, int time, boolean isReadOnly) {
    transactionMapping.put(name, new Transaction(name, time, isReadOnly, this));
  }

  void read(String transactionName, String key) {
    Transaction transaction = transactionMapping.get(transactionName);
    if (transaction == null) {
      return;
    }
    
    if (transaction.isReadOnly) {
      if (transaction.snapshot.containsKey(key)) {
        System.out.print("Read by " + transaction.name + ", ");
        int[] valueInfo = transaction.snapshot.get(key);
        print(key, valueInfo[0], valueInfo[1]);
      } else {
        abort(transaction);
      }
    } else {
      int i = 0;
      
      for (; i < sites.length; i++) {
        // If not read-only type, get the first working site which contains that variable
        if (!sites[i].isDown && sites[i].database.containsKey(key)) {
          ItemInfo itemInfo = sites[i].database.get(key);
          
          // Need to check if the variable is ready for read, and make sure it does not have a 
          // write lock on it
          if (itemInfo.isReadyForRead && !itemInfo.hasWriteLock()) {
            // Get a new lock and put it inside the lock list of that variable
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.lockList.add(lock);
            sites[i].addLock(lock);
            transaction.locksHolding.add(lock);
            
            // print out the read value
            System.out.print("Read by " + transaction.name + ", ");
            print(itemInfo.key, itemInfo.value, i + 1);
            break;
          } else if (itemInfo.isReadyForRead) {
            // First check if the transaction holding the lock is the same with the current 
            // transaction. If so, print out the value of that write lock even if the transaction 
            // has not committed. If not, check whether the current transaction should wait for the 
            // transaction holding the write lock also check if all the transactions in the wait 
            // list of that variable are younger than the current transaction. Otherwise, there is 
            // no need to wait if decide to wait, put the lock (which represents an operation to be 
            // performed later) to the wait list
            if (itemInfo.getWriteLock().transaction.name.equals(transaction.name)) {
              LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
              
              itemInfo.lockList.add(lock);
              sites[i].addLock(lock);
              transaction.locksHolding.add(lock);
              System.out.print("Read by " + transaction.name + ", ");
              print(itemInfo.key, itemInfo.getWriteLock().value, itemInfo.getWriteLock().site.siteID);
            }
            else if (itemInfo.getWriteLock().transaction.initTime > transaction.initTime && itemInfo.canWait(transaction)) {
              LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
              
              itemInfo.waitList.add(lock);
              sites[i].addLock(lock);
              transaction.locksHolding.add(lock);
            } else {
              abort(transaction);
            }
            break;
          }
        }
      }
      
      // does not find a working site containing the variable, abort
      if (i == sites.length) {
        if (!isReplicated(key)) {
          for (int pos = 0; pos < sites.length; pos++) {
            if (sites[pos].database.containsKey(key) && sites[pos].isDown) {
              LockInfo lock = new LockInfo(transaction, sites[pos].database.get(key), sites[pos], LockType.READ, 
                  0, true); 
              sites[pos].addLock(lock);
              transaction.locksHolding.add(lock);
              sites[pos].database.get(key).lockList.add(lock);
              break;
            } 
          }
        }
        else {
          abort(transaction);
        }
      }
    }
  }

  void write(String transactionName, String key, int value) {
    Transaction transaction = transactionMapping.get(transactionName);
    
    if (transaction == null) {
      return;
    }
    
    int i = 0;
    boolean shouldAbort = true;
    
    for (; i < sites.length; i++) {
      // need to check if the variable is ready for read,
      // and make sure it does not have any lock on it (except for the lock held by itself)
      if (!sites[i].isDown && sites[i].database.containsKey(key)) {
        ItemInfo itemInfo = sites[i].database.get(key);
        if (!itemInfo.hasLock()) {
          // if it does not have lock, get a new lock
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          itemInfo.lockList.add(lock);
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
          shouldAbort = false;
        } else {
          List<LockInfo> lockList = itemInfo.getLockList();
          // if it already has a lock, first check if it is itself holding the lock
          int pos = 0;
          for (; pos < lockList.size(); pos++) {
            if (!lockList.get(pos).transaction.name.equals(transaction.name)) {
              break;
            }
          }
          // if it's not itself, decide if it should wait
          if (pos != lockList.size()) {
            for (LockInfo lock : lockList) {
              if (lock.transaction.initTime < transaction.initTime) {
                abort(transaction);
                break;
              }
            }
            if (!itemInfo.canWait(transaction)) {
              abort(transaction);
              break;
            }
          }
          
          // if it's only itself or it decides to wait,
          // get a new lock and put it in the lock list or wait list
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          if (pos == lockList.size()) {
              itemInfo.lockList.add(lock);
          }
          else {
            itemInfo.waitList.add(lock); 
          }
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
          shouldAbort = false;
        }
      }
    }
    
    if (shouldAbort) {
      if (!isReplicated(key)) {
        for (int pos = 0; pos < sites.length; pos++) {
          if (sites[pos].database.containsKey(key) && sites[pos].isDown) {
            LockInfo lock = new LockInfo(transaction, sites[pos].database.get(key), sites[pos], LockType.WRITE, 
                value, true); 
            sites[pos].addLock(lock);
            transaction.locksHolding.add(lock);
            sites[pos].database.get(key).lockList.add(lock);
            break;
          } 
        }
      }
      else {
        abort(transaction);
      }
    }
  }

  void abort(Transaction transaction) {
    end(transaction, false);
  }

  void end(Transaction transaction, boolean toCommit) {
    if (transaction == null || !transactionMapping.containsKey(transaction.name)) {
      return;
    }
    
    System.out.println(transaction.name + (toCommit ? " committed" : " aborted"));
    
    // Commit writes if the transaction is to commit.
    if (toCommit) {
      transaction.commitWrites();
    }
    
    transaction.releaseLocks();
    transactionMapping.remove(transaction.name);
  }

  void fail(int siteNumber) {
    sites[siteNumber - 1].fail();
  }

  void recover(int siteNumber) {
    sites[siteNumber - 1].recover();
  }

  /**
   * dump all items in each site
   */
  void dump() {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump();
      System.out.println();
    }
  }

  /**
   * dump a specific item in each site
   */
  void dump(String key) {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump(key);
    }
  }
  

  void print(String item, int value, int siteNumber) {
    System.out.println(item + ": " + value + " at site " + siteNumber);
  }
}