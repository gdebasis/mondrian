package solver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class RectQuadruple {
    Rect l, r, t, b; // left, top, right, bottom
    Rect inner, outer;

    RectQuadruple(Rect outer, Rect inner) {
        this.outer = outer; // the entire area that needs be tiled
        this.inner = inner; // a sub-rectangle acting as a pivot

        int dx = inner.x - outer.x;
        int dy = inner.y - outer.y;

        t = new Rect(outer.x, outer.y, dy+inner.w, dx);
        l = new Rect(inner.x, outer.y, dy, outer.h-t.h);
        b = new Rect(inner.x+inner.h, inner.y, outer.w-l.w, outer.h-(inner.h+t.h));
        r = new Rect(outer.x, inner.y+inner.w, outer.w-t.w, outer.h-b.h);
    }

    boolean isValid() {
        return t.isValid() && l.isValid() && b.isValid() && r.isValid();
    }

    static void testBisectionSplit() throws IOException {
        String outFile = "bisectiontest.htm";
        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        int SCALE_FACTOR = 50;
        Rect outer = new Rect(0, 0, 12, 12);
        Rect inner = new Rect(5, 5, 5, 4);

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", outer.w*SCALE_FACTOR, outer.h*SCALE_FACTOR));

        List<Rect> subrects = outer.biSectionSplit(true, 1);
        for (Rect s: subrects)
            bw.write(s.toSVG(SCALE_FACTOR, 1, "black"));

        subrects = inner.biSectionSplit(true, 1);
        for (Rect s: subrects)
            bw.write(s.toSVG(SCALE_FACTOR, 1, "black"));

        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }

    static void testSpiralSplit() throws IOException {
        String outFile = "spiraltest.htm";
        FileWriter fw = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fw);

        int SCALE_FACTOR = 50;
        Rect outer = new Rect(0, 0, 11, 13);
        Rect inner = new Rect(5, 3, 5, 5);
        RectQuadruple rq = new RectQuadruple(outer, inner);

        if (!rq.isValid())
            System.err.println("Invalid");

        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<svg width=\"%d\" height=\"%d\">\n", outer.w*SCALE_FACTOR, outer.h*SCALE_FACTOR));

        bw.write(outer.toSVG(SCALE_FACTOR, 1, "black"));
        bw.write(inner.toSVG(SCALE_FACTOR, 1, "black"));
        bw.write(rq.t.toSVG(SCALE_FACTOR, 3, "red"));
        bw.write(rq.l.toSVG(SCALE_FACTOR, 3, "blue"));
        bw.write(rq.b.toSVG(SCALE_FACTOR, 3, "green"));
        bw.write(rq.r.toSVG(SCALE_FACTOR, 3, "yellow"));

        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }

    public static void main(String[] args) {
        try {
            testBisectionSplit();
            testSpiralSplit();
        }
        catch (IOException ex) { ex.printStackTrace(); }
    }
}


