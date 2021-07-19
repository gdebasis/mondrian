package solver;

public class Rect implements Comparable<Rect> {
    int x, y;
    int w, h;
    int area;

    Rect(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        area = w*h;
    }

    public int compareTo(Rect r) { return Integer.compare(area, r.area); }

    public boolean congruent(Rect that) {
        return this.w+this.h == that.w+that.h && this.w*this.h == that.w*that.h;
    }

    RectPair split(boolean vertical, int cut) {
        Rect a, b;

        if (w-cut<=0 || h-cut<=0)
            return null;

        if (vertical) {
            a = new Rect(x, y, cut, h);
            b = new Rect(x, y+cut, w-cut, h);
        }
        else {
            a = new Rect(x, y, w, cut);
            b = new Rect(x+cut, y, w, h-cut);
        }

        return new RectPair(a, b);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(x).append(",")
                .append(y).append(":")
                .append(w).append("x")
                .append(h).append("=")
                .append(area)
        ;
        return buff.toString();
    }

    public String toSVG(int k) {
        String rect_svg = String.format(
                "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"%s\"/>",
                this.y*k, this.x*k, this.w*k, this.h*k, "stroke-width:1;stroke:black;fill:none");
        return rect_svg;
    }
}


