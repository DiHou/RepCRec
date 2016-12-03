package edu.nyu.adb.repcrec;

class LockInfo {
  Transaction transaction;
  ItemInfo itemInfo;
  SimulatedSite site;
  LockType lockType;
  int value;        // the value to write for the write lock
  boolean isValid;  // When transaction is finished or site is down, the lock becomes invalid.
  
  /**
   * when a lock is created, its pointer will be stored in three objects: the
   * variable, the site and the transaction. In this way, we can much more
   * easily handle the cases when a site is failed or a transaction aborts by
   * simply marking the lock as inactive
   */
  LockInfo(Transaction transaction, ItemInfo itemInfo, SimulatedSite site, LockType lockType, 
      int value, boolean isValid) {
    this.transaction = transaction;
    this.itemInfo = itemInfo;
    this.site = site;
    this.lockType = lockType;
    this.value = value;
    this.isValid = isValid;
  }
}