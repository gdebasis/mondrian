package solver;

import java.util.stream.IntStream;

public class Sampler {
    static int sample(int[] x, boolean uniform) { // x is sorted
        if (uniform) {
            return (int)(Math.random() * x.length);
        }

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


