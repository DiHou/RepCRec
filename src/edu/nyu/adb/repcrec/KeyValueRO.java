package edu.nyu.adb.repcrec;

class KeyValueRO {
  
  String key;
  int value;
  int siteID;
  
  KeyValueRO(String key, int value, int siteID) {
    this.key = key;
    this.value = value;
    this.siteID = siteID;
  }
  
  @Override
  public String toString() {
    return String.format("*   %s: %d, site: %d", key, value, siteID);
  }
}
