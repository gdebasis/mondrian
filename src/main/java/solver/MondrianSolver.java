package solver;
import java.io.FileReader;
import java.util.*;

class RectPair {
	Rect a, b;
	RectPair(Rect a, Rect b) { this.a = a; this.b = b; }
}

class MondrianSolver {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("usage MondrianSolver <n> <init.properties>");
			return;
		}

		Properties prop = new Properties();
		prop.load(new FileReader("init.properties"));

		StochasticBestFirstSearch se = new StochasticBestFirstSearch(Integer.parseInt(args[0]), prop);

		State bestState = se.epoch();
		System.err.println(
				String.format("Best state: %s",
						bestState.toString())
		);
		State.toSVG(bestState);
	}
}
