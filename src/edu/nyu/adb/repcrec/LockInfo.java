package edu.nyu.adb.repcrec;

class LockInfo {
  Transaction transaction;
  ItemInfo itemInfo;
  SimulatedSite site;
  LockType lockType;
  int value;  // the value to be written for the write lock
  boolean isActive;
  
  /**
   * when a lock is created, its pointer will be stored in three objects: the
   * variable, the site and the transaction. In this way, we can much more
   * easily handle the cases when a site is failed or a transaction aborts by
   * simply marking the lock as inactive
   */
  public LockInfo(Transaction transaction, ItemInfo itemInfo, SimulatedSite site, LockType lockType, 
      int value, boolean isActive) {
    this.transaction = transaction;
    this.itemInfo = itemInfo;
    this.site = site;
    this.lockType = lockType;
    this.value = value;
    this.isActive = isActive;
  }
}