package edu.nyu.adb.repcrec;

import java.io.BufferedReader;
import java.io.FileReader;

class QueryParser {
  int time = 0;
  TransactionManager tm;
  
  QueryParser(TransactionManager tm) {
    this.tm = tm;
  }
  
  public void startParsing(String file) {
    String line = null;
    
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("//")) {
          continue;
        }
        parseLine(line, true);
      }
    } catch (Exception e) {
      // Do nothing, simply return.
    }
  }

  private boolean parseLine(String line, boolean shouldAddTime) {
    if (shouldAddTime) {
      time++;
    }
    
    line = line.replaceAll(" ", "");
    
    if (line.contains(";")) {
      String[] opArray = line.split(";");
      for (String op : opArray) {
        parseLine(op, false);
      }
    } else if (line.startsWith("begin(")) {
      tm.begin(line.substring(6, line.length() - 1), time, false);
    } else if (line.startsWith("beginRO(")) {
      tm.begin(line.substring(8, line.length() - 1), time, true);
    } else if (line.contains("R(")) {
      callRead(line);
    } else if (line.contains("W(")) {
      callWrite(line);
    } else if (line.startsWith("end(")) {
      tm.end(tm.transactionList.get(line.substring(4, line.length() - 1)), true);
    } else if (line.contains("fail(")) {
      tm.fail(findSiteID(line));
    } else if (line.contains("recover(")) {
      tm.recover(findSiteID(line));
    } else if (line.startsWith("dump()")) {
      tm.dump();
    } else if (line.startsWith("dump(x")) {
      tm.dump(findXID(line));
    } else if (line.startsWith("dump(")) {
      SimulatedSite[] sites = tm.sites;
      sites[findSiteID(line) - 1].dump();
    }
    
    return true;
  }

  /**
   * find the Transaction ID in an input command
   * 
   * @param string
   * @return TID
   */
//  private String findTID(String s) {
//    int firstP = s.indexOf("(");
//    int lastP = s.indexOf(")");
//
//    return s.substring(firstP + 1, lastP);
//  }

  /**
   * find the X Position ID in an input command
   * 
   * @param string
   * @return x position ID
   */
  private String findXID(String s) {
    int firstP = s.indexOf("(");
    int lastP = s.indexOf(")");

    return s.substring(firstP + 1, lastP);
  }

  /**
   * find the Site ID in an input command
   * 
   * @param string
   * @return site ID
   */
  private int findSiteID(String s) {
    int firstP = s.indexOf("(");
    int lastP = s.indexOf(")");

    String siteId = s.substring(firstP + 1, lastP);

    return Integer.parseInt(siteId);
  }

  /**
   * find the variables in a read command, and call read function in Action
   * class
   * 
   * @param string
   */
  public void callRead(String s) {
    int firstP = s.indexOf("(");
    int lastP = s.indexOf(")");

    String variablesString = s.substring(firstP + 1, lastP);
    String[] variables = variablesString.split(",");

    tm.read(variables[0], variables[1]);

  }

  /**
   * find the variables in a write command, and call write function in Action
   * class
   * 
   * @param string
   */
  public void callWrite(String s) {
    int firstP = s.indexOf("(");
    int lastP = s.indexOf(")");

    String variablesString = s.substring(firstP + 1, lastP);
    String[] variables = variablesString.split(",");

    tm.write(variables[0], variables[1], Integer.parseInt(variables[2]));
  }
}