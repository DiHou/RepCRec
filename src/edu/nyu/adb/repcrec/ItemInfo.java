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
    validateLockList();
    
    for (LockInfo lock: lockList) {
      if (lock.lockType == LockType.WRITE) {
        return true;
      }
    } 
    return false;
  }

  LockInfo getWriteLockInfo() {
    validateLockList();
    for (LockInfo lockInfo: lockList) {
      if (lockInfo.lockType == LockType.WRITE) {
        return lockInfo;
      }
    } 
    return null;
  }

  boolean isReadOrWriteLocked() {
    validateLockList();
    return lockList.size() != 0;
  }

  // Remove invalid locks of which transaction is aborted or site is down on locklist.
  void validateLockList() {
    removeInvalidLock(lockList);
  }

  // Remove invalid locks of which transaction is aborted or site is down on waitlist.
  void validateWaitList() {
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
    validateLockList();
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
   * When a lock on the item is released, update the lock list (grant lock to the first transaction 
   * on the waitList).
   * 1.If it is a read lock, traverse the waitList to let in all the read lock requests.
   * 2.If it is a write lock, traverse the waitList to let in the read lock request from the same 
   *     transaction.
   */
  void updateItemLockStatus() {
    validateLockList();
    validateWaitList();
    
    if (lockList.size() == 0 && waitList.size() > 0) {
      if (waitList.get(0).lockType == LockType.WRITE) {
        LockInfo currentLock = waitList.remove(0);
        lockList.add(currentLock);
        
        int i = 0;
        while (waitList.size() >= i + 1) {
          if (waitList.get(i).transaction.name.equals(currentLock.transaction.name)) {
            LockInfo readLockInfo = waitList.remove(i);
            readLockInfo.value = currentLock.value;
            lockList.add(readLockInfo);
          } else {
            i++;
          }
        }
      } else {
        int i = 0;
        while (waitList.size() >= i + 1) {
//          LockInfo lockInfo = waitList.get(i);
          if (waitList.get(i).lockType == LockType.READ) {
//            lockList.add(waitList.remove(i));
            
            LockInfo readLockInfo = waitList.remove(i);
            readLockInfo.value = readLockInfo.site.database.get(readLockInfo.itemInfo.key).value;
            lockList.add(readLockInfo);
//            waitList.remove(i);
//            System.out.print("Read by " + lockInfo.transaction.name + ", ");
//            print(lockInfo.itemInfo.key, lockInfo.itemInfo.value, lockInfo.site.siteID);
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