## Mondrian Solver

A Java implementation (work in progress)

To build the project install `maven` and execute
```
mvn compile
```

To run the main class, simply execute the script with the following argument structure
```
run.sh <n> <depth> <probability of exploring a bad state>
```
For example, a sample invocation is
```
run.sh 8 4 0.1 > nohup.out
```
where tile an 8x8 square with stochastic depth-first search (max depth to 4) and the probability of exploring along a bad (infeasible/worse) state is 0.1.

The shell script also opens the browser with a rendering of the solution.

The shell script works on Mac with the Chrome browser. For a different browser/OS, please change the last line of `run.sh`. 

![8x8 square](sample.png)

