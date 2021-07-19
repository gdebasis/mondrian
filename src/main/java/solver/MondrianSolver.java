package solver;
import java.io.FileReader;
import java.util.*;

class RectPair {
	Rect a, b;

	RectPair(Rect a, Rect b) { this.a = a; this.b = b; }
}

class MondrianSolver {

	static void solve(int n, int maxDepth, int queueSize, boolean uniformSampling, int beamSize) throws Exception {
		StochasticBestFirstSearch se = new StochasticBestFirstSearch
				(n, queueSize, maxDepth, uniformSampling, beamSize);
		State bestState = se.epoch();
		System.err.println(
			String.format("Best state: %s",
			bestState.toString())
		);
		State.toSVG(bestState);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("usage MondrianSolver <n> <init.properties>");
			return;
		}

		Properties prop = new Properties();
		prop.load(new FileReader("init.properties"));
		int maxDepth = Integer.parseInt(prop.getProperty("maxdepth", "10"));
		int maxStatesToVisit = Integer.parseInt(prop.getProperty("maxstates_to_visit", "1000"));
		int beamSize = Integer.parseInt(prop.getProperty("beamsize", "20"));
		boolean uniformSampling = prop.getProperty("sampling", "biased").equals("uniform");

		solve(
			Integer.parseInt(args[0]),
			maxDepth,
			maxStatesToVisit,
			uniformSampling,
			beamSize
		);
	}
}
