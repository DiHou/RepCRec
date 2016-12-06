package edu.nyu.adb.repcrec;

/**
 * Used for deadlock detection.
 * Package access level, not intended to expose for public use.
 * 
 * @author yanghui
 */
class Conflict {
  
  String waiting;  // the transaction waiting for a lock (blocked)
  String waited;   // the transaction holding the lock (read or write)
  
  Conflict(String waiting, String waited) {
    this.waiting = waiting;
    this.waited = waited;
  }
  
  @Override
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
  
  @Override
  public int hashCode() {
    return 31 * waiting.hashCode() + waited.hashCode();
  }
  
  @Override
  public String toString() {
    return String.format("Conflict: %s -> %s", waiting, waited);
  }
}
