package edu.nyu.adb.repcrec;

import java.util.ArrayList;

class ItemInfo {
  String key;
  int value;
  boolean isReadyForRead;
  ArrayList<LockInfo> lockList;
  ArrayList<LockInfo> waitList;

  ItemInfo(String name, int value) {
    this.key = name;
    this.value = value;
    this.isReadyForRead = true;
    this.lockList = new ArrayList<LockInfo>();
    this.waitList = new ArrayList<LockInfo>();
  }
  
  /**
   * when a transaction wants to read, it needs to examine if there is write lock on this variable 
   * before examination, make sure all the locks in the lock list are active (remove the locks 
   * which have been released due to the end of a transaction or the failure of a site)
   */
  boolean isWriteLocked() {
    cleanLock();
    
    // Normally if there's a write lock, it must be the first and only lock in the
    // lock list. But if one transaction wants to have a read lock and then a write lock on the same 
    // variable, it might be able to get both at the same time, so the lock list might contain
    // both write lock and read locks.
    for (LockInfo lock: lockList) {
      if (lock.lockType == LockType.WRITE) {
        return true;
      }
    } 
    return false;
  }
  
  LockInfo getWriteLock() {
    cleanLock();
    for (LockInfo lock: lockList) {
      if (lock.lockType == LockType.WRITE) {
        return lock;
      }
    } 
    return null;
  }

  boolean hasLock() {
    cleanLock();
    return lockList.size() != 0;
  }

  /**
   * clean lock
   * 
   * when a transaction ends or a site fails, the lock release process will be triggered from the 
   * corresponding objects, so a lock which has been released will be marked as inactive but still 
   * exist in the variable lock list, such locks should be removed whenever the variable object is
   * visited
   */
  void cleanLock() {
    for (int i = 0; i < lockList.size();) {
      LockInfo lock = lockList.get(i);
      if (!lock.isActive) {
        lockList.remove(i);
      } else {
        i++;
      }
    }
  }

  /**
   * clean wait
   * 
   * if a lock is in the wait list and is never acquired because the transaction aborts or the site 
   * fails, it should be removed from the waitList
   */
  void cleanWaitList() {
    for (int i = 0; i < waitList.size();) {
      LockInfo lock = waitList.get(i);
      if (!lock.isActive) {
        waitList.remove(i);
      } else {
        i++;
      }
    }
  }

  ArrayList<LockInfo> getLockList() {
    cleanLock();
    return lockList;
  }

  /**
   * check if a transaction can wait
   * 
   * for efficiency, there is no need to add a lock to the wait list if there exists an older 
   * transaction, so the time stamps of the transactions in the wait list should be in strict 
   * decreasing order
   */
  boolean canWait(Transaction t) {
    cleanWaitList();
    return waitList.isEmpty() || waitList.get(waitList.size() - 1).transaction.initTime > t.initTime;
  }
  
  /**
   * whenever a lock on this variable is released, update the lock list and check if locks in the 
   * wait list also should be moved to the active lock list
   */
  void update() {
    cleanLock();
    cleanWaitList();
    
    if (lockList.size() == 0 && waitList.size() > 0) {
      if (waitList.get(0).lockType == LockType.WRITE) {
        Transaction t = waitList.get(0).transaction;
        lockList.add(waitList.get(0));
        waitList.remove(0);
        while (waitList.size() > 0 && waitList.get(0).transaction.name.equals(t.name)) {
          lockList.add(waitList.get(0));
          waitList.remove(0);
        }
      } else {
        while (waitList.size() > 0 && waitList.get(0).lockType == LockType.READ) {
          LockInfo lock = waitList.get(0);
          lockList.add(lock);
          waitList.remove(0);
          
          System.out.print("Read by " + lock.transaction.name + ", ");
          print(lock.itemInfo.key, lock.itemInfo.value, lock.site.siteID);
        }
      }
    }
  }
  
  void print(String key, int value, int siteNumber) {
    System.out.println(key + ": " + value + " at site " + siteNumber);
  }
}