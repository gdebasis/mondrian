package solver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class StochasticExplorer {
    Map<String, State> smap;
    int max;
    int n;
    List<State> sortedStates;

    StochasticExplorer(int n) {
        smap = new HashMap<>();
        this.n = n;
        this.max = n*n;
        // generate the initial state, which is an nxn square
        State initState = new State(n);
        smap.put(initState.key(), initState);
    }

    void addState(State x) {
        String key = x.key();
        if (!smap.containsKey(key))
            smap.put(key, x);
    }

    State sample() { // proportional to the goodness score of the state
        List<State> values = new ArrayList(smap.values());
        Collections.sort(values);

        // after sorting the values towards the beginning of the list are likely to be good solutions
        int[] scores = new int[values.size()];
        int i=0;
        for (State s: values)
            scores[i++] = max - s.getScore(); // convert min to max

        int sampled = Sampler.sample(scores);
        return values.get(sampled);
    }

    public void epoch() {
        State selectedState = sample(); // sample a state by max likelihood over goodness... we prefer to see better states
        List<State> neighbors = selectedState.expand();
        for (State n: neighbors) addState(n);
        smap.remove(selectedState.key());
    }

    void sortStates() {
        sortedStates = new ArrayList(smap.values());
        Collections.sort(sortedStates);
    }

    void printStates() { // sorted by the goodness scores
        System.out.println("Number of states: " + sortedStates.size());

        for (State s: sortedStates)
            System.out.print(String.format("%d ", s.score));
        System.out.println();
    }

    void printBest() {
        State bestState = sortedStates.get(0);
        System.out.println(bestState.blocks);
        System.out.println(bestState.score);
    }

    void toSVG(State bestState) throws IOException {
        String outFile = String.format("solutions/mondrian-%d-%d.htm", n, n);
        if (bestState == null)
            bestState = sortedStates.get(0);

        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>%dx%d - solution = %d: %s </div>",
                n, n, bestState.score, bestState.blocks.toString()));
        bw.write("<br><br>");

        bw.write(bestState.toSVG(20));
        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }
}



