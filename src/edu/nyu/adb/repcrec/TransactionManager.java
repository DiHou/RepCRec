package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
          transaction.locksHolding.add(lock);
          sites[i].addLock(lock);
          
          System.out.print("Read by " + transaction.name + ", ");
          print(itemInfo.key, itemInfo.value, i + 1);
          return;
        } else if (itemInfo.isReadyForRead) {  // item is write locked
          LockInfo writeLock = itemInfo.getWriteLockInfo();
          if (writeLock.transaction.name.equals(transaction.name)) {  // locked by self
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.lockList.add(lock);
            transaction.locksHolding.add(lock);
            sites[i].addLock(lock);
            
            System.out.print("Read by " + transaction.name + ", ");
            print(itemInfo.key, itemInfo.getWriteLockInfo().value, itemInfo.getWriteLockInfo().site.siteID);
          } else {
            LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
            
            itemInfo.waitList.add(lock);
            transaction.locksHolding.add(lock);
            sites[i].addLock(lock);
            sites[i].conflicts.add(new Conflict(transactionName, writeLock.transaction.name));
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
        if (!itemInfo.isReadOrWriteLocked()) {    // if the item does not have any lock
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          
          itemInfo.lockList.add(lock);
          sites[i].addLock(lock);
          transaction.locksHolding.add(lock);
//          shouldAbort = false;
        } else {
          ArrayList<LockInfo> lockList = itemInfo.getLockList();
          
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
            for (int j = 0; j < lockListSize; j++) {
              sites[i].conflicts.add(new Conflict(transactionName, lockList.get(j).transaction.name));
            }
          }
          
          transaction.locksHolding.add(lock);
          sites[i].addLock(lock);
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
    if (transaction == null) {
      return;
    }
    
    if (toCommit) {
      System.out.println(transaction.name + " committed");
    }
    if (!toCommit) {
      System.out.println(transaction.name + " aborted");
    }
    
    // Commit writes if the transaction is to commit.
    if (toCommit) {
      transaction.commitWrites();
    }
    
    transaction.releaseLocks();
    transactionMapping.remove(transaction.name);
  }

  void deadLockCheckAndHandle() {
    HashSet<Conflict> conflicts = constructConflicts();
    HashSet<String> deadLockCycle = detectDeadlockCycle(conflicts);
//    System.out.println("Deadlock? " + (deadLockCycle == null));
    if (deadLockCycle != null) {
      System.out.println("There is deadlock");
      Transaction youngest = null, transaction = null;
      int initTime = -1;
      
      for (String transactionName: deadLockCycle) {
        transaction = transactionMapping.get(transactionName);
        if (transaction.initTime > initTime) {
          youngest = transaction;
          initTime = transaction.initTime;
        }
      }
      
      abort(youngest);
    }
  }
  
  HashSet<Conflict> constructConflicts() {
    HashSet<Conflict> result = new HashSet<>();
    
    for (int i = 0; i < sites.length; i++) {
      result.addAll(sites[i].conflicts);
    }
    
//    for (Conflict conflict: result) {
//      System.out.println("Conflict: " + conflict.waiting + " -> " + conflict.waited);
//    }
    
    return result;
  }
  
  HashSet<String> detectDeadlockCycle(HashSet<Conflict> conflicts) {
    HashSet<String> visitedAll = new HashSet<>();
    HashSet<String> waitings = new HashSet<>();
    HashMap<String, ArrayList<String>> map = new HashMap<>();
    
    ArrayList<String> current = null;
    for (Conflict conflict: conflicts) {
      waitings.add(conflict.waiting);
      current = map.containsKey(conflict.waiting) ? map.get(conflict.waiting) : new ArrayList<>();
      current.add(conflict.waited);
      map.put(conflict.waiting, current);
    }
    
    HashSet<String> deadLockCycle = new HashSet<>();
    for (String waiting: waitings) {
      if (visitedAll.contains(waiting)) {
        continue;
      }
      if (dfs(waiting, new HashSet<>(), visitedAll, map, deadLockCycle)) {
        return deadLockCycle;
      }
    }
    
    return null;
  }
  
  private boolean dfs(String waiting, HashSet<String> visited, HashSet<String> visitedAll, 
      HashMap<String, ArrayList<String>> map, HashSet<String> deadLockCycle) {
    if (visited.contains(waiting)) {
      deadLockCycle.addAll(visited);
      return true;
    }
    visited.add(waiting);
    visitedAll.add(waiting);
    
    if (!map.containsKey(waiting)) {
      return false;
    }
    ArrayList<String> waiteds = map.get(waiting);
    
    for (String waited: waiteds) {
      if (!visited.contains(waited) && dfs(waited, visited, visitedAll, map, deadLockCycle)) {
        return true;
      }
    }
    
    return false;
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