package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.List;

/**
 * The Variable class is contains all the functions and variables needed for the
 * variables for the distributed database, including the locks. The lockList in
 * it lists of all the locks currently placed on this (copy of); the waitList in
 * it lists of all the locks which will be placed on this (copy of) variable,
 * currently are waiting for other locks to release.
 * 
 *
 */
public class ItemInfo {
  String key;
  int value;
  boolean isReadyForRead;
  List<LockInfo> lockList;
  List<LockInfo> waitList;

  public ItemInfo(String name, int value) {
    this.key = name;
    this.value = value;
    isReadyForRead = true;
    lockList = new ArrayList<LockInfo>();
    waitList = new ArrayList<LockInfo>();
  }
  
  /**
   * if the variable has write lock
   * 
   * when a transaction wants to read, it needs to examine if there is write
   * lock on this variable before examination, make sure all the locks in the
   * lock list are active (remove the locks which have been released due to
   * the end of a transaction or the failure of a site)
   * 
   * @return boolean if the variable has write lock
   */
  public boolean hasWriteLock() {

    cleanLock();

    // Normally if there's a write lock, it must be the first and only lock in the
    // lock list. But if one transaction wants to have a read lock and then a write lock on the same 
    // variable, it might be able to get both at the same time, so the lock list might contain
    // both write lock and read locks.
    for (LockInfo lock: lockList) {
      if (lock.type.equals("Write")) {
        return true;
      }
    } 
    return false;
  }
  
  /**
   * if a variable has write lock, return that write lock
   * 
   * @return the write lock if the variable does have one
   */
  public LockInfo getWriteLock() {
    cleanLock();
    for (LockInfo lock: lockList) {
      if (lock.type.equals("Write")) {
        return lock;
      }
    } 
    return null;
  }

  /**
   * hasLock
   * 
   * @return boolean if has lock
   */
  public boolean hasLock() {
    cleanLock();
    return lockList.size() != 0;
  }

  /**
   * clean lock
   * 
   * when a transaction ends or a site fails, the lock release process will be
   * triggered from the corresponding objects, so a lock which has been
   * released will be marked as inactive but still exist in the variable lock
   * list, such locks should be removed whenever the variable object is
   * visited
   */
  public void cleanLock() {
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
   * if a lock is in the wait list and is never acquired because the
   * transaction aborts or the site fails, it should be removed from the wait
   * list
   */
  public void cleanWait() {

    for (int i = 0; i < waitList.size();) {
      LockInfo lock = waitList.get(i);
      if (!lock.isActive) {
        waitList.remove(i);
      } else {
        i++;
      }
    }
  }

  /**
   * place a lock on the variable
   * 
   * @param lock
   */
  public void placeLock(LockInfo lock) {
    lockList.add(lock);
  }

  /**
   * lockList getter
   * 
   * @return LockList
   */
  public List<LockInfo> getLockList() {
    cleanLock();
    return lockList;
  }

  /**
   * check if a transaction can wait
   * 
   * for efficiency, there is no need to add a lock to the wait list if there
   * exists an older transaction, so the time stamps of the transactions in
   * the wait list should be in strict decreasing order
   * 
   * @param transaction
   * @return boolean if a transaction can wait
   */
  public boolean canWait(Transaction t) {
    cleanWait();
    return waitList.isEmpty()
        || waitList.get(waitList.size() - 1).transaction.initTime > t.initTime;
  }
  
  /**
   * update
   * 
   * whenever a lock on this variable is released, update the lock list and
   * check if locks in the wait list also should be moved to the active lock
   * list
   */
  public void update() {
    cleanLock();
    cleanWait();

    if (lockList.size() == 0 && waitList.size() > 0) {

      if (waitList.get(0).type.equals("Write")) {
        Transaction t = waitList.get(0).transaction;
        lockList.add(waitList.get(0));
        waitList.remove(0);
        while (waitList.size() > 0 && waitList.get(0).transaction.name.equals(t.name)) {
          lockList.add(waitList.get(0));
          waitList.remove(0);
        }
      } else {
        while (waitList.size() > 0
            && waitList.get(0).type.equals("Read")) {
          LockInfo lock = waitList.get(0);
          lockList.add(lock);
          waitList.remove(0);

          System.out.print("Read by "
              + lock.transaction.name + ", ");
          print(lock.variable.key, lock.site
              .siteIndex(), lock.variable.value);
        }
      }
    }
  }
  
  public void print(String vName, int siteNumber, int value) {
    System.out.println(vName + ": " + value + " at site " + siteNumber);
  }
}