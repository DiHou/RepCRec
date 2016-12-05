package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Package access level, not intended to expose for public use.
 *  
 * @author yanghui
 */
class TransactionManager {
  HashMap<String, Transaction> transactionMapping;
  HashMap<String, Unfinished> unfinished = new HashMap<>();
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

  void begin(String name, int time, boolean isReadOnly) {
    transactionMapping.put(name, new Transaction(name, time, isReadOnly, this));
  }

  boolean read(String transactionName, String key) {
    Transaction transaction = transactionMapping.get(transactionName);
    if (transaction == null) {
      return true;  //simply ignore it
    }
    
    if (transaction.isReadOnly) {
      if (transaction.dbSnapshot.containsKey(key)) {
        transaction.readsOfRO.add(transaction.dbSnapshot.get(key));
      } else {
        abort(transaction);
      }
      return true;
    }
    
    // If the transaction is not read-only, get its value from an alive site which stores its 
    // information. If all the site that stores its info is down, add it to unfinished and return.
    boolean foundAlive = false;
    for (int i = 0; i < sites.length; i++) {
      if (sites[i].isDown) {
        continue;
      } else if (sites[i].database.containsKey(key)) {
        foundAlive = true;
        ItemInfo itemInfo = sites[i].database.get(key);
        
        if (itemInfo.isReadReady) {
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.READ, 0, true);
          transaction.locksHolding.add(lock);
          sites[i].lockTable.add(lock);
          
          LockInfo writeLock = itemInfo.getWriteLockInfo();
          if (writeLock != null) {  // the item is write-locked
            if (writeLock.transaction.name.equals(transaction.name)) {  // it is locked by self
              itemInfo.lockers.add(lock);
            } else {
              itemInfo.waitList.add(lock);
              sites[i].conflicts.add(new Conflict(transactionName, writeLock.transaction.name));
            }
          } else {
            itemInfo.lockers.add(lock);
          }
          break;
        }
      }
    }
    
    // No alive site contains the variable, add the query to unfinished list.
    if (!foundAlive) {
      unfinished.put(transactionName, new Unfinished(transactionName, true, key));
      return false;
    }
    return true;
  }

  boolean write(String transactionName, String key, int value) {
    Transaction transaction = transactionMapping.get(transactionName);
    
    if (transaction == null) {
      return true;  //simply ignore it
    }
    
    boolean foundAlive = false;
    for (int i = 0; i < sites.length; i++) {
      if (sites[i].isDown) {
        continue;
      } else if (sites[i].database.containsKey(key)) {
        foundAlive = true;
        ItemInfo itemInfo = sites[i].database.get(key);
        
        ArrayList<LockInfo> lockList = itemInfo.getLockers();
        if (lockList.size() == 0) {    // if the item does not have any lock
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          
          itemInfo.lockers.add(lock);
          sites[i].lockTable.add(lock);
          transaction.locksHolding.add(lock);
        } else {
          // Check whether only itself owns the lock.
          boolean lockOnlyOwnedBySelf = true;
          int lockListSize = lockList.size();
          for (int j = 0; j < lockListSize; j++) {
            if (!lockList.get(j).transaction.name.equals(transaction.name)) {
              lockOnlyOwnedBySelf = false;
              break;
            }
          }
          
          LockInfo lock = new LockInfo(transaction, itemInfo, sites[i], LockType.WRITE, value, true);
          if (lockOnlyOwnedBySelf) {
            itemInfo.lockers.add(lock);
          } else {
            itemInfo.waitList.add(lock);
            for (int j = 0; j < lockListSize; j++) {
              if (!transactionName.equals(lockList.get(j).transaction.name)) {
                sites[i].conflicts.add(
                    new Conflict(transactionName, lockList.get(j).transaction.name));
              }
            }
          }
          
          transaction.locksHolding.add(lock);
          sites[i].lockTable.add(lock);
        }
      }
    }
    
    // No alive site contains the variable, add the query to unfinished list.
    if (!foundAlive) {
      unfinished.put(transactionName, new Unfinished(transactionName, false, key, value));
      return false;
    }
    return true;
  }

  void abort(Transaction transaction) {
    end(transaction, false);
  }

  void end(Transaction transaction, boolean toCommit) {
    if (transaction == null) {
      return;
    }
    unfinished.remove(transaction.name);  // Remove unfinished query if there is any.
    
    // Commit or abort for read-only transaction.
    if (transaction.isReadOnly) {
      if (toCommit) {
        System.out.printf("* %s(RO) starts committing...\n", transaction.name);
        transaction.commitReadsAndWritesRO();
      }
      transaction.readsOfRO.clear();
      transactionMapping.remove(transaction.name);
      System.out.printf("%s%s(RO) is %s.\n\n", (toCommit ? "* " : ""), transaction.name, 
          (toCommit ? "committed" : "aborted"));
      return;
    }
    
    // Commit or abort for read-write transaction.
    if (toCommit) {
      System.out.printf("* %s starts committing...\n", transaction.name);
      transaction.commitReadsAndWrites();
    }
    System.out.printf("%s%s is %s.\n\n", (toCommit ? "* " : ""), transaction.name, 
        (toCommit ? "committed" : "aborted"));
    
    transaction.releaseLocks();
    updateConflicts(transaction.name);
    transactionMapping.remove(transaction.name);
  }

  // Update conflicts on each site when a transaction is committed / aborted.
  void updateConflicts(String transactionName) {
    for (int i = 0; i < sites.length; i++) {
      sites[i].updateConflicts(transactionName);
    }
  }

  void deadLockCheckAndHandle() {
    HashSet<Conflict> conflicts = constructConflicts();
    HashSet<String> deadLockCycle = detectDeadlockCycle(conflicts);
//    System.out.println("Deadlock? " + (deadLockCycle != null));
    if (deadLockCycle != null) {
      Transaction youngest = null, transaction = null;
      int initTime = -1;
      
      for (String transactionName: deadLockCycle) {
        transaction = transactionMapping.get(transactionName);
        if (transaction != null && transaction.initTime > initTime) {
          youngest = transaction;
          initTime = transaction.initTime;
        }
      }
      
      abort(youngest);
      
      // Recursively check and handle deadlock until no cycle is left.
      // (Sometimes there are multiple cycles.)
      deadLockCheckAndHandle();
    }
  }
  
  private HashSet<Conflict> constructConflicts() {
    HashSet<Conflict> result = new HashSet<>();
    
    for (int i = 0; i < sites.length; i++) {
      result.addAll(sites[i].conflicts);
    }
    
//    for (Conflict conflict: result) {
//      System.out.println(conflict);
//    }
    
    return result;
  }
  
  private HashSet<String> detectDeadlockCycle(HashSet<Conflict> conflicts) {
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
      if (dfs(waiting, new LinkedHashSet<>(), visitedAll, map, deadLockCycle)) {
        return deadLockCycle;
      }
    }
    
    return null;
  }

  // This is the Depth First Search algorithm to detect the cycle.
  private boolean dfs(String waiting, LinkedHashSet<String> visited, HashSet<String> visitedAll, 
      HashMap<String, ArrayList<String>> map, HashSet<String> deadLockCycle) {
    if (visited.contains(waiting)) {
      getCycle(visited, waiting, deadLockCycle);
      return true;
    }
    visited.add(waiting);
    visitedAll.add(waiting);
    
    if (!map.containsKey(waiting)) {
      return false;
    }
    ArrayList<String> waiteds = map.get(waiting);
    
    boolean findDeadlock = false;
    for (String waited: waiteds) {
      findDeadlock = dfs(waited, visited, visitedAll, map, deadLockCycle);
      if (findDeadlock) {
        return true;
      } else {
        visited.remove(waited);
      }
    }
    
    return false;
  }

  private void getCycle(LinkedHashSet<String> visited, String head, HashSet<String> deadLockCycle) {
    boolean started = false;
    
    // The input cycle may be shape of "q", we need to get the "o" part out.
    for (String s: visited) {
      if (started == false) {
        if (head.equals(s)) {
          deadLockCycle.add(s);
          started = true;
        } else {
          continue;
        }
      } else {
        deadLockCycle.add(s);
      }
    }
  }

  void fail(int siteNumber) {
    sites[siteNumber - 1].fail();
  }

  void recover(int siteNumber) {
    sites[siteNumber - 1].recover();
  }

  // dump all items in each site
  void dump() {
    for (int i = 0; i < sites.length; i++) {
      if (!sites[i].isDown) {
        sites[i].dump();
      }
    }
  }

  // dump a specific item in each site
  void dump(String key) {
    System.out.printf("dumping item %s...\n", key);
    
    for (int i = 0; i < sites.length; i++) {
      if (!sites[i].isDown) {
        sites[i].dump(key);
      }
    }
    
    System.out.println();
  }
}