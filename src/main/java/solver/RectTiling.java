/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solver;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author debforit
 */

public class RectTiling {
    
    TreeSet<Rectangle> tiles;
    int width; // floor width
    int height; // floor height
    Random r ;
    
    static final long SEED = 31416;
    //static final long SEED = 141421;
    //static final long SEED = System.currentTimeMillis();
    
    static int SCALE_FACTOR = 50;
    
    static final List<Color> colors = new ArrayList<>();
    
    static void init() {
        if (colors.isEmpty()) {
            colors.add(new Color(255, 255, 102));
            colors.add(new Color(225, 225, 225));
            colors.add(Color.RED);
            colors.add(new Color(0, 153, 255));
        }
    }
    
    public RectTiling(int width, int height, int numBars) {
        this.width = width;
        this.height = height;
        
        r = new Random(SEED);
        tiles = new TreeSet<>();
        buildTowers(numBars);
    }
    
    // create the vertical tiles first
    void buildTowers(int k) {
        final int min_w = 2;
        final int min_h = 2;
        
        // decide at random which tower's height will be equal to the
        // height of the floor.
        int width_covered = 0;
        int color = 0;
        int numColors = colors.size();
        int avgWidth = width/k;
        
        
        Rectangle prev = null;
        int i = 0;
        
        while (width_covered < width) {
            int w = min_w + r.nextInt(avgWidth); // integer in [1, k]
            int h;
            
            if (w + width_covered >= width) {
                w = width - width_covered;
                h = height;
            }
            else
                h = min_h + r.nextInt(height-min_h+1); // integer in [1, height]
            
            Rectangle tower = new Rectangle(width_covered, 0, w, h, color);
            tower.adjLeft = prev;
            if (prev != null)
                prev.adjRight = tower;
            
            tiles.add(tower);            
            width_covered += w;
            
            prev = tower;
            color = (color + 1)%numColors;
            i++;
        }
    }

    static void swap(List<Rectangle> tiles, int i, int j) {
        Rectangle tmp = tiles.get(i);
        tiles.set(i, tiles.get(j));
        tiles.set(j, tmp);
    }
    
    String tilesToString(TreeSet<Rectangle> tiles) {
        StringBuffer buff = new StringBuffer();
        for (Rectangle r: tiles)
            buff.append(r.rectToString()).append("\n");
        return buff.toString();
    }

    void saveTile() {
        saveTile(-1);
    }
    
    void saveTile(int i) {
        try {
            saveTile(tiles, i);
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
    void saveTile(TreeSet<Rectangle> tiles, int i) throws IOException {
        String fileName = i>=0?
                String.format("mondrian-%d-%d_%d.htm", width, height, i):
                String.format("mondrian-%d-%d_final.htm", width, height);
        
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);
        
        bw.write("<!DOCTYPE html>\n<html>\n<body>\n");
        bw.write(String.format("<div>Mondrian score of %dx%d: %s </div>", width, height, mondrianScore()));
        
        bw.write(tilesToSVG(tiles));
        
        bw.write("</body>\n</html>");
        bw.close();
        fw.close();
    }
    
    int pickColor(Set<Color> seenColors) {
        Color c;
        int ncolors = colors.size();
        
        for (int i=0; i < ncolors; i++) {
            c = colors.get(i);
            if (!seenColors.contains(c))
                return i;
        }

        // create a random color
        c = new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256));
        colors.add(c);
        return colors.size()-1;
    }
    
    Rectangle fill(TreeSet<Rectangle> presentState, Rectangle pivotRect) {
        
        Rectangle pivotFilled = null;
        Set<Color> seenColors = new HashSet<>();
        seenColors.add(pivotRect.getColor());
        if (pivotRect.adjLeft!=null)
            seenColors.add(pivotRect.adjLeft.getColor());
        if (pivotRect.adjRight!=null)
            seenColors.add(pivotRect.adjRight.getColor());
        
        do {
            if (pivotRect.level() >= height)
                break;

            // check which side the water flows
            int leftLevel = pivotRect.adjLeft==null? this.height: pivotRect.adjLeft.level();
            int rightLevel = pivotRect.adjRight==null? this.height: pivotRect.adjRight.level();
            boolean isLeft = leftLevel < rightLevel;

            // continue traversing along either the left or right
            Rectangle x = isLeft? pivotRect.adjLeft : pivotRect.adjRight;
            int fillWidth = pivotRect.width;

            while (x!=null && x.level() <= pivotRect.level()) {
                presentState.remove(x);
                seenColors.add(x.getColor());
                fillWidth += x.width;
                x = isLeft? x.adjLeft: x.adjRight;
            }

            int fillHeight = x==null? height: x.level();
            fillHeight -= pivotRect.level();

            pivotFilled = new Rectangle(pivotRect.x_top, pivotRect.y_top+pivotRect.height, fillWidth, fillHeight, pickColor(seenColors));
            if (isLeft) {
                pivotFilled.adjLeft = x;
                if (x!=null) x.adjRight = pivotFilled;
                pivotFilled.adjRight = pivotRect.adjRight;
            }
            else {
                pivotFilled.adjRight = x;
                pivotFilled.adjLeft = pivotRect.adjLeft;
                if (x!=null) x.adjLeft = pivotFilled;
            }
        }
        while (false);
        
        if (pivotFilled!=null)    
            presentState.add(pivotFilled);
        
        presentState.remove(pivotRect);
        return pivotFilled;
    }
    
    public void fill() {
        
        TreeSet<Rectangle> presentState = new TreeSet<>(tiles);
        
        //int iters = 0;
        //saveTile(iters);            
        
        while (!presentState.isEmpty()) {
            
            Rectangle pivot = presentState.first();
            Rectangle filled = fill(presentState, pivot);
            if (filled != null)
                tiles.add(filled);
            
            //iters++;
            //saveTile(iters);            
            //System.out.println(tilesToString(presentState));
        }
        saveTile();            
    }
    
    String tilesToSVG(TreeSet<Rectangle> tiles) {
        StringBuffer buff = new StringBuffer();
        buff.append(String.format("<svg width=\"%d\" height=\"%d\">\n", width*SCALE_FACTOR, height*SCALE_FACTOR));
        
        for (Rectangle r: tiles)
            buff.append(r.rectToSVG(SCALE_FACTOR)).append("\n");
        
        buff.append("</svg>");
        
        return buff.toString();
    }
    
    public String mondrianScore() {
        int minArea = height*width, maxArea=0;
        Rectangle minRect = tiles.first(), maxRect = tiles.last();
        
        for (Rectangle r: tiles) {
            int area = r.area();
            if (area < minArea) {
                minArea = area;
                minRect = r;
            }
            if (area > maxArea) {
                maxRect = r;
                maxArea = area;
            }
        }
        return String.format("(%dx%d) - (%dx%d) = %d",
                maxRect.width, maxRect.height,
                minRect.width, minRect.height,
                maxArea-minArea);
    }
    
    public static void main(String[] args) {
        try {
            RectTiling.init();
            
            RectTiling mondrian = new RectTiling(9, 9, 5);            
            
            mondrian.fill();
            System.out.println(mondrian.tilesToString(mondrian.tiles));
            System.out.println("Score = " + mondrian.mondrianScore());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
