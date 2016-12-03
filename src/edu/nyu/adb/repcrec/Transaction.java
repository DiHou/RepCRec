package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {
  final String name;
  final int initTime;
  final boolean isReadOnly;
  TransactionManager manager;
  List<LockInfo> lockTable; // the locks the transaction is currently holding
  HashMap<String, Integer[]> readOnly; // stores all 'caches' for read-only

  public Transaction(String name, int initTime, boolean isReadOnly, TransactionManager manager) {
    this.name = name;
    this.initTime = initTime;
    this.isReadOnly = isReadOnly;
    this.manager = manager;
    lockTable = new ArrayList<LockInfo>();
    
    //get the values of the variables at this particular point,
    // according to multi-version concurrency control
    if (isReadOnly) {
      readOnly = new HashMap<String, Integer[]>();
      for (int i = 1; i <= 20; i++) {
        String item = "x" + i;
        SimulatedSite[] sites = manager.sites;
        
        for (int j = 0; j < sites.length; j++) {
          if (sites[j].database.containsKey(item)) {
            readOnly.put(item, new Integer[] {sites[j].database.get(item).value, j + 1});
            //System.out.printf("%d, %s\n",sites[j].getVariable(vName).getValue(), vName);
            break;
          }
        }
      }
      //System.out.println(readOnly.get("x1")[1]);
    }
  }

  public boolean containsReadOnly(String item) {
    return readOnly.containsKey(item);
  }
  
  public void addLock(LockInfo lock) {
    lockTable.add(lock);
  }

  /**
   * actually commit write operations when a transaction commits
   */
  public void commitWrites() {
    for (LockInfo lockInfo : lockTable) {
      if (lockInfo.isActive && lockInfo.lockType == LockType.WRITE) {
        lockInfo.itemInfo.value = lockInfo.value;
        lockInfo.itemInfo.isReadyForRead = true;
      }
    }
  }

  public void releaseLocks() {
    for (LockInfo lock : lockTable) {
      lock.isActive = false;
      lock.itemInfo.update();
    }
  }
}