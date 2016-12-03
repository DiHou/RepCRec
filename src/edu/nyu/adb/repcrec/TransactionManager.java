package edu.nyu.adb.repcrec;

import java.util.HashMap;
import java.util.List;

/**
 * The Manager class is use as a Transaction Manager, it has the main methods
 * needed to control transactions in the database.
 *
 */
public class TransactionManager {
  HashMap<String, Transaction> transactionList;
  SimulatedSite[] sites;

  public void print(String item, int value, int siteNumber) {
    System.out.println(item + ": " + value + " at site " + siteNumber);
  }

  public void initialize() {
    transactionList = new HashMap<String, Transaction>();
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

  /**
   * determine whether a variable is replicated
   */
  public boolean isReplicated(String name) {
    int index = Integer.parseInt(name.substring(1, name.length()));
    return index % 2 == 0 ? true : false;
  }

  public void begin(String name, int time, boolean isReadOnly) {
    transactionList.put(name, new Transaction(name, time, isReadOnly, this));
  }

  public void read(String transactionName, String key) {
    Transaction transaction = transactionList.get(transactionName);
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
        // if not read-only type,
        // get the first working site which contains that variable
        if (!sites[i].isDown && sites[i].database.containsKey(key)) {
          ItemInfo itemInfo = sites[i].database.get(key);
          
          // need to check if the variable is ready for read,
          // and make sure it does not have a write lock on it
          if (itemInfo.isReadyForRead && !itemInfo.hasWriteLock()) {
            // get a new lock and put it inside the lock list of
            // that variable
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.addLock(lock);
            sites[i].addLock(lock);
            transaction.addLock(lock);
            
            // print out the read value
            System.out.print("Read by " + transaction.name + ", ");
            print(itemInfo.key, itemInfo.value, i + 1);
            break;
            
            // if the variable has a write lock
          } else if (itemInfo.isReadyForRead) {
            // first check if the transaction holding the lock is the same with the current transaction,
            // if so, print out the value of that write lock even if the transaction has not committed
            // if not, then check if the current transaction should wait for the
            // transaction holding the write lock
            // also check if all the transactions in the wait list
            // of that variable are younger than the current
            // transaction,
            // because otherwise, there is no need to wait
            // if decide to wait, put the lock (which represents an
            // operation to be performed later) to the wait list
            if (itemInfo.getWriteLock().transaction.name.equals(transaction.name)) {
              LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
              
              itemInfo.addLock(lock);
              sites[i].addLock(lock);
              transaction.addLock(lock);
              System.out.print("Read by " + transaction.name + ", ");
              print(itemInfo.key, itemInfo.getWriteLock().value, itemInfo.getWriteLock().site.siteID);
            }
            else if (itemInfo.getWriteLock().transaction.initTime > transaction.initTime && itemInfo.canWait(transaction)) {
              LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
              
              itemInfo.waitList.add(lock);
              sites[i].addLock(lock);
              transaction.addLock(lock);
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
              transaction.addLock(lock);
              sites[pos].database.get(key).addLock(lock);
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

  public void write(String transaction, String key, int value) {
    Transaction t = transactionList.get(transaction);
    
    if (t == null) {
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
          LockInfo lock = new LockInfo(t, itemInfo, sites[i], LockType.WRITE, value, true);
          itemInfo.addLock(lock);
          sites[i].addLock(lock);
          t.addLock(lock);
          shouldAbort = false;
        } else {
          List<LockInfo> lockList = itemInfo.getLockList();
          // if it already has a lock, first check if it is itself holding the lock
          int pos = 0;
          for (; pos < lockList.size(); pos++) {
            if (!lockList.get(pos).transaction.name.equals(t.name)) {
              break;
            }
          }
          // if it's not itself, decide if it should wait
          if (pos != lockList.size()) {
            for (LockInfo lock : lockList) {
              if (lock.transaction.initTime < t.initTime) {
                abort(t);
                break;
              }
            }
            if (!itemInfo.canWait(t)) {
              abort(t);
              break;
            }
          }
          
          // if it's only itself or it decides to wait,
          // get a new lock and put it in the lock list or wait list
          LockInfo lock = new LockInfo(t, itemInfo, sites[i], LockType.WRITE, value, true);
          if (pos == lockList.size()) {
              itemInfo.addLock(lock);
          }
          else {
            itemInfo.waitList.add(lock); 
          }
          sites[i].addLock(lock);
          t.addLock(lock);
          shouldAbort = false;
        }
      }
    }
    
    if (shouldAbort) {
      if (!isReplicated(key)) {
        for (int pos = 0; pos < sites.length; pos++) {
          if (sites[pos].database.containsKey(key) && sites[pos].isDown) {
            LockInfo lock = new LockInfo(t, sites[pos].database.get(key), sites[pos], LockType.WRITE, 
                value, true); 
            sites[pos].addLock(lock);
            t.addLock(lock);
            sites[pos].database.get(key).addLock(lock);
            break;
          } 
        }
      }
      else {
        abort(t);
      }
    }
  }

  public void abort(Transaction transaction) {
    end(transaction, false);
  }

  public void end(Transaction transaction, boolean toCommit) {
    if (transaction == null || !transactionList.containsKey(transaction.name)) {
      return;
    }
    
    if (toCommit) {
      System.out.println(transaction.name + " committed");
    } else {
      System.out.println(transaction.name + " aborted");
    }
    
    // difference between commit and abort is that if all the locks should
    // be 'realized', or simply discarded
    if (toCommit) {
      transaction.commitWrites();
    }
    
    transaction.releaseLocks();
    transactionList.remove(transaction.name);
  }

  public void fail(int siteNumber) {
    sites[siteNumber - 1].fail();
  }

  public void recover(int siteNumber) {
    sites[siteNumber - 1].recover();
  }

  /**
   * dump all items in each site
   */
  public void dump() {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump();
      System.out.println();
    }
  }

  /**
   * dump a specific item in each site
   */
  public void dump(String key) {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump(key);
    }
  }
}