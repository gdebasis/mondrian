package solver;
import java.util.*;
import java.util.stream.*;

class RectPair {
	Rect a, b;

	RectPair(Rect a, Rect b) { this.a = a; this.b = b; }
}

class Sampler {
	static int sample(int[] x) { // x is sorted
		float[] probs = new float[x.length];
		int sum = IntStream.of(x).sum();
 	
		for (int i=0; i<x.length; i++)
			probs[i] = x[i]/(float)sum;

		float y = 0;
		float r = (float)Math.random();

		for (int i=0; i<x.length-1; i++) {
			y += probs[i];
			if (r < y)
				return i;
		}
		return x.length-1; 
	}
}

class BlockByPosComprator implements Comparator<Rect> {
	public int compare(Rect a, Rect b) {
		int a_pos = a.x*a.x + a.y*a.y;
		int b_pos = a.x*a.x + a.y*a.y;
		return Integer.compare(a_pos, b_pos);
	}
}

class MondrianSolver {

	static void solveByStochasticSearch(int n) throws Exception {
		int NUM_EPOCHS = 100;
		StochasticExplorer se = new StochasticExplorer(n); // StochasticExplorer(20);

		for (int i=0; i<NUM_EPOCHS; i++) {
			se.epoch();
			se.sortStates();
			se.printBest();
		}
		se.toSVG(null);
	}

	static void solveByIterDeep(int n, int depth, float epsilon) throws Exception {
		IterativeDeepeningExplorer se = new IterativeDeepeningExplorer(n, epsilon);
		State bestState = se.epoch(depth);
		System.err.println(
			String.format("Best state: %s",
			bestState.toString())
		);
		se.toSVG(bestState);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println("usage MondrianSolver <n> <maxdepth> <prob of exploring an infeasible/worse state>");
			return;
		}
		solveByIterDeep(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Float.parseFloat(args[2]));

		//old code: solveByStochasticSearch();
	}
}
