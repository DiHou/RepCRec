package edu.nyu.adb.repcrec;

public class Conflict {
  
  String waiting;  // the transaction waiting for a lock (blocked)
  String waited;  // the transaction holding the lock (read or write)
  
}
