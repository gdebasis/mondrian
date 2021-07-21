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
    boolean uniformSampling;

    static final float EPSILON = 0.1f; // prob. of including an infeasible state in the beam search

    int beamSize;
    static int MAX_QUEUE;
    static int MAX_NUMVISITED;
    Map<String, State> stateQueue;

    StochasticBestFirstSearch(int n, Properties prop) {

        int maxDepth = Integer.parseInt(prop.getProperty("maxdepth", "10"));
        MAX_QUEUE = Integer.parseInt(prop.getProperty("maxqueue_size", "1000"));
        MAX_NUMVISITED = Integer.parseInt(prop.getProperty("maxstates_to_explore", "1000"));
        beamSize = Integer.parseInt(prop.getProperty("beamsize", "20"));
        uniformSampling = prop.getProperty("sampling", "biased").equals("uniform");

        root = new State(n, 0); // root is at depth 0
        maxScore = n*n;
        bestScore = maxScore;
        stateQueue = new HashMap<>();
        StochasticBestFirstSearch.maxDepth = maxDepth;

        numVisited = 0;
    }

    public State epoch() { // dfs up to a specified depth
        State x, next;
        boolean[] modes = {false, true};
        List<State> beam = new ArrayList<>();

        addState(root);

        while (stateQueue.size() <= MAX_QUEUE) {
            numVisited++;
            if (numVisited %100 ==0) {
                System.err.print(
                    String.format("Visited %d states; #states remaining in queue: %d\r",
                            numVisited, stateQueue.size()));
            }
            if (numVisited == MAX_NUMVISITED)
                break;

            x = sample(); // sample()
            if (x==null)
                break; // no more states to sample from!

            update(x);  // update bestState
            if (x.depth==maxDepth) {
                System.out.println("MAX-DEPTH exceeded: Skipping state " + x.toString());
                continue; // depth too large... don't explore further
            }

            beam.clear();
            // Generate next states
            for (boolean mode: modes) {
                for (Rect r : x.blocks) {
                    int max = mode ? r.y + r.w : r.x + r.h;
                    max = max/2;

                    // create a new state and recursively visit that node
                    // (if the depth is less than max-depth)
                    // new state should be created vertically (if the current one is horizontal)
                    for (int mid = 1; mid <= max; mid++) {
                        RectPair rp = r.split(mode, mid);
                        if (rp != null) {
                            next = new State(x, r, rp);
                            beam.add(next);
                        }
                    }
                }
            }

            List<State> topK = beam.stream()
                    .sorted()
                    .limit(beamSize)
                    .collect(Collectors.toList()); // first K - the best states

            List<Integer> scores = topK.stream()
                .map(State::getScore)
                .collect(Collectors.toList());
            System.out.println("Selected scores of top states: " + scores);

            for (State s: topK) {
                addState(s); // add to state-queue
            }
        }

        System.err.println(String.format("Updating through the remaining %d states in queue...", stateQueue.size()));

        // See if any node in our queue contains a better score
        for (State s: stateQueue.values()) {
            if (s.isInfeasible())
                continue;
            int score = s.getScore();
            if (score < bestScore) {
                bestScore = score;
                bestState = s;
            }
        }
        return bestState;
    }

    void addState(State x) {
        if (x.isInfeasible()) {
            System.out.println(String.format("State [%s] is infeasible", x.toString()));
            float p = (float)Math.random();
            if (p > EPSILON)
                return; // Prob. of not adding = 1-EPSILON
        }

        String signature = x.areaSignature();
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
        if (x.isInfeasible())
            return;

        System.out.println("Done visiting a feasible state " + x.toString());
        System.out.println("State removed: " + stateQueue.remove(x.areaSignature()) + " #states = " + stateQueue.size());

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

        if (uniformSampling) {
            int size = stateQueue.size();
            int rindex = (int)(Math.random()*size);
            return this.stateQueue.values().stream().skip(rindex).findFirst().get();
        }

        List<State> sortedStates = this.stateQueue.values().stream()
                .sorted()
                .limit(beamSize)
                .collect(Collectors.toList());

        // after sorting the values towards the beginning of the list are likely to be good solutions
        int[] scores = new int[sortedStates.size()];
        int i=0;
        for (State s: sortedStates)
            scores[i++] = State.maxScore - s.getScore(); // convert min to max

        int sampled = Sampler.sample(scores);
        return sortedStates.get(sampled);
    }
}
