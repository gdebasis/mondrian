package solver;

import java.util.Comparator;

// If the area's comprise of numbers that involve many factors (e.g. 24=6x4, 12x2, 3x8), then such
// a state is more preferable

public class FactorsHeuristic implements Comparator<State> {

    static final int[] primes = {
            2, 3, 5, 7, 11,
    //        13, 17, 19,
    //        23, 29, 31, 37, 41, 43, 47, 53,
    //        59, 61, 67, 71, 73, 79, 83, 89, 97
    };

    static int findExponent(int n, int p) {
        int e = 0;
        while (n%p == 0) {
            n = n/p;
            e++;
        }
        return e;
    }

    static public float avgNumFactors(State x) {
        int totalNumFactors = 0;
        for (Rect r: x.blocks) {
            for (int p: primes)
                totalNumFactors += findExponent(r.area, p);
        }
        return totalNumFactors/(float)x.blocks.size();
    }

    @Override
    public int compare(State a, State b) {
        return Float.compare(a.compositeHeuristicScore(), b.compositeHeuristicScore());
    }

    public static void main(String[] args) {
        System.out.println(FactorsHeuristic.findExponent(12, 2));
        System.out.println(FactorsHeuristic.findExponent(12, 3));
    }
}



