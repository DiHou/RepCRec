package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Site class contains all the variables and functions needed for a site in
 * a distributed database, including the locks. Each database have 10 sites.
 * 
 *
 */
public class SimulatedSite {
  int index;
  TransactionManager tm;
  boolean isDown;
  HashMap<String, ItemInfo> variableList; // stores the variables on the site
  List<LockInfo> lockTable; // stores all the locks on the variables of this site
  //List<Lock> waitForReadyReadTable; // if necessary, store all the read transactions waiting for the variable to become ready

  /**
   * site constructor
   * 
   * @param index
   */
  public SimulatedSite(int index, TransactionManager tm) {
    this.index = index;
    this.tm = tm;
    isDown = false;
    variableList = new HashMap<String, ItemInfo>();
    lockTable = new ArrayList<LockInfo>();
  }

  /**
   * get the site index
   * 
   * @return site index
   */
  public int siteIndex() {
    return index;
  }

  /**
   * add a variable into variableList
   * 
   * @param variable
   */
  public void add(ItemInfo v) {
    variableList.put(v.key, v);
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
    System.out.println("Site " + index + " failed");

    for (LockInfo lock : lockTable) {
      lock.isActive = false;
      tm.abort(tm.transactionList.get(lock.transaction.name));
    }
    lockTable.clear();
  }

  /**
   * recover a site
   */
  public void recover() {
    isDown = false;
    System.out.println("Site " + index + " recovered");
    // if the variable is not replicated, mark ready_for_read as true,
    // otherwise, mark it false
    for (ItemInfo v : variableList.values()) {
      if (!tm.isReplicated(v.key)) {
        v.isReadyForRead = true;
      } else {
        v.isReadyForRead = false;
      }
    }
    //if (!lockTable.isEmpty())
  }

  /**
   * add a lock into lockTable
   * 
   * @param lock
   */
  public void placeLock(LockInfo lock) {
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
      if (variableList.containsKey(temp.toString())) {
        ItemInfo v = variableList.get(temp.toString());
        if (v.isReadyForRead) {
          tm.print(v.key, index, v.value);
        }
      }
    }
  }

  public void dump(String key) {
    if (variableList.containsKey(key)) {
      ItemInfo v = variableList.get(key);
      tm.print(v.key, index, v.value);
    }
  }
}