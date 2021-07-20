package solver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class AreaFreq {
    HashMap<String, Integer> map;

    AreaFreq() {
        map = new HashMap<>();
    }

    AreaFreq(AreaFreq that) {
        map = new HashMap<>(that.map);
    }

    String key(Rect r) { return r.w < r.h ? r.w + ":" + r.h: r.h + ":" + r.w; }

    void update(Rect r, int delta) {
        String key = key(r);
        Integer c = map.get(key);
        if (c==null)
            c = new Integer(0);

        c += delta;
        if (c > 0)
            map.put(key, c);
        else
            map.remove(key);
    }

    int getFreq(Rect r) {
        String key = key(r);
        Integer c = map.get(key);
        return (c==null)? 0: c;
    }
}

public class State implements Comparable<State> {
    List<Rect> blocks;
    AreaFreq areaFreq;
    int score;
    int depth; // depth in the exploration tree
    TreeSet<Integer> areaSet;   // set of areas of blocks of this state -- quick equivalence check

    // global variables to be shared across all instances
    static int n;
    static int maxScore;

    State(int n, int depth) { // root state with no partitions
        blocks = new ArrayList<>(n);
        if (State.n==0) { // do it only once
            State.n = n;
            State.maxScore = n*n;
        }

        score = maxScore;
        Rect board = new Rect(0, 0, n, n);
        this.blocks.add(board);

        areaSet = new TreeSet<>();
        areaSet.add(board.area);

        areaFreq = new AreaFreq();
        this.depth = depth;
    }

    State(final State that, Rect parent, RectPair children, boolean generatedVertically) {
        blocks = new ArrayList<>();
        blocks.addAll(that.blocks);

        areaFreq = new AreaFreq(that.areaFreq);
        areaSet = new TreeSet<>(that.areaSet);

        this.addBlock(children.a);
        this.addBlock(children.b);
        this.removeBlock(parent);

        areaSet.add(children.a.area);
        areaSet.add(children.b.area);
        areaSet.remove(parent.area);

        this.score = computeScore();
        this.depth = that.depth+1;  // child is deeper by 1 level
    }

    public int compareTo(State that) {
        int c = Integer.compare(this.getScore(), that.getScore());
        return c!=0? c: -1 * Integer.compare(this.depth, that.depth); // favour a higher depth
    }

    int getScore() { return score; }

    int computeScore() {
        if (blocks.size()==1) return maxScore;

        int max = blocks.stream().map(Rect::getArea).max(Integer::compareTo).get();
        int min = blocks.stream().map(Rect::getArea).min(Integer::compareTo).get();

        score = max-min;
        return score;
    }

    // There's no point of exploring a state that has the same
    // signature in terms of the multiset representation of the areas
    // Return a string signature of the multiset so that we know
    // what states to avoid exploring
    String areaSignature() {
        return areaSet.toString();
    }

    void addConstraintViolationPenalty() { // add penalty if applicable
        if (isInfeasible())
            score = maxScore;
    }

    boolean isInfeasible() {
        for (Rect r: blocks) {
            if (areaFreq.getFreq(r) > 1)
                return true;
        }
        return false;
    }

    void addBlock(Rect r) { // warning: calling function needs to ensure that the block is unique
        blocks.add(r);
        areaFreq.update(r, 1);
    }

    void removeBlock(Rect r) {
        this.blocks.remove(r);
        areaFreq.update(r, -1);
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
        List<Rect> sortedBlocks =
            this.blocks.stream().sorted()
            .collect(Collectors.toList());

        StringBuilder buff = new StringBuilder();
        for (Rect r: sortedBlocks)
            buff.append(r.toString()).append(" | ");
        if (buff.length()>1) buff.deleteCharAt(buff.length()-1);
        buff.append(String.format(" (score = %d) (depth = %d)", getScore(), depth));
        return buff.toString();
    }

    public static void toSVG(State bestState) throws IOException {
        String outFile = String.format("solutions/mondrian-%d-%d.htm", n, n);

        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>%dx%d - solution = %d: %s </div>",
                bestState.n, bestState.n, bestState.score, bestState.toString()));
        bw.write("<br><br>");

        bw.write(bestState.toSVG(50));
        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }

    public int A_star_Heuristic() { // higher the better
        return StochasticBestFirstSearch.maxDepth - depth + blocks.size();
    }
}

