package edu.nyu.adb.repcrec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Package access level, not intended to expose for public use.
 *  
 * @author di
 */
class SimulatedSite {
  final int siteID;
  final TransactionManager manager;
  HashMap<String, ItemInfo> database;
  List<LockInfo> lockTable;
  HashSet<Conflict> conflicts;
  boolean isDown;

  // Update conflicts when some transaction commits / aborts.
  void updateConflicts(String transactionName) {
    HashSet<Conflict> newConflicts = new HashSet<>();
    for (Conflict conflict: conflicts) {
      if (conflict.waiting.equals(transactionName) || conflict.waited.equals(transactionName)) {
        continue;
      }
      newConflicts.add(conflict);
    }
    conflicts = newConflicts;
  }

  SimulatedSite(int siteID, TransactionManager manager) {
    this.siteID = siteID;
    this.manager = manager;
    this.database = new HashMap<String, ItemInfo>();
    this.lockTable = new ArrayList<LockInfo>();
    this.conflicts = new HashSet<>();
    this.isDown = false;
  }

  // print all items
  void dump() {
    System.out.printf("dumping site %d...\n", siteID);
    
    for (int i = 1; i <= 20; i++) {
      String item = "x" + i;
      
      if (database.containsKey(item)) {
        ItemInfo itemInfo = database.get(item);
        if (itemInfo.isReadReady) {
          System.out.printf("- %s\n", itemInfo.toString());
        }
      }
    }
    
    System.out.println();
  }

  // print a specific item
  void dump(String key) {
    if (database.containsKey(key)) {
      ItemInfo itemInfo = database.get(key);
      if (itemInfo.isReadReady) {
        System.out.printf("- %s, site: %d\n", itemInfo.toString(), siteID);
      }
    }
  }
  
  // Mark all the locks invalid, clear lockTable and conflict when a site fails.
  void fail() {
    isDown = true;
    System.out.printf("Site %d failed.\n", siteID);
    
    for (LockInfo lock : lockTable) {
      lock.isValid = false;
      manager.abort(manager.transactionMapping.get(lock.transaction.name));
    }
    
    lockTable.clear();
    conflicts.clear();
  }

  void recover() {
    isDown = false;
    
    // Set un-replicated item isReadyReady.
    for (ItemInfo itemInfo : database.values()) {
      if (!isReplicated(itemInfo.key)) {
        itemInfo.isReadReady = true;
      } else {
        itemInfo.isReadReady = false;
      }
    }
    
    System.out.printf("Site %d is recovered.\n", siteID);
  }
  

  boolean isReplicated(String name) {
    int index = Integer.parseInt(name.substring(1, name.length()));
    return index % 2 == 0 ? true : false;
  }
}