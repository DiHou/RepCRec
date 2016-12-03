package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {
  final String name;
  final int initTime;
  final boolean isReadOnly;
  TransactionManager manager;
  List<LockInfo> locksHolding;
  HashMap<String, int[]> snapshot;

  public Transaction(String name, int initTime, boolean readOnly, TransactionManager manager) {
    this.name = name;
    this.initTime = initTime;
    this.isReadOnly = readOnly;
    this.manager = manager;
    locksHolding = new ArrayList<LockInfo>();
    
    if (readOnly) {
      snapshot = new HashMap<>();
      for (int i = 1; i <= 20; i++) {
        String item = "x" + i;
        SimulatedSite[] sites = manager.sites;
        
        for (int j = 0; j < sites.length; j++) {
          if (sites[j].database.containsKey(item)) {
            snapshot.put(item, new int[] {sites[j].database.get(item).value, j + 1});
            break;
          }
        }
      }
    }
  }
  
  public void addLock(LockInfo lock) {
    locksHolding.add(lock);
  }

  /**
   * actually commit write operations when a transaction commits
   */
  public void commitWrites() {
    for (LockInfo lockInfo : locksHolding) {
      if (lockInfo.isActive && lockInfo.lockType == LockType.WRITE) {
        lockInfo.itemInfo.value = lockInfo.value;
        lockInfo.itemInfo.isReadyForRead = true;
      }
    }
  }

  public void releaseLocks() {
    for (LockInfo lock : locksHolding) {
      lock.isActive = false;
      lock.itemInfo.update();
    }
  }
}