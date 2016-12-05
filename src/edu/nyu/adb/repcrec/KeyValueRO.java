package edu.nyu.adb.repcrec;

class KeyValueRO {
  
  String key;
  int value;
  int site;
  
  KeyValueRO(String key, int value, int site) {
    this.key = key;
    this.value = value;
    this.site = site;
  }
  
  @Override
  public String toString() {
    return String.format("*   %s: %d", key, value);
  }
}
