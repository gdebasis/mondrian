package solver;

import java.util.LinkedList;
import java.util.List;

public class IterativeDeepeningExplorer extends StochasticExplorer {
    State bestState;
    int bestScore;
    int maxScore;
    float epsilon;

    IterativeDeepeningExplorer(int n, float epsilon) {
        super(n);
        maxScore = n*n;
        this.epsilon = epsilon;
        bestScore = maxScore;
    }

    public State epoch(int depth) { // dfs up to a specified depth
        visit(getRoot(), depth);
        return bestState;
    }

    public State getRoot() {
        return smap.values().stream().findFirst().get(); // the only value;
    }

    public void visit(State x, int depth) { // x is the parent state

        int score = x.getScore();
        if (score < bestScore || bestState==null) {
            bestScore = score;
            bestState = x;
        }
        if (depth == 0)
            return; // have traversed till max-depth

        boolean[] modes = {false, true};
        for (boolean mode: modes) { // mode=0/1 vertical/horizontal
            for (Rect r : x.blocks) {
                int max = mode ? r.y + r.w : r.x + r.h;

                // create a new state and recursively visit that node
                // (if the depth is less than max-depth)
                // new state should be created vertically (if the current one is horizontal)
                for (int mid = 1; mid <= max / 2; mid++) {
                    RectPair rp = r.split(mode, mid);

                    State next = new State(x, r, rp, mode);
                    next.addConstraintViolationPenalty();

                    // we always explore if the child state is feasible or child is a better node
                    // else we explore it stochastically with prob = epsilon
                    boolean toExplore = next.score==maxScore || next.score>x.score?
                            (float)Math.random() < epsilon? true: false:
                    true;

                    if (toExplore) {
                        System.out.println(x.toString() + " ---> " + next.toString());
                        visit(next, depth - 1);
                    }
                }
            }
        }
    }

}
