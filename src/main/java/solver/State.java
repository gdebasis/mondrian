package solver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;

class AreaFreq {
    HashMap<String, Integer> map; // w:h --> freq (>1 implies infeasible state)

    AreaFreq() {
        map = new HashMap<>();
    }

    AreaFreq(AreaFreq that) {
        map = new HashMap<>(that.map);
    }

    AreaFreq(List<Rect> rects) {
        this();
        for (Rect r: rects) {
            update(r, 1);
        }
    }

    void update(Rect r, int delta) {
        String key = r.key();
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
        String key = r.key();
        Integer c = map.get(key);
        return (c==null)? 0: c;
    }
}

public class State implements Comparable<State> {
    List<Rect> blocks;
    AreaFreq areaFreq;
    int score;
    int depth; // depth in the exploration tree
    Set<Integer> areaSet;   // set of areas of blocks of this state -- quick equivalence check

    enum Direction { RIGHT, LEFT, TOP, BOTTOM};

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

    State(final State that, final List<Rect> blocks) {
        this.blocks = blocks;
        areaSet = blocks.stream().map(Rect::getArea).distinct().collect(Collectors.toSet());
        areaFreq = new AreaFreq(blocks);

        this.score = computeScore();
        this.depth = that.depth+1;  // child is deeper by 1 level
    }

    State(final State that, Rect parent, List<Rect> children) {
        blocks = new ArrayList<>();
        blocks.addAll(that.blocks);

        areaFreq = new AreaFreq(that.areaFreq);
        areaSet = new TreeSet<>(that.areaSet);

        for (Rect child: children)
            this.addBlock(child);

        this.removeBlock(parent);

        for (Rect child: children)
            areaSet.add(child.area);

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
            buff.append(r.toSVGColor(SCALE_FACTOR, 3, "black")).append("\n");

        List<Rect> grid = new ArrayList<>(n*n);
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                grid.add(new Rect(i, j, 1, 1));
            }
        }
        for (Rect r: grid)
            buff.append(r.toSVG(SCALE_FACTOR, 1, "black")).append("\n");

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

    float compositeHeuristicScore() {
        // a high numFactors will bring this ratio down which is desirable
        // because the sample() function works by selecting the first-K values
        return this.score/FactorsHeuristic.avgNumFactors(this);
    }

    static List<State> mergeAllHorizontally(final State s, int i, Direction d) {
        List<State> mergedStates = new LinkedList<>();
        State s_dash = s;

        if (d == Direction.LEFT) {
            s_dash = s.reflectHorizontally();
        }
        final Rect key = s_dash.blocks.get(i);

        List<Rect> adjRects = s_dash.blocks
                .stream()
                .filter(p -> p.y==key.y+key.w)  // only those rectangles that 'touch' the key
                .collect(Collectors.toList())
                ;

        for (Rect q: adjRects) {
            mergedStates.add(mergeWithRight(s_dash, key, q));
        }

        if (d == Direction.LEFT) { // reflect back
            mergedStates = mergedStates.stream().map(State::reflectHorizontally).collect(Collectors.toList());
        }
        return mergedStates;
    }

    State reflectHorizontally() {
        State s = new State(State.n, this.depth);
        s.blocks.clear();

        for (Rect r: this.blocks) {
            s.blocks.add(r.getReflectedHorizontally());
        }

        s.areaFreq = this.areaFreq;
        s.score = this.score;
        s.areaSet = this.areaSet;
        return s;
    }

    static State mergeWithRight(State s, Rect key, Rect q) {
        List<Rect> mergedRects = new ArrayList<>(s.blocks); // copy the existing rectangles

        mergedRects.remove(q);
        mergedRects.remove(key);

        int newRect_x_start, newRect_x_end;
        Rect key_top = null, q_top = null, q_bottom = null, key_bottom = null;

        newRect_x_start = max(q.x, key.x);
        newRect_x_end = min(q.x+q.h, key.x+key.h);
        Rect newRect = new Rect(newRect_x_start, key.y, key.w + q.w, newRect_x_end - newRect_x_start);
        mergedRects.add(newRect);

        if (newRect_x_start > key.x) {
            key_top = new Rect(key.x, key.y, key.w, newRect_x_start-key.x);
            mergedRects.add(key_top);
        }
        else {
            q_top = new Rect(q.x, q.y, q.w, newRect_x_start-q.x);
            mergedRects.add(q_top);
        }
        if (newRect_x_end < q.x+q.h) {
            q_bottom = new Rect(newRect_x_end, q.y, q.w, q.x+q.h-newRect_x_end);
            mergedRects.add(q_bottom);
        }
        else {
            key_bottom = new Rect(newRect_x_end, key.y, key.w, key.x+key.h-newRect_x_end);
            mergedRects.add(key_bottom);
        }

        State newState = new State(s, mergedRects);
        return newState;
    }

    public static void toSVG(State bestState) throws IOException {
        final int MAX = 600;
        String outFile = String.format("solutions/mondrian-%d-%d.htm", n, n);

        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>%dx%d - solution = %d: %s </div>",
                bestState.n, bestState.n, bestState.score, bestState.toString()));
        bw.write("<br><br>");

        bw.write(bestState.toSVG(MAX/n));
        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }

    public static void main(String[] args) throws IOException {
        int SCALE_FACTOR = 20;
        FileWriter fw;
        BufferedWriter bw;

        fw = new FileWriter("beforemerge.htm");
        bw = new BufferedWriter(fw);
        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");

        State s = new State(7, 0);
        s.blocks.clear();
        int p = 4;

        s.blocks.add(new Rect(0, 0, 6, 2));
        s.blocks.add(new Rect(2, 0, 4, 2));
        s.blocks.add(new Rect(4, 0, 4, 1));
        s.blocks.add(new Rect(5, 0, 4, 2));
        s.blocks.add(new Rect(2, 4, 2, 5));
        s.blocks.add(new Rect(0, 6, 1, 3));
        s.blocks.add(new Rect(3, 6, 1, 1));
        s.blocks.add(new Rect(4, 6, 1, 3));

        System.out.println(s.blocks);
        bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", State.n*SCALE_FACTOR, State.n*SCALE_FACTOR));

        State s_ref = s.reflectHorizontally();
        for (Rect x: s.blocks) {
            bw.write(x.toSVG(SCALE_FACTOR, 1, "black"));
            bw.newLine();
        }

        bw.write("</svg></body>\n</html>");
        bw.close();
        fw.close();

        fw = new FileWriter("aftermerge.htm");
        bw = new BufferedWriter(fw);
        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", State.n*SCALE_FACTOR, State.n*SCALE_FACTOR));

        List<State> mergedStates = mergeAllHorizontally(s, p, Direction.LEFT);
        State merged = mergedStates.get(2);

        for (Rect x: merged.blocks) {
            bw.write(x.toSVG(SCALE_FACTOR, 1, "black"));
            bw.newLine();
        }
        System.out.println(merged.blocks);

        bw.write("</svg></body>\n</html>");
        bw.close();
        fw.close();
    }
}

