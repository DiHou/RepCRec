package edu.nyu.adb.repcrec;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * Package access level, not intended to expose for public use.
 *  
 * @author di
 */
class QueryParser {
  int time = 0;  // logical time
  final TransactionManager manager;
  
  QueryParser(TransactionManager manager) {
    this.manager = manager;
  }
  
  void startParsing(String file) {
    String readLine = null;
    
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      while ((readLine = reader.readLine()) != null) {
        if (!readLine.startsWith("//")) {  // Skip comments.
          parse(readLine, true);
        }
      }
    } catch (IOException ioe) {
      System.err.println("Error encountered when parse file.");
    }
  }

  private void parse(String input, boolean incrementTime) {
    if (incrementTime) {
      time++;
      
      // Try unfinished queries first, as a site may recover.
      executeUnfinished();
    }
    String query = input.replaceAll(" ", "");
    
    // Deadlock detection is needed only after read/write because other queries won't cause deadlock.
    if (query.contains(";")) {
      String[] queries = query.split(";");
      for (int i = 0; i < queries.length; i++) {
        parse(queries[i], false);
      }
    } else if (query.startsWith("begin(")) {
      manager.begin(query.substring(6, query.length() - 1), time, false);
    } else if (query.startsWith("beginRO(")) {
      manager.begin(query.substring(8, query.length() - 1), time, true);
    } else if (query.startsWith("R(")) {
      String[] splitted = query.substring(2, query.indexOf(")")).split(",");
      manager.read(splitted[0], splitted[1]);
      manager.deadLockCheckAndHandle();
    } else if (query.startsWith("W(")) {
      String[] splitted = query.substring(2, query.indexOf(")")).split(",");
      manager.write(splitted[0], splitted[1], Integer.parseInt(splitted[2]));
      manager.deadLockCheckAndHandle();
    } else if (query.startsWith("end(")) {
      manager.end(manager.transactionMapping.get(query.substring(4, query.length() - 1)), true);
    } else if (query.startsWith("fail(")) {
      manager.fail(Integer.parseInt(query.substring(5, query.length() - 1)));
    } else if (query.startsWith("recover(")) {
      manager.recover(Integer.parseInt(query.substring(8, query.length() - 1)));
    } else if (query.startsWith("dump()")) {
      manager.dump();
    } else if (query.startsWith("dump(x")) {
      manager.dump(query.substring(5, query.length() - 1));
    } else if (query.startsWith("dump(")) {
      manager.sites[Integer.parseInt(query.substring(5, query.length() - 1)) - 1].dump();
    } else {
      // ignore illegal query
    }
  }
  
  private void executeUnfinished() {
    HashSet<String> finished = new HashSet<>();
    for (Unfinished unfinished: manager.unfinished.values()) {
      boolean succeed = false;
      if (unfinished.isRead) {
        succeed = manager.read(unfinished.transactionName, unfinished.key);
      } else {
        succeed = manager.write(unfinished.transactionName, unfinished.key, unfinished.value);
      }
      
      if (succeed) {
        finished.add(unfinished.transactionName);
      }
    }
    
    for (String s: finished) {
      manager.unfinished.remove(s);
    }
  }
}