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
  ArrayList<LockInfo> lockers;
  ArrayList<LockInfo> waitList;

  ItemInfo(String name, int value) {
    this.key = name;
    this.value = value;
    this.isReadReady = true;
    this.lockers = new ArrayList<LockInfo>();
    this.waitList = new ArrayList<LockInfo>();
  }

  LockInfo getWriteLockInfo() {
    validateLockers();
    
    for (LockInfo lockInfo: lockers) {
      if (lockInfo.lockType == LockType.WRITE) {
        return lockInfo;
      }
    }
    
    return null;
  }

  ArrayList<LockInfo> getLockers() {
    validateLockers();
    return lockers;
  }

  void validateLockers() {
    validate(lockers);
  }

  void validateWaitList() {
    validate(waitList);
  }

  // Remove invalid locks of which transaction is aborted or site is down.
  private void validate(ArrayList<LockInfo> list) {
    for (int i = 0; i < list.size();) {
      LockInfo lock = list.get(i);
      if (!lock.isValid) {
        list.remove(i);
      } else {
        i++;
      }
    }
  }

  /**
   * When a lock on the item is released, update the lock list (grant lock to the first transaction 
   * on the waitList).
   * 1.If it is a read lock, traverse the waitList to let in all the read lock requests.
   * 2.If it is a write lock, traverse the waitList to let in the read lock request from the same 
   *     transaction.
   */
  void updateItemLockStatus() {
    validateLockers();
    validateWaitList();
    
    if (lockers.size() == 0 && waitList.size() > 0) {
      if (waitList.get(0).lockType == LockType.WRITE) {
        LockInfo currentLock = waitList.remove(0);
        lockers.add(currentLock);
        
        int i = 0;
        while (waitList.size() >= i + 1) {
          if (waitList.get(i).transaction.name.equals(currentLock.transaction.name)) {
            LockInfo readLockInfo = waitList.remove(i);
            readLockInfo.value = currentLock.value;
            lockers.add(readLockInfo);
          } else {
            i++;
          }
        }
      } else {
        int i = 0;
        while (waitList.size() >= i + 1) {
          if (waitList.get(i).lockType == LockType.READ) {
            LockInfo readLockInfo = waitList.remove(i);
            readLockInfo.value = readLockInfo.site.database.get(readLockInfo.itemInfo.key).value;
            lockers.add(readLockInfo);
          } else {
            i++;
          }
        }
      }
    }
  }
  
  @Override
  public String toString() {
    return key + ": " + value;
  }
}