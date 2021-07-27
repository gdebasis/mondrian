package solver;

import java.util.*;
import java.util.stream.Collectors;

public class StochasticBestFirstSearch {
    State root;
    State bestState;
    int bestScore;
    int maxScore;
    static int maxDepth;
    int numVisited;

    boolean uniformSampling;
    boolean spiralSplit;
    boolean toMerge;
    int mergeMinDepth;
    int spiralMaxDepth;

    static final boolean DEBUG = true;
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
        spiralSplit = Boolean.parseBoolean(prop.getProperty("gen.spiral", "false"));
        toMerge = Boolean.parseBoolean(prop.getProperty("gen.merge", "false"));
        mergeMinDepth = Integer.parseInt(prop.getProperty("merge.mindepth", "4"));
        spiralMaxDepth = Integer.parseInt(prop.getProperty("spiral.maxdepth", "2"));
    }

    // beam is an o/p parameter
    void genNextStatesByBisection(State x, List<State> beam) {
        State next;
        boolean[] modes = {false, true};

        if (x.blocks.isEmpty()) {
            x.blocks.add(new Rect(0, 0, State.n, State.n));
            x.areaSet.add(State.maxScore);
        }

        // Generate next states
        for (boolean mode: modes) {
            for (Rect r : x.blocks) {
                int max = mode ? r.y + r.w : r.x + r.h;
                max = max>>1;

                // create a new state and recursively visit that node
                // (if the depth is less than max-depth)
                // new state should be created vertically (if the current one is horizontal)
                for (int mid = 1; mid <= max; mid++) {
                    List<Rect> subRects = r.biSectionSplit(mode, mid);
                    if (subRects != null) {
                        next = new State(x, r, subRects);
                        beam.add(next);
                    }
                }
            }
        }
    }

    // beam is an o/p parameter
    List<State> genNextStatesBySpiralEnclosure(State x) {
        State next;
        List<State> subStates = new LinkedList<>();

        // Generate next states by spiral transformation for the rect with max area
        Rect r = x.blocks.stream().max(Rect::compareTo).get();

        int xmin = r.x + 1;
        int ymin = r.y+1;
        int xmax = r.x+r.h-1;
        int ymax = r.y+r.w-1;

        for (int i=xmin; i<xmax; i++) {
            for (int j=ymin; j<ymax; j++) {
                for (int p=xmin+1; p<=xmax; p++) {
                    for (int q=ymin+1; q<=ymax; q++) {

                        List<Rect> subRects = r.spiralSplit(i, p, j, q);
                        if (subRects == null)
                            continue;
                        next = new State(x, r, subRects);

                        subStates.add(next);
                    }
                }

            }
        }

        return subStates;
    }

    public State epoch() { // dfs up to a specified depth
        State x, next;
        boolean[] modes = {false, true};
        List<State> beam = new ArrayList<>();

        addState(root);

        while (stateQueue.size() <= MAX_QUEUE) {
            numVisited++;
            if (numVisited%10==0)
                System.err.print(
                    String.format("Visited %d states; #states remaining in queue: %d\r",
                        numVisited, stateQueue.size()));

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

            genNextStatesByBisection(x, beam);

            if (spiralSplit && x.depth < spiralMaxDepth) {
                System.out.println("Generating spiral transformation states...");
                List<State> spiralTransformedStates = genNextStatesBySpiralEnclosure(x);
                beam.addAll(spiralTransformedStates); // add more states
                System.out.println("Added " + spiralTransformedStates.size() + " states as spiral transformation");
            }

            // Generate all merged states
            if (toMerge && x.depth > mergeMinDepth) {
                System.out.println("Generating merged states...");
                for (int i = 0; i < x.blocks.size(); i++) {
                    List<State> mergedStates = State.mergeAll(x, i);
                    beam.addAll(mergedStates);
                }
            }

            System.out.println("Total states to select from: " + beam.size());
            // favour states where the areas are highly composite numbers -- not too useful... removed
            List<State> topK = beam.stream()
                    //.sorted(new FactorsHeuristic())
                    .sorted()
                    .limit(beamSize)
                    .collect(Collectors.toList()); // first K - the best states

            if (DEBUG) {
                List<Integer> scores = topK.stream()
                        .map(State::getScore)
                        .collect(Collectors.toList());
                System.out.println("Selected scores of top states: " + scores);
            }

            System.out.println("Adding states to queue...");
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
