## Mondrian Solver

A Java implementation (work in progress)

To build the project install `maven` and execute
```
mvn compile
```

The main class requires two arguments - i) the size of the sqauare to tile, and ii) the number of initial vertical bars (towers) to generate a solution.
An example invocation is
```
mvn exec:java@solver -Dexec.args="10 4"
```
where we want to tile a 10x10 square with 4 pivot rectangles. Changing this number would yield different solutions.

The program creates an HTML file named `mondrian-NxN_final.htm`, where N is the square size (e.g. 10). You can open this file on a browser to visualize the solution, like the one shown below.

![12x12 square](sample.png)
