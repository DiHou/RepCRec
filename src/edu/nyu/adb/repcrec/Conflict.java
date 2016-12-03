package edu.nyu.adb.repcrec;

class Conflict {
  
  String waiting;  // the transaction waiting for a lock (blocked)
  String waited;  // the transaction holding the lock (read or write)
  
  Conflict(String waiting, String waited) {
    this.waiting = waiting;
    this.waited = waited;
  }
  
  public boolean equals(Object objectToCompare) {
    if (objectToCompare == null || objectToCompare.getClass() != getClass()) {
      return false;
    }
    
    if (this == objectToCompare) {
      return true;
    }
    
    Conflict other = (Conflict) objectToCompare;
    return this.waiting.equals(other.waiting) && this.waited.equals(other.waited);
  }
}
