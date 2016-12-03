package edu.nyu.adb.repcrec;

import java.util.HashMap;
import java.util.List;

/**
 * The Manager class is use as a Transaction Manager, it has the main methods
 * needed to control transactions in the database.
 *
 */
public class TransactionManager {

  HashMap<String, Transaction> transactionList;
  SimulatedSite[] sites;

  public void print(String vName, int siteNumber, int value) {
    System.out.println(vName + ": " + value + " at site " + siteNumber);
  }

  public void initialize() {
    transactionList = new HashMap<String, Transaction>();
    sites = new SimulatedSite[10];
    
    for (int i = 0; i < 10; i++) {
      sites[i] = new SimulatedSite(i + 1, this);
      
      for (int j = 1; j <= 20; j++) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("x");
        builder.append(j);
        
        if (j % 2 == 0) {
          sites[i].add(new ItemInfo(builder.toString(), j * 10));
        } else if (i == 9 && (j == 9 || j == 19)) {
          sites[i].add(new ItemInfo(builder.toString(), j * 10));
        } else if (((j + 1) % 10) == i + 1) {
          sites[i].add(new ItemInfo(builder.toString(), j * 10));
        }
      }
    }
  }

  /**
   * determine whether a variable is replicated
   */
  public boolean isReplicated(String name) {
    int index = Integer.parseInt(name.substring(1, name.length()));
    return index % 2 == 0 ? true : false;
  }

  /**
   * instantiate a new transaction
   */
  public void begin(String name, int time, boolean isReadOnly) {
    transactionList.put(name, new Transaction(name, time, isReadOnly, this));
  }

  /**
   * perform read operation
   */
  public void read(String transaction, String key) {
    Transaction t = transactionList.get(transaction);
    if (t == null) {
      return;
    }
    
    // if the transaction is read-only type,
    // simply retrieve and return the 'cache' it stores
    if (t.isReadOnly) {
      if (t.containsReadOnly(key)) {
        System.out.print("Read by " + t.name + ", ");
        Integer[] array = t.readOnly.get(key);
        print(key, array[1], array[0]);
      } else {
        abort(t);
      }
    } else {
      int i = 0;
      
      for (; i < sites.length; i++) {
        // if not read-only type,
        // get the first working site which contains that variable
        if (!sites[i].isDown && sites[i].variableList.containsKey(key)) {
          ItemInfo v = sites[i].variableList.get(key);
          
          // need to check if the variable is ready for read,
          // and make sure it does not have a write lock on it
          if (v.isReadyForRead && !v.hasWriteLock()) {
            // get a new lock and put it inside the lock list of
            // that variable
            LockInfo lock = new LockInfo(t, v, sites[i], "Read", 0, true);
            
            v.placeLock(lock);
            sites[i].placeLock(lock);
            t.placeLock(lock);
            
            // print out the read value
            System.out.print("Read by " + t.name + ", ");
            print(v.key, i + 1, v.value);
            break;
            
            // if the variable has a write lock
          } else if (v.isReadyForRead) {
            // first check if the transaction holding the lock is the same with the current transaction,
            // if so, print out the value of that write lock even if the transaction has not committed
            // if not, then check if the current transaction should wait for the
            // transaction holding the write lock
            // also check if all the transactions in the wait list
            // of that variable are younger than the current
            // transaction,
            // because otherwise, there is no need to wait
            // if decide to wait, put the lock (which represents an
            // operation to be performed later) to the wait list
            if (v.getWriteLock().transaction.name.equals(t.name)) {
              LockInfo lock = new LockInfo(t, v, sites[i], "Read", 0, true);
              
              v.placeLock(lock);
              sites[i].placeLock(lock);
              t.placeLock(lock);
              System.out.print("Read by " + t.name + ", ");
              print(v.key, v.getWriteLock().site.siteIndex(), v.getWriteLock().value);
            }
            else if (v.getWriteLock().transaction.initTime > t.initTime && v.canWait(t)) {
              LockInfo lock = new LockInfo(t, v, sites[i], "Read", 0, true);
              
              v.waitList.add(lock);
              sites[i].placeLock(lock);
              t.placeLock(lock);
            } else {
              abort(t);
            }
            break;
          }
        }
      }
      
      // does not find a working site containing the variable, abort
      if (i == sites.length) {
        if (!isReplicated(key)) {
          for (int pos = 0; pos < sites.length; pos++) {
            if (sites[pos].variableList.containsKey(key) && sites[pos].isDown) {
              LockInfo lock = new LockInfo(t, sites[pos].variableList.get(key), sites[pos], "Read", 
                  0, true); 
              sites[pos].placeLock(lock);
              t.placeLock(lock);
              sites[pos].variableList.get(key).placeLock(lock);
              break;
            } 
          }
        }
        else {
          abort(t);
        }
      }
    }
  }

  public void write(String transaction, String key, int value) {
    Transaction t = transactionList.get(transaction);
    
    if (t == null) {
      return;
    }
    
    int i = 0;
    boolean shouldAbort = true;
    
    for (; i < sites.length; i++) {
      // need to check if the variable is ready for read,
      // and make sure it does not have any lock on it (except for the lock held by itself)
      if (!sites[i].isDown && sites[i].variableList.containsKey(key)) {
        ItemInfo v = sites[i].variableList.get(key);
        if (!v.hasLock()) {
          // if it does not have lock, get a new lock
          LockInfo lock = new LockInfo(t, v, sites[i], "Write", value, true);
          v.placeLock(lock);
          sites[i].placeLock(lock);
          t.placeLock(lock);
          shouldAbort = false;
        } else {
          List<LockInfo> lockList = v.getLockList();
          // if it already has a lock, first check if it is itself holding the lock
          int pos = 0;
          for (; pos < lockList.size(); pos++) {
            if (!lockList.get(pos).transaction.name.equals(t.name)) {
              break;
            }
          }
          // if it's not itself, decide if it should wait
          if (pos != lockList.size()) {
            for (LockInfo lock : lockList) {
              if (lock.transaction.initTime < t.initTime) {
                abort(t);
                break;
              }
            }
            if (!v.canWait(t)) {
              abort(t);
              break;
            }
          }
          
          // if it's only itself or it decides to wait,
          // get a new lock and put it in the lock list or wait list
          LockInfo lock = new LockInfo(t, v, sites[i], "Write", value, true);
          if (pos == lockList.size()) {
              v.placeLock(lock);
          }
          else {
            v.waitList.add(lock); 
          }
          sites[i].placeLock(lock);
          t.placeLock(lock);
          shouldAbort = false;
        }
      }
    }
    
    if (shouldAbort) {
      if (!isReplicated(key)) {
        for (int pos = 0; pos < sites.length; pos++) {
          if (sites[pos].variableList.containsKey(key) && sites[pos].isDown) {
            LockInfo lock = new LockInfo(t, sites[pos].variableList.get(key), sites[pos], "Write", 
                value, true); 
            sites[pos].placeLock(lock);
            t.placeLock(lock);
            sites[pos].variableList.get(key).placeLock(lock);
            break;
          } 
        }
      }
      else {
        abort(t);
      }
    }
  }

  public void abort(Transaction transaction) {
    end(transaction, false);
  }

  public void end(Transaction transaction, boolean toCommit) {
    if (transaction == null || !transactionList.containsKey(transaction.name)) {
      return;
    }
    
    if (toCommit) {
      System.out.println(transaction.name + " committed");
    } else {
      System.out.println(transaction.name + " aborted");
    }
    
    // difference between commit and abort is that if all the locks should
    // be 'realized', or simply discarded
    if (toCommit) {
      transaction.realizeLocks();
    }
    
    transaction.nullifyLocks();
    transactionList.remove(transaction.name);
  }

  public void fail(int siteNumber) {
    sites[siteNumber - 1].fail();
  }

  public void recover(int siteNumber) {
    sites[siteNumber - 1].recover();
  }

  /**
   * dump all items in each site
   */
  public void dump() {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump();
      System.out.println();
    }
  }

  /**
   * dump a specific item in each site
   */
  public void dump(String key) {
    for (int i = 0; i < sites.length; i++) {
      sites[i].dump(key);
    }
  }
  
  /**
   * testing 12 files together, for testing purpose
   */
  public void test12Files(){
    for (int i = 1; i <= 12; i++) {
       initialize();
      
       QueryParser parser = new QueryParser(this);
       StringBuilder temp = new StringBuilder();
      
       temp.append("input");
       temp.append(i);
       temp.append(".txt");
      
       String fileName = temp.toString();
      
       System.out.println("******Output " + i + "******");
       System.out.println();
       parser.startParsing(fileName);
       System.out.println();
       }
    
  }

  /**
   * program entrance
   */
  public static void main(String[] args) {
    // test12Files();
    
    if (args.length < 0) {
      return;
    }
    System.out.println("Input file: " + args[0]);
    
    TransactionManager tm = new TransactionManager();
    tm.initialize();
    new QueryParser(tm).startParsing(args[0]);
  }

}