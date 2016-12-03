package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;

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
  
  /**
   * Actually commit write operations when a transaction commits.
   */
  void commitWrites() {
    for (LockInfo lockInfo : locksHolding) {
      if (lockInfo.isActive && lockInfo.lockType == LockType.WRITE) {
        lockInfo.itemInfo.value = lockInfo.value;
        lockInfo.itemInfo.isReadyForRead = true;
      }
    }
  }

  void releaseLocks() {
    for (LockInfo lock : locksHolding) {
      lock.isActive = false;
      lock.itemInfo.update();
    }
  }
}