package edu.nyu.adb.repcrec;

/**
 * This is the lock information stored in lockTable of each SimulatedSite.
 * Package access level, not intended to expose for public use.
 *  
 * @author yanghui
 */
class LockInfo {
  Transaction transaction;
  ItemInfo itemInfo;
  SimulatedSite site;
  LockType lockType;
  int value;
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