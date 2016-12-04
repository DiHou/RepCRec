package edu.nyu.adb.repcrec;

class Unfinished {
  
  String transactionName;
  boolean isRead;
  String key;
  int value;
  
  Unfinished (String transactionName, boolean isRead, String item) {
    this(transactionName, true, item, -1);
  }
  
  Unfinished (String transactionName, boolean isRead, String key, int value) {
    this.transactionName = transactionName;
    this.isRead = isRead;
    this.key = key;
    this.value = value;
  }
}
