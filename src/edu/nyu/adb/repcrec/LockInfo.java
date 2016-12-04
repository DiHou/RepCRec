package edu.nyu.adb.repcrec;


class LockInfo {
  Transaction transaction;
  ItemInfo itemInfo;
  SimulatedSite site;
  LockType lockType;
  int value;        // the value to write for the write lock
  boolean isValid;  // When transaction is finished or site is down, the lock becomes invalid.
  
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