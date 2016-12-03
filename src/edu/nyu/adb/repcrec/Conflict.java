package edu.nyu.adb.repcrec;

class Conflict {
  
  String waiting;  // the transaction waiting for a lock (blocked)
  String waited;  // the transaction holding the lock (read or write)
  
  Conflict(String waiting, String waited) {
    this.waiting = waiting;
    this.waited = waited;
  }
}
