/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solver;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author debforit
 */
public class Rectangle implements Comparable<Rectangle> {
    int x_top;
    int y_top;
    int width;
    int height;
    int color;
    Rectangle adjLeft;
    Rectangle adjRight;
    
    public Rectangle(int x_top, int y_top, int width, int height) {
        this(x_top, y_top, width, height, 0);
    }
    
    public Rectangle(int x_top, int y_top, int width, int height, int color) {
        this.x_top = x_top;
        this.y_top = y_top;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public String rectToString() {
        StringBuffer buff = new StringBuffer();
        buff
            .append("(")
            .append(x_top)
            .append(", ")
            .append(y_top)
            .append(") Area=")
            .append(width)
            .append("x")
            .append(height)
        ;
                
        return buff.toString();
    }
    
    int level() {  // water level
        return y_top + height;
    }
    
    public String rectToSVG(int k) {
        String colorStr = String.format("fill:rgb(%d,%d,%d);stroke-width:2;stroke:rgb(0,0,0)",
                RectTiling.colors.get(color).getRed(),
                RectTiling.colors.get(color).getGreen(),
                RectTiling.colors.get(color).getBlue()
        );
        
        String rect_svg = String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" style=\"%s\"/>",
                this.x_top*k, this.y_top*k, this.width*k, this.height*k, colorStr);
        
        String text_svg;
        
        if (height > width)
            text_svg = String.format(
            "<text font-family=\"Verdana\" font-size=\"10\" fill=\"black\""
                    + "transform=\"translate(%d,%d) rotate(270)\">%dx%d=%d</text>",
                    (int)((x_top+width/4.0f)*k), (int)((y_top+height/4.0f)*k), width, height, area());
        else
            text_svg = String.format(
                "<text font-family=\"Verdana\" font-size=\"10\" fill=\"black\""
                    + "transform=\"translate(%d,%d)\">%dx%d=%d</text>",
                    (int)((x_top+width/4.0f)*k), (int)((y_top+height/4.0f)*k), width, height, area());
            
        return rect_svg + "\n" + text_svg;
    }
    
    int area() { return width*height; }
    
    Color getColor() { return RectTiling.colors.get(color); } 

    @Override
    public int compareTo(Rectangle that) {
        int a = this.level();
        int b = that.level();
        if (a == b)
            return Integer.compare(x_top, that.x_top);
        return Integer.compare(a, b);
    }
}
