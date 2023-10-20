How to run TPCMain:
Server:
mvn clean
mvn compile
mvn exec:java -Dexec.mainClass=bgu.spl.net.BGSServer.TPCMain -Dexec.args=7777
Client:
make clean
make
bin/BGSclient "10.0.2.15" 7777

How to run ReactorMain
Server:
mvn clean
mvn compile
mvn exec:java -Dexec.mainClass="bgu.spl.net.BGSServer.ReactorMain" -Dexec.args="7777 5"
Client:
make clean
make
bin/BGSclient "10.0.2.15" 7777

Command lines for examples:
REGISTER ohad barzilay 01-01-1990
LOGIN albert einstein 14-03-1879
LOGOUT
FOLLOW 0 omer
POST hello world
PM alon hello
LOGSTAT
STAT
BLOCK kadosh

Filtered words are stroed in: bgu/spl/net/api/bidi/impl.BidiMessagingProtocolImpl.java in lines 208-213 in pmMsg function
