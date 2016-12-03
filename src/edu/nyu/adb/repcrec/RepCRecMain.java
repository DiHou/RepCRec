package edu.nyu.adb.repcrec;

public class RepCRecMain {

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
