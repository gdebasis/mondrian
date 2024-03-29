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

        areaSet = new TreeSet<>();
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
        return toSVG(SCALE_FACTOR, false);
    }

    String toSVG(int SCALE_FACTOR, boolean color) {
        StringBuffer buff = new StringBuffer();
        buff.append(String.format("<svg width=\"%d\" height=\"%d\">\n", n*SCALE_FACTOR, n*SCALE_FACTOR));

        for (Rect r : blocks)
            buff.append(
                color && r.x>0 || r.y>0? r.toSVGColor(SCALE_FACTOR, 5, "black"):
                        r.toSVG(SCALE_FACTOR, 3, "black")
                ).append("\n");

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

    public static List<State> mergeAll(final State s, int i) { // ith rect is the pivot
        State s_dash = null;
        List<State> mergedStates = new LinkedList<>();

        for (Direction d: Direction.values()) {
            if (d==Direction.RIGHT)
                s_dash = s;
            else if (d==Direction.LEFT)
                s_dash = s.reflectHorizontally();
            else if (d==Direction.TOP)
                s_dash = s.rotate(true);
            else if (d==Direction.BOTTOM)
                s_dash = s.rotate(false);

            mergedStates.addAll(mergeAllAlongRight(s_dash, i));
        }
        // No need to reflect/rotate back because states are equivalent
        return mergedStates;
    }

    // Merge along right... to merge along other directions the input is transformed
    static List<State> mergeAllAlongRight(final State s, int i) {
        List<State> mergedStates = new LinkedList<>();
        final Rect key = s.blocks.get(i);

        List<Rect> adjRects = s.blocks
                .stream()
                .filter(p -> p.y==key.y+key.w)  // only those rectangles that 'touch' the key
                .collect(Collectors.toList())
                ;

        for (Rect q: adjRects) {
            mergedStates.add(mergeAlongRight(s, key, q));
        }
        return mergedStates;
    }

    State rotate(boolean antiClockwise) {  // top===right, bottom===left
        State s = new State(State.n, this.depth);
        for (Rect r: this.blocks) {
            if (antiClockwise)
                s.blocks.add(r.getReflectedHorizontally().getRotated());
            else
                s.blocks.add(r.getRotated().getReflectedHorizontally());
        }

        s.areaFreq = this.areaFreq;
        s.score = this.score;
        s.areaSet = this.areaSet;
        return s;
    }

    State reflectHorizontally() {
        State s = new State(State.n, this.depth);
        for (Rect r: this.blocks) {
            s.blocks.add(r.getReflectedHorizontally());
        }

        s.areaFreq = this.areaFreq;
        s.score = this.score;
        s.areaSet = this.areaSet;
        return s;
    }

    static State mergeAlongRight(State s, Rect key, Rect q) {
        int d;
        List<Rect> mergedRects = new ArrayList<>(s.blocks); // copy the existing rectangles

        mergedRects.remove(q);
        mergedRects.remove(key);

        int newRect_x_start, newRect_x_end;
        Rect key_top = null, q_top = null, q_bottom = null, key_bottom = null;

        newRect_x_start = max(q.x, key.x);
        newRect_x_end = min(q.x+q.h, key.x+key.h);
        Rect newRect = new Rect(newRect_x_start, key.y, key.w + q.w, newRect_x_end - newRect_x_start);
        mergedRects.add(newRect);

        d = newRect_x_start - key.x;
        if (d > 0) {
            key_top = new Rect(key.x, key.y, key.w, d);
            mergedRects.add(key_top);
        }
        else if (d <= 0) {
            q_top = new Rect(q.x, q.y, q.w, newRect_x_start-q.x);
            mergedRects.add(q_top);
        }

        d = newRect_x_end - (q.x+q.h);
        if (d < 0) {
            q_bottom = new Rect(newRect_x_end, q.y, q.w, -d);
            mergedRects.add(q_bottom);
        }
        else if (d >= 0) {
            key_bottom = new Rect(newRect_x_end, key.y, key.w, key.x+key.h-newRect_x_end);
            mergedRects.add(key_bottom);
        }

        State newState = new State(s, mergedRects);
        return newState;
    }

    public static void toSVG(State bestState, boolean color) throws IOException {
        final int MAX = 600;
        String outFile = String.format("solutions/mondrian-%d-%d.htm", n, n);

        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        List<Rect> sortedRects = bestState.blocks.stream().sorted(Rect::compareTo).collect(Collectors.toList());

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>%dx%d solution: Score = %d (%s), #Rectangles = %d</div>",
                bestState.n, bestState.n, bestState.score, sortedRects, bestState.blocks.size()));
        bw.write("<br><br>");

        bw.write(bestState.toSVG(MAX/n, color));
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
        int p = 2;

        s.blocks.add(new Rect(0, 0, 6, 2));
        s.blocks.add(new Rect(2, 0, 4, 2));
        s.blocks.add(new Rect(4, 0, 4, 1));
        s.blocks.add(new Rect(5, 0, 4, 2));
        s.blocks.add(new Rect(2, 4, 2, 5));
        s.blocks.add(new Rect(0, 6, 1, 3));
        s.blocks.add(new Rect(3, 6, 1, 1));
        s.blocks.add(new Rect(4, 6, 1, 3));

        s.computeScore();
        System.out.println(s);
        bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", State.n*SCALE_FACTOR, State.n*SCALE_FACTOR));

        State s_ref = s.reflectHorizontally();
        for (Rect x: s.blocks) {
            bw.write(x.toSVG(SCALE_FACTOR, 1, "black"));
            bw.newLine();
        }

        bw.write("</svg></body>\n</html>");
        bw.close();
        fw.close();

        System.out.println("Merged states:");
        List<State> mergedStates = mergeAllAlongRight(s, p);

        int i = 1;
        for (State merged: mergedStates) {
            System.out.println(merged);

            fw = new FileWriter(String.format("aftermerge-%d.htm", i++));
            bw = new BufferedWriter(fw);

            bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
            bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", State.n * SCALE_FACTOR, State.n * SCALE_FACTOR));

            for (Rect x : merged.blocks) {
                bw.write(x.toSVG(SCALE_FACTOR, 1, "black"));
                bw.newLine();
            }

            bw.write("</svg></body>\n</html>");
            bw.close();
            fw.close();
        }
    }
}

