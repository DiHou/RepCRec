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
  
  boolean isWriteLocked() {
    cleanLockList();
    
    for (LockInfo lock: lockList) {
      if (lock.lockType == LockType.WRITE) {
        return true;
      }
    } 
    return false;
  }
  
  LockInfo getWriteLockInfo() {
    cleanLockList();
    for (LockInfo lockInfo: lockList) {
      if (lockInfo.lockType == LockType.WRITE) {
        return lockInfo;
      }
    } 
    return null;
  }

  boolean isReadOrWriteLocked() {
    cleanLockList();
    return lockList.size() != 0;
  }

  // Remove invalid locks of which transaction is aborted or site is down.
  void cleanLockList() {
    cleanList(lockList);
  }

  // Remove invalid locks of which transaction is aborted or site is down.
  void cleanWaitList() {
    cleanList(waitList);
  }

  private void cleanList(ArrayList<LockInfo> list) {
    for (int i = 0; i < list.size();) {
      LockInfo lock = list.get(i);
      if (!lock.isValid) {
        list.remove(i);
      } else {
        i++;
      }
    }
  }

  ArrayList<LockInfo> getLockList() {
    cleanLockList();
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
    cleanLockList();
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