package solver;

import java.util.List;

public class StateGenerator {
    static final int N = 32;
    static final int K = 10;

    static State genNextStatesByBisection(State x, boolean mode) {
        State next = null;

        if (x.blocks.isEmpty()) {
            x.blocks.add(new Rect(0, 0, State.n, State.n));
            x.areaSet.add(State.maxScore);
        }

        // Generate next states
        Rect r = x.blocks.get((int)(Math.random() * x.blocks.size()));
        if (r.w <= 2 || r.h <= 2)
            return null;

        int cut = mode ? r.y + r.w/2 : r.x + r.h/2;
        List<Rect> subRects = r.biSectionSplit(mode, cut);
        if (subRects != null) {
            next = new State(x, r, subRects);
        }
        return next;
    }

    public static void main(String[] args) {
        State s = new State(N, 0);
        State next = null;
        boolean mode;

        for (int k=0; k<K; k++) {
            //System.out.println(s);
            mode = Math.random() > 0.5;
            next = genNextStatesByBisection(s, mode);
            if (next==null) continue;
            s = next;
        }

        if (next != null) {
            //System.out.println(next);
            for (Rect r: next.blocks) {
                System.out.println(String.format("(%d,%d) %dx%d", r.x, r.y, r.w, r.h));
            }
        }
    }
}
