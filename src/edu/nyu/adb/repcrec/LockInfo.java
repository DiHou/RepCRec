package edu.nyu.adb.repcrec;

/**
 * The Lock class goes beyond the general concept of lock because it can either
 * represent a lock currently being held, or a lock which hasn't been acquired
 * yet, in latter case, it serves as a container storing the information which
 * will be used later
 *
 */
class LockInfo {
  Transaction transaction;
  ItemInfo variable;
  String type; // either "Read" or "Write"
  SimulatedSite site;
  boolean isActive;
  int value; // the value to be written if it's a write lock

  /**
   * when a lock is created, its pointer will be stored in three objects: the
   * variable, the site and the transaction. In this way, we can much more
   * easily handle the cases when a site is failed or a transaction aborts by
   * simply marking the lock as inactive
   */
  public LockInfo(Transaction transaction, ItemInfo variable, SimulatedSite site,
      String type, int value, boolean isActive) {
    this.transaction = transaction;
    this.variable = variable;
    this.site = site;
    this.value = value;
    this.isActive = isActive;
    this.type = type;
  }
}