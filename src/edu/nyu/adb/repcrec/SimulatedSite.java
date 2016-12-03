package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimulatedSite {
  final int siteNum;
  boolean isDown;
  final TransactionManager manager;
  HashMap<String, ItemInfo> database;
  List<LockInfo> lockTable;
  //List<Lock> waitForReadyReadTable; // if necessary, store all the read transactions waiting for the variable to become ready

  public SimulatedSite(int index, TransactionManager manager) {
    this.siteNum = index;
    this.isDown = false;
    this.manager = manager;
    this.database = new HashMap<String, ItemInfo>();
    this.lockTable = new ArrayList<LockInfo>();
  }

  public int siteIndex() {
    return siteNum;
  }

  public void put(ItemInfo v) {
    database.put(v.key, v);
  }

  /**
   * fail a site
   * 
   * when a site fails,
   * mark all the locks on this site as inactive before erasing the lock table, 
   * so that other objects will know the lock has been released
   * also abort all the transaction which hold locks on this site
   */
  public void fail() {
    isDown = true;
    System.out.println("Site " + siteNum + " failed");

    for (LockInfo lock : lockTable) {
      lock.isActive = false;
      manager.abort(manager.transactionList.get(lock.transaction.name));
    }
    lockTable.clear();
  }

  /**
   * recover a site
   */
  public void recover() {
    isDown = false;
    System.out.println("Site " + siteNum + " recovered");
    // if the variable is not replicated, mark ready_for_read as true,
    // otherwise, mark it false
    for (ItemInfo v : database.values()) {
      if (!manager.isReplicated(v.key)) {
        v.isReadyForRead = true;
      } else {
        v.isReadyForRead = false;
      }
    }
  }

  public void addLock(LockInfo lock) {
    lockTable.add(lock);
  }

  /**
   * print the all variables and values in the site
   */
  public void dump() {
    for (int i = 1; i <= 20; i++) {
      StringBuilder temp = new StringBuilder();
      temp.append("x");
      temp.append(i);
      if (database.containsKey(temp.toString())) {
        ItemInfo v = database.get(temp.toString());
        if (v.isReadyForRead) {
          manager.print(v.key, siteNum, v.value);
        }
      }
    }
  }

  public void dump(String key) {
    if (database.containsKey(key)) {
      ItemInfo v = database.get(key);
      manager.print(v.key, siteNum, v.value);
    }
  }
}