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
      return;  //simply ignore it
    }
    
    if (transaction.isReadOnly) {
      if (transaction.dbSnapshot.containsKey(key)) {
        System.out.print("Read by " + transaction.name + ", ");
        int[] valueInfo = transaction.dbSnapshot.get(key);
        print(key, valueInfo[0], valueInfo[1]);
      } else {
        abort(transaction);
      }
      return;
    }
    
    // If the transaction is not read-only, get its value from an alive site which stores its 
    // information. If all the site that stores its info is down, add it to unfinished and return.
    for (int i = 0; i < sites.length; i++) {
      if (sites[i].isDown) {
        continue;
      } else if (sites[i].database.containsKey(key)) {
        ItemInfo itemInfo = sites[i].database.get(key);
        
        if (itemInfo.isReadyForRead && !itemInfo.isWriteLocked()) {
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
          
          itemInfo.lockList.add(lock);
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
          
          System.out.print("Read by " + transaction.name + ", ");
          print(itemInfo.key, itemInfo.value, i + 1);
          return;
        } else if (itemInfo.isReadyForRead) {  // item is write locked
          // if it is locked by itself
          if (itemInfo.getWriteLock().transaction.name.equals(transaction.name)) {
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.lockList.add(lock);
            sites[i].addLock(lock);
            transaction.locksHolding.add(lock);
            System.out.print("Read by " + transaction.name + ", ");
            print(itemInfo.key, itemInfo.getWriteLock().value, itemInfo.getWriteLock().site.siteID);
          } else {
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.waitList.add(lock);
            sites[i].addLock(lock);
            transaction.locksHolding.add(lock);
          }
          return;
        }
      }
    }
    
    //!!!! need to handle the case that every site that contains the item is down
    //add to the unfinished hashmap for processing.
    
    // does not find a working site containing the variable, abort
//    if (i == sites.length) {
//      if (!isReplicated(key)) {
//        for (int pos = 0; pos < sites.length; pos++) {
//          if (sites[pos].database.containsKey(key) && sites[pos].isDown) {
//            LockInfo lock = new LockInfo(transaction, sites[pos].database.get(key), sites[pos], LockType.READ, 
//                0, true); 
//            sites[pos].addLock(lock);
//            transaction.locksHolding.add(lock);
//            sites[pos].database.get(key).lockList.add(lock);
//            break;
//          } 
//        }
//      }
//      else {
//        abort(transaction);
//      }
//    }
  }

  void write(String transactionName, String key, int value) {
    Transaction transaction = transactionMapping.get(transactionName);
    
    if (transaction == null) {
      return;
    }
    
//    boolean shouldAbort = true;
    
    for (int i = 0; i < sites.length; i++) {
      if (sites[i].isDown) {
        continue;
      } else if (sites[i].database.containsKey(key)) {
        ItemInfo itemInfo = sites[i].database.get(key);
        if (!itemInfo.hasLock()) {    // if the item does not have any lock
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          
          itemInfo.lockList.add(lock);
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
//          shouldAbort = false;
        } else {
          List<LockInfo> lockList = itemInfo.getLockList();
          
          // Check whether only itself owns the lock.
          boolean lockOnlyOwnedBySelf = true;
          int lockListSize = lockList.size();
          for (int j = 0; j < lockListSize; j++) {
            if (!lockList.get(j).transaction.name.equals(transaction.name)) {
              lockOnlyOwnedBySelf = false;
              break;
            }
          }
          
          // If it's not itself, decide if it should wait
//          if (!lockOnlyOwnedBySelf) {
//            for (LockInfo lock : lockList) {
//              if (lock.transaction.initTime < transaction.initTime) {
//                abort(transaction);
//                break;
//              }
//            }
//            if (!itemInfo.canWait(transaction)) {
//              abort(transaction);
//              break;
//            }
//          }
          
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          if (lockOnlyOwnedBySelf) {
            itemInfo.lockList.add(lock);
          } else {
            itemInfo.waitList.add(lock); 
          }
          
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
//          shouldAbort = false;
        }
      }
    }
    
//    if (shouldAbort) {
//      if (!isReplicated(key)) {
//        for (int pos = 0; pos < sites.length; pos++) {
//          if (sites[pos].database.containsKey(key) && sites[pos].isDown) {
//            LockInfo lock = new LockInfo(transaction, sites[pos].database.get(key), sites[pos], LockType.WRITE, 
//                value, true); 
//            sites[pos].addLock(lock);
//            transaction.locksHolding.add(lock);
//            sites[pos].database.get(key).lockList.add(lock);
//            break;
//          } 
//        }
//      }
//      else {
//        abort(transaction);
//      }
//    }
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

  void deadLockCheckAndHandle() {
    
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