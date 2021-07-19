package solver;

import java.util.*;

public class IterativeDeepeningExplorer {
    State root;
    State bestState;
    int bestScore;
    int maxScore;
    float epsilon;
    int depth
    Map<String, State> stateSignatures;

    IterativeDeepeningExplorer(int n, float epsilon, int depth) {
        root = new State(n);
        maxScore = n*n;
        this.epsilon = epsilon;
        bestScore = maxScore;
        stateSignatures = new HashMap<>();
        this.depth = depth;
    }

    public State epoch() { // dfs up to a specified depth
        visit(root, depth, false);
        return bestState;
    }

    public void visit(State x, int depth, boolean mode) { // x is the parent state

        String signature = x.areaMultiSet2String();
        if (stateSignatures.containsKey(signature)) {
            System.out.println("Skipping state with signature " + signature);
            return; // have seen a similar state before!
        }
        else {
            stateSignatures.put(signature, x);
        }

        int score = x.getScore();
        if (score < bestScore || bestState==null) {
            bestScore = score;
            bestState = x;
        }
        if (depth == this.depth)
            return; // have traversed till max-depth

        for (Rect r : x.blocks) {
            int max = mode? r.y + r.w : r.x + r.h;

            // create a new state and recursively visit that node
            // (if the depth is less than max-depth)
            // new state should be created vertically (if the current one is horizontal)
            for (int mid=1; mid <= max/2; mid++) {
                RectPair rp = r.split(mode, mid);
                if (rp==null)
                    continue;

                State next = new State(x, r, rp, mode);
                next.addConstraintViolationPenalty();

                // we always explore if the child state is feasible or child is a better node
                // else we explore it stochastically with prob = epsilon
                /*
                boolean toExplore = next.score==maxScore || next.score>x.score?
                        (float)Math.random() < epsilon? true: false:
                    true;
                 */
                float p = (float)Math.random();
                //float q = (float)Math.random();
                boolean toExplore = p<epsilon; // || q < (1 - next.score/((float)State.maxScore));
                if (toExplore) {
                    System.out.println(x.toString() + " ---> " + next.toString());
                    visit(next, depth+1, !mode);
                }
            }
        }
    }

}
