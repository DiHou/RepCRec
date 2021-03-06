package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Package access level, not intended to expose for public use.
 *  
 * @author di
 */
class Transaction {
  final String name;
  final int initTime;
  final boolean isReadOnly;
  TransactionManager manager;
  ArrayList<LockInfo> locksHolding;
  HashMap<String, KeyValueRO> dbSnapshot;  // for read-only transaction
  ArrayList<KeyValueRO> readsOfRO;  // for read-only transaction

  Transaction(String name, int initTime, boolean readOnly, TransactionManager manager) {
    this.name = name;
    this.initTime = initTime;
    this.isReadOnly = readOnly;
    this.manager = manager;
    this.locksHolding = new ArrayList<LockInfo>();
    
    if (readOnly) {
      createDatabaseSnapshot();
      readsOfRO = new ArrayList<>();
    }
  }

  private void createDatabaseSnapshot() {
    this.dbSnapshot = new HashMap<>();
    
    for (int i = 1; i <= 20; i++) {
      String item = "x" + i;
      SimulatedSite[] sites = manager.sites;
      
      for (int j = 0; j < sites.length; j++) {
        if (sites[j].database.containsKey(item)) {
          dbSnapshot.put(item, new KeyValueRO (item, sites[j].database.get(item).value, j + 1));
          break;
        }
      }
    }
  }

  // Defer all read and write until now, when the transaction commits.
  void commitReadsAndWrites() {
    for (LockInfo lockInfo : locksHolding) {
      if (lockInfo.isValid && lockInfo.lockType == LockType.WRITE) {
        lockInfo.itemInfo.value = lockInfo.value;
        lockInfo.itemInfo.isReadReady = true;
      } else if (lockInfo.isValid && lockInfo.lockType == LockType.READ) {
        System.out.printf("*   %s: %d, site: %d\n", lockInfo.itemInfo.key, lockInfo.itemInfo.value, 
            lockInfo.site.siteID);
      }
    }
  }

  // Commit reads of read-only transaction
  void commitReadsAndWritesRO() {
    for (KeyValueRO keyValue: readsOfRO) {
      System.out.println(keyValue);
    }
  }

  // Release locks and update item lock status.
  void releaseLocks() {
    for (LockInfo lock : locksHolding) {
      lock.isValid = false;
      lock.itemInfo.updateItemLockStatus();
    }
  }
}