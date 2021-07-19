package solver;

import java.util.*;
import java.util.stream.Collectors;

public class StochasticBestFirstSearch {
    State root;
    State bestState;
    int bestScore;
    int maxScore;
    int maxDepth;
    int numVisited;
    boolean uniformSamping;

    static int MAX_VISITED;
    Map<String, State> stateQueue;

    StochasticBestFirstSearch(int n, int queueSize, int maxDepth, boolean uniformSampling) {
        root = new State(n, 0); // root is at depth 0
        maxScore = n*n;
        bestScore = maxScore;
        stateQueue = new HashMap<>();
        this.maxDepth = maxDepth;

        numVisited = 0;
        MAX_VISITED = queueSize;
        this.uniformSamping = uniformSampling;
    }

    public State epoch() { // dfs up to a specified depth
        Random rg = new Random();
        addState(root);

        while (numVisited++ <= MAX_VISITED) {
            if (numVisited%1000==0) {
                System.out.println(String.format("Visited %d states", numVisited));
                System.out.println("#states remaining in queue " + stateQueue.size());
            }

            State x = sample(); // sample() or take_best
            if (x==null)
                break; // no more states to sample from!
            if (x.depth==maxDepth)
                continue; // depth too large... select another at lower depth

            boolean mode = rg.nextBoolean();

            update(x);  // update bestState

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
                    addState(next);
                }
            }
        }
        return bestState;
    }

    void addState(State x) {
        String signature = x.areaMultiSet2String();
        if (stateQueue.containsKey(signature)) {
            return; // have seen a similar state before!
        }
        else {
            stateQueue.put(signature, x);
        }
    }

    void update(State x) {
        stateQueue.remove(x.areaMultiSet2String());
        // update the best state
        int score = x.getScore();
        if (score < bestScore || bestState==null) {
            bestScore = score;
            bestState = x;
        }
    }

    State sample() { // proportional to the goodness score of the state
        if (stateQueue.isEmpty())
            return null;

        //return this.stateQueue.values().stream().sorted().collect(Collectors.toList()).get(0);

        ///*
        List<State> sortedStates = this.stateQueue.values().stream().sorted().collect(Collectors.toList());

        // after sorting the values towards the beginning of the list are likely to be good solutions
        int[] scores = new int[sortedStates.size()];
        int i=0;
        for (State s: sortedStates)
            scores[i++] = State.maxScore - s.getScore(); // convert min to max

        int sampled = Sampler.sample(scores, uniformSamping);
        return sortedStates.get(sampled);
        //*/
    }
}
