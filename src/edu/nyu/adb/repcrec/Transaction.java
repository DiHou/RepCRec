package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The transaction class is contains all the functions and variables needed for
 * the transaction for the distributed database, including locks.
 *
 */
public class Transaction {
  final String name;
  final boolean isReadOnly;
  final int initTime;
  TransactionManager tm;
  List<LockInfo> lockTable; // the locks the transaction is currently holding
  HashMap<String, Integer[]> readOnly; // stores all 'caches' for read-only

  /**
   * transaction constructor
   * 
   * @param name
   * @param time
   * @param b
   */
  public Transaction(String name, int initTime, boolean b, TransactionManager tm) {
    this.name = name;
    this.isReadOnly = b;
    this.initTime = initTime;
    this.tm = tm;
    lockTable = new ArrayList<LockInfo>();
    
    //get the values of the variables at this particular point,
    // according to multi-version concurrency control
    if (isReadOnly) {
      readOnly = new HashMap<String, Integer[]>();
      for (int i = 1; i <= 20; i++) {
        StringBuilder temp = new StringBuilder();
        
        temp.append('x');
        temp.append(i);
        String vName = temp.toString();
        SimulatedSite[] sites = tm.sites;
        
        for (int j = 0; j < sites.length; j++) {
          if (sites[j].variableList.containsKey(vName)) {
            readOnly.put(vName, new Integer[] {
                sites[j].variableList.get(vName).value, j + 1 });
            //System.out.printf("%d, %s\n",sites[j].getVariable(vName).getValue(), vName);
            
            break;
          }
        }
      }
      //System.out.println(readOnly.get("x1")[1]);
    }
  }

  /**
   * check if contains Read Only
   * 
   * @param vName
   * @return boolean if contains Read Only
   */
  public boolean containsReadOnly(String vName) {
    return readOnly.containsKey(vName);
  }
  
  /**
   * add a lock in lockTable
   */
  public void placeLock(LockInfo lock) {
    lockTable.add(lock);
  }

  /**
   * time getter
   * 
   * @return time
   */
//  public int getTime() {
//    return initTime;
//  }

  /**
   * realizeLocks
   * 
   * when a site commits, perform write operation if there's any
   */
  public void realizeLocks() {
    
    for (LockInfo lock : lockTable) {
      if (lock.isActive && lock.type.equals("Write")) {
        
        lock.variable.value = lock.value;
        
        //mark all the variables as ready_for_read
        lock.variable.isReadyForRead = true;
      }
    }
  }

  /**
   * nullifyLocks
   * 
   * when a site ends (whether commit or abort),
   * release all the locks by marking them as inactive
   */
  public void nullifyLocks() {
    for (LockInfo lock : lockTable) {
      lock.isActive = false;
      lock.variable.update();
    }
  }
}