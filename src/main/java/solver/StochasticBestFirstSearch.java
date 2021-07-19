package solver;

import java.util.*;
import java.util.stream.Collectors;

class HeuristicStateEvaluator implements Comparator<State> {

    // This is the goodness of a state with a heuristic that
    // a state is favoured if the number of rectangles is large and
    // depth is small. The sample() function selects the top-k
    @Override
    public int compare(State a, State b) {
        return Integer.compare(
            a.A_star_Heuristic(),
            b.A_star_Heuristic()
        );
    }
}

public class StochasticBestFirstSearch {
    State root;
    State bestState;
    int bestScore;
    int maxScore;
    static int maxDepth;
    int numVisited;
    boolean uniformSamping;

    int beamSize;
    static int MAX_VISITED;
    Map<String, State> stateQueue;

    StochasticBestFirstSearch(int n, int queueSize, int maxDepth, boolean uniformSampling, int K) {
        root = new State(n, 0); // root is at depth 0
        maxScore = n*n;
        bestScore = maxScore;
        stateQueue = new HashMap<>();
        StochasticBestFirstSearch.maxDepth = maxDepth;

        numVisited = 0;
        MAX_VISITED = queueSize;
        this.uniformSamping = uniformSampling;
        this.beamSize = K;
    }

    public State epoch() { // dfs up to a specified depth
        State x, next;
        boolean[] modes = {false, true};
        List<State> children = new ArrayList<>();

        addState(root);

        while (numVisited++ <= MAX_VISITED) {
            if (numVisited%1000==0) {
                System.err.println(String.format("Visited %d states", numVisited));
                System.err.println("#states remaining in queue " + stateQueue.size());
            }

            x = sample(); // sample()
            if (x==null)
                break; // no more states to sample from!
            if (x.depth==maxDepth)
                continue; // depth too large... select another at lower depth

            update(x);  // update bestState

            children.clear();
            // Generate next states
            for (boolean mode: modes) {
                for (Rect r : x.blocks) {
                    int max = mode ? r.y + r.w : r.x + r.h;

                    // create a new state and recursively visit that node
                    // (if the depth is less than max-depth)
                    // new state should be created vertically (if the current one is horizontal)
                    for (int mid = 1; mid <= max / 2; mid++) {
                        RectPair rp = r.split(mode, mid);
                        if (rp != null) {
                            next = new State(x, r, rp, mode);
                            next.addConstraintViolationPenalty();
                            children.add(next);
                        }
                    }
                }
            }

            List<State> topK = children.stream()
                    .sorted()
                    .limit(beamSize)
                    .collect(Collectors.toList()); // first K - the best states
            for (State s: topK) {
                addState(s); // add to state-queue
            }
        }

        System.err.println(String.format("Updating through the remaining %d states in queue...", stateQueue.size()));

        // See if any node in our queue contains a better score
        for (State s: stateQueue.values()) {
            int score = s.getScore();
            if (score < bestScore) {
                bestScore = score;
                bestState = s;
            }
        }

        return bestState;
    }

    void addState(State x) {
        String signature = x.areaMultiSet2String();
        State seen = stateQueue.get(signature);
        if (seen != null) {
            System.out.println(
                String.format(
                    "State [%s] not stored because it is equivalent to a stored state [%s]",
                    x.toString(), seen.toString()));
            return; // have seen a similar state before!
        }
        else {
            stateQueue.put(signature, x);
        }
    }

    void update(State x) {
        System.out.println("Done visiting state " + x.toString());
        System.out.println("State removed: " + stateQueue.remove(x.areaMultiSet2String()) + " #states = " + stateQueue.size());

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

        List<State> sortedStates = this.stateQueue.values().stream()
                .sorted()
                .limit(beamSize)
                .collect(Collectors.toList());

        // after sorting the values towards the beginning of the list are likely to be good solutions
        int[] scores = new int[sortedStates.size()];
        int i=0;
        for (State s: sortedStates)
            scores[i++] = State.maxScore - s.getScore(); // convert min to max

        int sampled = Sampler.sample(scores, uniformSamping);
        return sortedStates.get(sampled);
    }
}
