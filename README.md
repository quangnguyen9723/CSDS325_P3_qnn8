# CSDS325_P3_qnn8
# Distance vector routing

Java version: Java 18

To run the project, do the following steps

    cd src/
    javac *.java (you might also need to cd into message folder and node folder)    

Then open 7 terminal sessions, 1 for server and 6 for nodes.
You have to start the server first.
In the src/ folder, in each terminal session:

    java Server
    java node/NodeU
    java node/NodeV
    java node/NodeW
    java node/NodeX
    java node/NodeY
    java node/NodeZ

The printed-out result at each of the node's terminal is the finalized dv table 
