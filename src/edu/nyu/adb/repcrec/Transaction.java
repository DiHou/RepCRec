package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Package access level, not intended to expose for public use.
 *  
 * @author yanghui
 */
class Transaction {
  final String name;
  final int initTime;
  final boolean isReadOnly;
  TransactionManager manager;
  ArrayList<LockInfo> locksHolding;
  HashMap<String, int[]> dbSnapshot;

  Transaction(String name, int initTime, boolean readOnly, TransactionManager manager) {
    this.name = name;
    this.initTime = initTime;
    this.isReadOnly = readOnly;
    this.manager = manager;
    this.locksHolding = new ArrayList<LockInfo>();
    
    if (readOnly) {
      createDatabaseSnapshot();
    }
  }
  
  private void createDatabaseSnapshot() {
    this.dbSnapshot = new HashMap<>();
    
    for (int i = 1; i <= 20; i++) {
      String item = "x" + i;
      SimulatedSite[] sites = manager.sites;
      
      for (int j = 0; j < sites.length; j++) {
        if (sites[j].database.containsKey(item)) {
          dbSnapshot.put(item, new int[] {sites[j].database.get(item).value, j + 1});
          break;
        }
      }
    }
  }
  
  void commitReadsAndWrites() {  // Defer write commits until now, when the transaction commits.
    for (LockInfo lockInfo : locksHolding) {
      if (lockInfo.isValid && lockInfo.lockType == LockType.WRITE) {
        lockInfo.itemInfo.value = lockInfo.value;
        lockInfo.itemInfo.isReadReady = true;
      } else if (lockInfo.lockType == LockType.READ) {   //lockInfo.isValid && 
//        System.out.println("I am in commit read");
//        System.out.println(lockInfo.value);
//lockInfo.itemInfo.key + ": " + lockInfo.value + " at site " + lockInfo.site.siteID
        System.out.print("Read by " + lockInfo.transaction.name + ", ");
        print(lockInfo.itemInfo.key, lockInfo.itemInfo.value, lockInfo.site.siteID);
      }
    }
  }

  void releaseLocks() {  // Release locks and update item lock status.
    for (LockInfo lock : locksHolding) {
      lock.isValid = false;
      lock.itemInfo.updateItemLockStatus();
    }
  }
  
  void print(String key, int value, int siteNumber) {
    System.out.println(key + ": " + value + " at site " + siteNumber);
  }
}