package solver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class State implements Comparable<State> {
    List<Rect> blocks;
    Map<Integer, Integer> multiSetArea; // area --> freq map
    int score;
    int depth; // depth in the exploration tree

    // global variables to be shared across all instances
    static int n;
    static int maxScore;

    State(int n, int depth) { // root state with no partitions
        blocks = new ArrayList<>();
        if (State.n==0) { // do it only once
            State.n = n;
            State.maxScore = n*n;
        }

        score = maxScore;
        Rect board = new Rect(0, 0, n, n);
        this.blocks.add(board);
        multiSetArea = new TreeMap<>();
        multiSetArea.put(score, 1);
        this.depth = depth;
    }

    State(State that, Rect parent, RectPair children, boolean generatedVertically) {
        blocks = new ArrayList<>();
        blocks.addAll(that.blocks);
        this.multiSetArea = new HashMap<>(that.multiSetArea);

        this.addBlock(children.a);
        this.addBlock(children.b);

        this.removeBlock(parent);
        this.score = computeScore();
        this.depth = that.depth+1;  // child is deeper by 1 level
    }

    public int compareTo(State that) {
        return Integer.compare(this.getScore(), that.getScore());
    }

    int getScore() { return score; }

    private int computeScore() {
        if (blocks.size()==1) return maxScore;

        int max = multiSetArea.keySet().stream().max(Integer::compare).get();
        int min = multiSetArea.keySet().stream().min(Integer::compare).get();

        score = max-min;
        return score;
    }

    // There's no point of exploring a state that has the same
    // signature in terms of the multiset representation of the areas
    // Return a string signature of the multiset so that we know
    // what states to avoid exploring
    String areaMultiSet2String() {
        List<Integer> keys = multiSetArea.keySet().stream().sorted().collect(Collectors.toList());
        return keys.toString();
    }

    void addConstraintViolationPenalty() { // add penalty if applicable
        for (int freq: this.multiSetArea.values()) {
            if (freq > 1)
                score = maxScore; // override the score to maxvalue (nxn)
        }
    }

    boolean isInfeasible() {
        for (int freq: this.multiSetArea.values()) {
            if (freq > 1)
                return true;
        }
        return false;
    }

    void addBlock(Rect r) { // warning: calling function needs to ensure that the block is unique
        blocks.add(r);

        Integer freq = multiSetArea.get(r.area);
        if (freq == null)
            freq = new Integer(0);

        multiSetArea.put(r.area, new Integer(freq+1));
    }

    void removeBlock(Rect r) {
        this.blocks.remove(r);
        Integer freq = multiSetArea.get(r.area); // this can't be null
        if (freq == 1)
            multiSetArea.remove(r.area); // no redundant map of the form of area -> 0
        else
            multiSetArea.put(r.area, new Integer(freq-1));
    }

    // Generate all possible neighbors from a given state
    List<State> expand() {
        List<State> neighbors = new LinkedList<>();
        for (Rect r: blocks) {
            neighbors.addAll(expand(r, false));
            neighbors.addAll(expand(r, true));
        }
        return neighbors;
    }

    List<State> expand(Rect r, boolean vertical) {
        List<State> neighbors = new LinkedList<>();
        int max = vertical? r.y + r.w : r.x + r.h;

        for (int mid = 1; mid <= max/2; mid++) {
            RectPair rp = r.split(vertical, mid);
            // this part of the code makes sure that we always stay in the feasible solution space! Revisit later
            if (blocks.contains(rp.a) || blocks.contains(rp.b))
                continue;
            if (rp.a.congruent(rp.b))
                continue; // can't add both as they're congruent.. also makes sure that solution is feasible
            State next = new State(this, r, rp, vertical);
            neighbors.add(next);
        }

        return neighbors;
    }

    String toSVG(int SCALE_FACTOR) {
        StringBuffer buff = new StringBuffer();
        buff.append(String.format("<svg width=\"%d\" height=\"%d\">\n", n*SCALE_FACTOR, n*SCALE_FACTOR));

        for (Rect r: blocks)
            buff.append(r.toSVG(SCALE_FACTOR)).append("\n");

        buff.append("</svg>");

        return buff.toString();
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Rect r: this.blocks)
            buff.append(r.toString()).append("|");
        if (buff.length()>1) buff.deleteCharAt(buff.length()-1);
        buff.append(String.format(" (score = %d)", getScore()));
        return buff.toString();
    }

    public static void toSVG(State bestState) throws IOException {
        String outFile = String.format("solutions/mondrian-%d-%d.htm", n, n);

        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>%dx%d - solution = %d: %s </div>",
                bestState.n, bestState.n, bestState.score, bestState.blocks.toString()));
        bw.write("<br><br>");

        bw.write(bestState.toSVG(20));
        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }

    public int A_star_Heuristic() { // higher the better
        return StochasticBestFirstSearch.maxDepth - depth + blocks.size();
    }
}

