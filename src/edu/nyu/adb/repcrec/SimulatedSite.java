package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimulatedSite {
  final int siteID;
  final TransactionManager manager;
  HashMap<String, ItemInfo> database;
  List<LockInfo> lockTable;
  boolean isDown;
  //List<Lock> waitForReadyReadTable; // if necessary, store all the read transactions waiting for the variable to become ready

  public SimulatedSite(int siteID, TransactionManager manager) {
    this.siteID = siteID;
    this.manager = manager;
    this.database = new HashMap<String, ItemInfo>();
    this.lockTable = new ArrayList<LockInfo>();
    this.isDown = false;
  }
  
  /**
   * when a site fails, mark all the locks on this site as inactive before erasing the lock table, 
   * so that other objects will know the lock has been released also abort all the transaction 
   * which hold locks on this site
   */
  public void fail() {
    isDown = true;
    System.out.println("Site " + siteID + " failed");

    for (LockInfo lock : lockTable) {
      lock.isActive = false;
      manager.abort(manager.transactionList.get(lock.transaction.name));
    }
    lockTable.clear();
  }

  public void recover() {
    isDown = false;
    for (ItemInfo itemInfo : database.values()) {
      if (!manager.isReplicated(itemInfo.key)) {
        itemInfo.isReadyForRead = true;
      } else {
        itemInfo.isReadyForRead = false;
      }
    }
    System.out.println("Site " + siteID + " recovered");
  }

  public void addLock(LockInfo lock) {
    lockTable.add(lock);
  }

  /**
   * print the all variables and values in the site
   */
  public void dump() {
    for (int i = 1; i <= 20; i++) {
      String item = "x" + i;
      if (database.containsKey(item)) {
        ItemInfo itemInfo = database.get(item);
        if (itemInfo.isReadyForRead) {
          manager.print(itemInfo.key, itemInfo.value, siteID);
        }
      }
    }
  }

  public void dump(String key) {
    if (database.containsKey(key)) {
      ItemInfo itemInfo = database.get(key);
      manager.print(itemInfo.key, itemInfo.value, siteID);
    }
  }
}