package solver;
import java.util.*;

class RectPair {
	Rect a, b;

	RectPair(Rect a, Rect b) { this.a = a; this.b = b; }
}

class BlockByPosComprator implements Comparator<Rect> {
	public int compare(Rect a, Rect b) {
		int a_pos = a.x*a.x + a.y*a.y;
		int b_pos = b.x*b.x + b.y*b.y;
		return Integer.compare(a_pos, b_pos);
	}
}

class MondrianSolver {

	static void solveByIterDeep(int n, int maxDepth, int queueSize, boolean uniformSampling) throws Exception {
		StochasticBestFirstSearch se = new StochasticBestFirstSearch(n, queueSize, maxDepth, uniformSampling);
		State bestState = se.epoch();
		System.err.println(
			String.format("Best state: %s",
			bestState.toString())
		);
		State.toSVG(bestState);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.err.println("usage MondrianSolver <n> <maxdepth> <prob of exploring an infeasible/worse state>");
			return;
		}
		solveByIterDeep(
				Integer.parseInt(args[0]),
				Integer.parseInt(args[1]),
				Integer.parseInt(args[2]),
				Boolean.parseBoolean(args[3])
		);
	}
}
