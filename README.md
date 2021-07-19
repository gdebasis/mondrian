## Mondrian Solver

A Java implementation (work in progress)

To build the project install `maven` and execute
```
mvn compile
```

The algorithm implmented is a stochastic version of the best-first search with a bound on the number of states to visit.

To run the main class, simply execute the script with the following argument structure
```
run.sh <n> <max-depth> <max #states to visit> <uniform sampling (true/false)>
```
For example, a sample invocation is
```
run.sh 8 4 500 false > nohup.out
```
where tile an 8x8 square with stochastic depth-first search (max depth to 4), with 500 max number of states to visit and using biased sampling to probailistically select the next state to visit.

The shell script also opens the browser with a rendering of the solution.

The shell script works on Mac with the Chrome browser. For a different browser/OS, please change the last line of `run.sh`. 

![8x8 square](sample.png)

