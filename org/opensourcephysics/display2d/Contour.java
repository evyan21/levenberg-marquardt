/*
 * The org.opensourcephysics.display package contains components for rendering
 * two-dimensional scalar and vector fields.
 * Copyright (c) 2003  H. Gould, J. Tobochnik, and W. Christian.
 */
package org.opensourcephysics.display2d;

import  java.awt.*;
import  javax.swing.*;
import  org.opensourcephysics.display.*;

/**
 * Contour draws a scalar field as a contour plot.
 *
 * Contour uses code from the Surface Plotter package by Yanto Suryono.
 *
 * @author       Wolfgang Christian
 * @version 1.0
 */
public class Contour implements Measurable {
    private Color lineColor=new Color(0,64,0);      // dark green
    private Data2D data2d;
    private boolean visible = true;
    private int contour_lines = 12;                 // number of contour lines
    private boolean showContourLines = true;
    private boolean showColoredLevels = true;    // fill with colors
    private double contour_stepz;   // contour spacing
    private int[] xpoints = new int[8];
    private int[] ypoints = new int[8];
    private int[] contour_x = new int[8];
    private int[] contour_y = new int[8];
    private double[] delta = new double[4];
    private double[] intersection = new double[4];
    private double[][] contour_vertex = new double[4][3];
    private ContourAccumulator accumulator = new ContourAccumulator();
    private double zmin = 0, zmax = 1.0;        // the range for contour levels
    private boolean autoscaleZ=true;
    private ColorMapper colorMap= new ColorMapper(contour_lines,zmin,zmax,ColorMapper.SPECTRUM);
    private Color[]         contourColors= new Color[contour_lines + 2];

    /**
     * Constructs a Contour that renders the given dataset2d.
     *
     * @param  _dataset2d the data
     */
    public Contour (Data2D _data2d) {
      data2d=_data2d;
    }


    /**
     * Sets the visible flag.
     * Drawing will be disabled if visible is false.
     *
     * @param isVisible
     */
    public void setVisible(boolean isVisible){
      visible=isVisible;
    }


    /**
     * Paint the contour.
     * @param g
     */
    public void draw(DrawingPanel panel, Graphics g){
      double[][][] data=data2d.getData();
      if(!visible) return;
      if(data==null){
        g.setColor(Color.lightGray);
        g.fillRect(0, 0, panel.getBounds().width, panel.getBounds().height);
        g.setColor(Color.black);
        g.drawString("No data.",0,panel.getBounds().height/2);
        return;
      }
      accumulator.clearAccumulator();
      int calc_divisionsRow=data.length-1;
      int calc_divisionsCol=data[0].length-1;
      contour_stepz = (zmax - zmin)/(contour_lines + 1);
      double z=zmin;
      for(int c=0; c<contourColors.length; c++){
        contourColors[c]=colorMap.doubleToColor(z);
        z+=contour_stepz;
      }
      for (int i = 0; i < calc_divisionsRow; i ++) {
        double[][] col=data[i];
        double[][] next_col=data[i+1];
        for (int j = 0; j < calc_divisionsCol; j ++) {
          if(col[j][0]==col[j+1][0]){
            contour_vertex[0] = col[j];       // the point
            contour_vertex[1] = col[j+1];     // the point in the next col
            contour_vertex[2] = next_col[j+1];// the point in the next row in the next col
            contour_vertex[3] = next_col[j];  // the point in the next col
          }else{
            contour_vertex[0] = col[j];       // the point
            contour_vertex[3] = col[j+1];     // the point in the next col
            contour_vertex[2] = next_col[j+1];// the point in the next row in the next col
            contour_vertex[1] = next_col[j];  // the point in the next col
          }

          createContour(panel,g);
        }
      }
      g.setColor(lineColor);
      accumulator.drawAll(g);
    }

    /**
     * Sets the autoscale flag and the floor and ceiling values.
     *
     * If autoscaling is true, then the min and max values of z are set using the data.
     * If autoscaling is false, then floor and ceiling values become the max and min.
     * Values below min map to the first color; values above max map to the last color.
     *
     * @param isAutoscale
     * @param floor
     * @param ceil
     */
   public void setAutoscaleZ(boolean isAutoscale, double floor, double ceil) {
     autoscaleZ=isAutoscale;
     if(autoscaleZ){
       update();
     }else{
       zmax=ceil;
       zmin=floor;
       colorMap.setScale(zmin,zmax);
     }
    }

    /**
     * Updates the contour using the data array.
     */
    public void update () {
      if(autoscaleZ){
        double[] minmax=data2d.getZRange();
        zmax=minmax[1];
        zmin=minmax[0];
        colorMap.setScale(zmin,zmax);
      }
    }

    /**
     * Creates contour plot of a single area division. Called by
     * <code>draw</code> method
     *
     * @see #draw
     */
    private final void createContour (DrawingPanel panel, Graphics g) {
        double z = zmin;

        xpoints[0] = panel.xToPix(contour_vertex[0][0]);
        xpoints[2] = panel.xToPix(contour_vertex[1][0]);
        xpoints[4] = panel.xToPix(contour_vertex[2][0]);
        xpoints[6] = panel.xToPix(contour_vertex[3][0]);
        xpoints[1] = xpoints[3] = xpoints[5] = xpoints[7] = -1;

        ypoints[0] = panel.yToPix(contour_vertex[0][1]);
        ypoints[4] = panel.yToPix(contour_vertex[2][1]);
        ypoints[2] = ypoints[3] = panel.yToPix(contour_vertex[1][1]);
        ypoints[6] = ypoints[7] = panel.yToPix(contour_vertex[3][1]);

        int xmin = xpoints[0];
        int xmax = xpoints[4];
        for (int counter = 0; counter <= contour_lines + 1; counter++) {
            // Analyzes edges
            for (int edge = 0; edge < 4; edge++) {
                int index = (edge << 1) + 1;
                int nextedge = (edge + 1) & 3;
                if (z > contour_vertex[edge][2]) {
                    xpoints[index - 1] = -2;
                    if (z > contour_vertex[nextedge][2]) {
                        xpoints[(index + 1) & 7] = -2;
                        xpoints[index] = -2;
                    }
                }
                else if (z > contour_vertex[nextedge][2])
                    xpoints[(index + 1) & 7] = -2;
                if (xpoints[index] != -2) {
                    if (xpoints[index] != -1) {
                        intersection[edge] += delta[edge];
                        if ((index == 1) || (index == 5))
                            ypoints[index] = panel.yToPix(intersection[edge]);
                        else
                            xpoints[index] = panel.xToPix(intersection[edge]);
                    }
                    else {
                        if ((z > contour_vertex[edge][2]) || (z > contour_vertex[nextedge][2])) {
                            switch (index) {
                                case 1:
                                    delta[edge] = (contour_vertex[nextedge][1]
                                            - contour_vertex[edge][1])*contour_stepz/(
                                            contour_vertex[nextedge][2] - contour_vertex[edge][2]);
                                    intersection[edge] = (contour_vertex[nextedge][1]*(
                                            z - contour_vertex[edge][2]) + contour_vertex[edge][1]*(
                                            contour_vertex[nextedge][2] - z))/(
                                            contour_vertex[nextedge][2] - contour_vertex[edge][2]);
                                    xpoints[index] = xmin;
                                    ypoints[index] = panel.yToPix(intersection[edge]);
                                    break;
                                case 3:
                                    delta[edge] = (contour_vertex[nextedge][0]
                                            - contour_vertex[edge][0])*contour_stepz/(
                                            contour_vertex[nextedge][2] - contour_vertex[edge][2]);
                                    intersection[edge] = (contour_vertex[nextedge][0]*(
                                            z - contour_vertex[edge][2]) + contour_vertex[edge][0]*(
                                            contour_vertex[nextedge][2] - z))/(
                                            contour_vertex[nextedge][2] - contour_vertex[edge][2]);
                                    xpoints[index] = panel.xToPix(intersection[edge]);
                                    break;
                                case 5:
                                    delta[edge] = (contour_vertex[edge][1] -
                                            contour_vertex[nextedge][1])*contour_stepz/(
                                            contour_vertex[edge][2] - contour_vertex[nextedge][2]);
                                    intersection[edge] = (contour_vertex[edge][1]*(
                                            z - contour_vertex[nextedge][2])
                                            + contour_vertex[nextedge][1]*(contour_vertex[edge][2]
                                            - z))/(contour_vertex[edge][2] -
                                            contour_vertex[nextedge][2]);
                                    xpoints[index] = xmax;
                                    ypoints[index] = panel.yToPix(intersection[edge]);
                                    break;
                                case 7:
                                    delta[edge] = (contour_vertex[edge][0] -
                                            contour_vertex[nextedge][0])*contour_stepz/(
                                            contour_vertex[edge][2] - contour_vertex[nextedge][2]);
                                    intersection[edge] = (contour_vertex[edge][0]*(
                                            z - contour_vertex[nextedge][2])
                                            + contour_vertex[nextedge][0]*(contour_vertex[edge][2]
                                            - z))/(contour_vertex[edge][2] -
                                            contour_vertex[nextedge][2]);
                                    xpoints[index] = panel.xToPix(intersection[edge]);
                                    break;
                            }
                        }
                    }
                }
            }
            // Creates polygon
            int contour_n = 0;
            for (int index = 0; index < 8; index++) {
                if (xpoints[index] >= 0) {
                    contour_x[contour_n] = xpoints[index];
                    contour_y[contour_n] = ypoints[index];
                    contour_n++;
                }
            }
            if (showColoredLevels) {
                g.setColor(contourColors[counter]);
                g.fillPolygon(contour_x, contour_y, contour_n);
            }
            // Creates contour lines
            if (showContourLines) {
                int x = -1;
                int y = -1;
                for (int index = 1; index < 8; index += 2) {
                    if (xpoints[index] >= 0) {
                        if (x != -1)
                            accumulator.addLine(x, y, xpoints[index], ypoints[index]);
                        x = xpoints[index];
                        y = ypoints[index];
                    }
                }
                if ((xpoints[1] > 0) && (x != -1))
                    accumulator.addLine(x, y, xpoints[1], ypoints[1]);
            }
            if (contour_n < 3)
                break;
            z += contour_stepz;
        }
    }

    /**
     * Determines the palette type that will be used.
     * @param type
     */
    public void setColorPalette (Color[] colors) {
      colorMap.setColorPalette(colors);
    }

    /**
     * Sets the type of palette.
     *
     * Palette types are defined in the ColorMapper class and include:  SPECTRUM, GRAYSCALE, and DUALSHADE.
     *
     * @param mode
     */
    public void setPaletteType (int mode) {
      colorMap.setPaletteType(mode);
    }

    /**
     * Sets the floor, ceiling, and line colors.
     *
     * @param floorColor
     * @param ceilColor
     */
    public void setFloorCeilColor (Color floorColor, Color ceilColor){
      colorMap.setFloorCeilColor(floorColor, ceilColor);
    }

    /**
     *   Sets the number of contour levels.
     *
     * @param n number of levels.
     */
    public void setNumberOfLevels (int n) {           // Create colors array
       contour_lines=n;
       colorMap= new ColorMapper(contour_lines,zmin,zmax,colorMap.getPaletteType());
       contourColors= new Color[contour_lines + 2];
    }


    /* The following methods are requried for the measurable interface */
    public double getXMin(){return data2d.getLeft();}
    public double getXMax(){return data2d.getRight();}
    public double getYMin(){return data2d.getBottom();}
    public double getYMax(){return data2d.getTop();}
    public boolean isMeasured(){  //true if the measure is valid
      if(data2d==null) return false;
      return true;
    };
}



