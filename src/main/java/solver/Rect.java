package solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Rect implements Comparable<Rect> {
    int x, y;  // (row, col)
    int w, h;
    int area;

    static String[] fillStyles = {"gray", "red", "blue", "yellow"};
    static int numColors = fillStyles.length;

    Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        area = w*h;
    }

    boolean isValid() {
        return x>=0 && y>=0 && w>0 && h>0;
    }

    int getArea() { return area; }

    Rect getReflectedHorizontally() {
        Rect reflected = new Rect(x, State.n - (y + w), w, h);
        return reflected;
    }

    Rect getRotated() { // transpose
        Rect rotated = new Rect(y, x, h, w);
        return rotated;
    }

    public int compareTo(Rect r) { return Integer.compare(area, r.area); }

    public boolean congruent(Rect that) {
        return this.w+this.h == that.w+that.h && this.w*this.h == that.w*that.h;
    }

    public List<Rect> biSectionSplit(boolean vertical, int cut) {
        Rect a, b;
        List<Rect> children = new ArrayList<>(2);

        if (vertical && (y+w<=cut || cut<=y))
            return null;
        if (!vertical && (x+h<=cut || cut<=x))
            return null;

        if (vertical) {
            a = new Rect(x, y, cut, h);
            b = new Rect(x, y+cut, y+w-cut, h);
        }
        else {
            a = new Rect(x, y, w, cut);
            b = new Rect(x+cut, y, w, x+h-cut);
        }
        children.add(a);
        children.add(b);

        return children;
    }

    public List<Rect> spiralSplit(int xt, int xb, int yl, int yr) { // pivot is a subrectangle
        Rect pivot = new Rect(xt, yl, yr-yl, xb-xt);
        if (!pivot.isValid())
            return null;

        RectQuadruple quadruple = new RectQuadruple(this, pivot);
        if (!quadruple.isValid())
            return null;

        List<Rect> children = new ArrayList<>(4);
        children.add(quadruple.t);
        children.add(quadruple.l);
        children.add(quadruple.b);
        children.add(quadruple.r);
        children.add(pivot);

        return children;
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("(").append(x).append(", ")
                .append(y).append(") :")
                .append(w).append("x")
                .append(h).append("=")
                .append(area)
        ;
        return buff.toString();
    }

    public String toSVG(int k, int stroke_width, String colorName) {
        String rect_svg = String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"stroke-width:%d;stroke:%s;fill:none\"/>",
                this.y*k, this.x*k, this.w*k, this.h*k, stroke_width, colorName);
        return rect_svg;
    }

    String selectRandomColor() {
        if (numColors<=0) return "none";

        int i = (int)(Math.random()* numColors);
        String tmp = fillStyles[i];
        fillStyles[i] = fillStyles[numColors-1];
        fillStyles[numColors-1] = tmp;
        numColors--;
        return tmp;
    }

    public String toSVGColor(int k, int stroke_width, String colorName) {
        String rect_svg = String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"stroke-width:%d;stroke:%s;fill:%s\"/>",
                this.y*k, this.x*k, this.w*k, this.h*k, stroke_width, colorName, selectRandomColor());
        return rect_svg;
    }

    public String toSVGColor(int k, int stroke_width, String colorName, String fillColor) {
        String rect_svg = String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"stroke-width:%d;stroke:%s;fill:%s\"/>",
                this.y*k, this.x*k, this.w*k, this.h*k, stroke_width, colorName, fillColor);
        return rect_svg;
    }


    public String key() { return w < h ? w + ":" + h: h + ":" + w; }
}


