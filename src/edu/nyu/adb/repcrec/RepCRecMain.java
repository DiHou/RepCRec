package edu.nyu.adb.repcrec;

/**
 * Only this main class is public, other classes are used only within this package.
 * 
 * @author Yanghui Zhuang, N16904649; Di Hou, N14316306
 * Date: 2016-12-04
 */
public class RepCRecMain {

  public static void main(String[] args) {
    if (args.length < 0) {
      return;
    }
    System.out.printf("Executing %s\n\n", args[0]);
    
    TransactionManager tm = new TransactionManager();
    tm.initialize();
    new QueryParser(tm).startParsing(args[0]);
  }
}
