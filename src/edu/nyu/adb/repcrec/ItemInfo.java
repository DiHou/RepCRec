package edu.nyu.adb.repcrec;

import java.util.ArrayList;

/**
 * This is the value information stored in database of each SimulatedSite.
 * Package access level, not intended to expose for public use.
 *  
 * @author yanghui
 */
class ItemInfo {
  String key;
  int value;
  boolean isReadReady;
  ArrayList<LockInfo> lockList;
  ArrayList<LockInfo> waitList;

  ItemInfo(String name, int value) {
    this.key = name;
    this.value = value;
    this.isReadReady = true;
    this.lockList = new ArrayList<LockInfo>();
    this.waitList = new ArrayList<LockInfo>();
  }
  
  boolean isWriteLocked() {
    removeInvalidLockInLockList();
    
    for (LockInfo lock: lockList) {
      if (lock.lockType == LockType.WRITE) {
        return true;
      }
    } 
    return false;
  }
  
  LockInfo getWriteLockInfo() {
    removeInvalidLockInLockList();
    for (LockInfo lockInfo: lockList) {
      if (lockInfo.lockType == LockType.WRITE) {
        return lockInfo;
      }
    } 
    return null;
  }

  boolean isReadOrWriteLocked() {
    removeInvalidLockInLockList();
    return lockList.size() != 0;
  }

  // Remove invalid locks of which transaction is aborted or site is down on locklist.
  void removeInvalidLockInLockList() {
    removeInvalidLock(lockList);
  }

  // Remove invalid locks of which transaction is aborted or site is down on waitlist.
  void removeInvalidLockInWaitList() {
    removeInvalidLock(waitList);
  }

  private void removeInvalidLock(ArrayList<LockInfo> list) {
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
    removeInvalidLockInLockList();
    return lockList;
  }

//  /**
//   * check if a transaction can wait
//   * 
//   * for efficiency, there is no need to add a lock to the wait list if there exists an older 
//   * transaction, so the time stamps of the transactions in the wait list should be in strict 
//   * decreasing order
//   */
//  boolean canWait(Transaction t) {
//    removeInvalidLockInWaitList();
//    return waitList.isEmpty() || waitList.get(waitList.size() - 1).transaction.initTime > t.initTime;
//  }
  
  /**
   * whenever a lock on this variable is released, update the lock list and check if locks in the 
   * wait list also should be moved to the active lock list
   */
  void update() {
    removeInvalidLockInLockList();
    removeInvalidLockInWaitList();
    
    if (lockList.size() == 0 && waitList.size() > 0) {
      if (waitList.get(0).lockType == LockType.WRITE) {
        Transaction t = waitList.get(0).transaction;
        lockList.add(waitList.get(0));
        waitList.remove(0);
        int i = 0;
        while (waitList.size() >= i + 1) {
          if (waitList.get(i).transaction.name.equals(t.name)) {
            lockList.add(waitList.get(i));
            waitList.remove(i);
          } else {
            i++;
          }
        }
      } else {
        int i = 0;
        while (waitList.size() >= i + 1) {
          if (waitList.get(i).lockType == LockType.READ) {
            LockInfo lock = waitList.get(i);
            lockList.add(lock);
            waitList.remove(i);
            
            System.out.print("Read by " + lock.transaction.name + ", ");
            print(lock.itemInfo.key, lock.itemInfo.value, lock.site.siteID);
          } else {
            i++;
          }
        }
      }
    }
  }
  
  void print(String key, int value, int siteNumber) {
    System.out.println(key + ": " + value + " at site " + siteNumber);
  }
}