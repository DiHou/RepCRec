Run with Java 8


If on CIMS machine, invoke Java 8 by typing:
$: module load java-1.8

Confirm version by:
$: javac -version
$: java -version



Execution
1. cd to current directory RepCRecDB_ZH

2. compile:
$: javac src/edu/nyu/adb/repcrec/*.java

3. run:
$: java -cp src edu.nyu.adb.repcrec.RepCRecMain /path/to/testfile.txt

4. if want to redirect output to a file:
$: java -cp src edu.nyu.adb.repcrec.RepCRecMain /path/to/testfile.txt > /path/to/outputfile.txt
