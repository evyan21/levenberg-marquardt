/*
 * The org.opensourcephysics.display package contains components for rendering
 * two-dimensional scalar and vector fields.
 * Copyright (c) 2003  H. Gould, J. Tobochnik, and W. Christian.
 */
package org.opensourcephysics.display2d;

import org.opensourcephysics.display.*;
import  java.awt.*;
import java.awt.geom.*;

/**
 * Data2D stores numeric data on a scaled 2d grid.
 *
 * Every grid point contains the x and y coordinates and one or more samples.
 * The first sample is usually the magnitude of the quantity of interest.
 *
 * Samples can represent almost anything. For example, we often use color-coded
 * arrows to display vector fields. The arrows's color is the first sample and its vertical and
 * horizonal components are the second and third samples.  This data is stored
 * in an internal array as follows:
 *<br>
 * <pre>
 * <code>data=new double [n][m][5]<\code>
 * <code>vertex=data[n][m]<\code>
 *
 * <code>vertex[0] = x  <\code>
 * <code>vertex[1] = y  <\code>
 * <code>vertex[2] = val_1  <\code>
 * <code>vertex[3] = val_2  <\code>
 * <code>vertex[4] = val_3  <\code>
 * <\pre>
 *
 * @author     Wolfgang Christian
 * @created    Feb 20, 2003
 * @version    1.0
 */

public class Data2D {
  double[][][] data;
  double left, right, bottom, top;
  double dx=0, dy=0;

  public Data2D(int row, int col, int nsamples) {
    if((row < 1) || (col < 1) ) {
      throw new IllegalArgumentException(
          "Number of dataset rows and columns must be positive. Your row=" + row + "  col=" + col);
    }
    if((nsamples < 1) ) {
      throw new IllegalArgumentException(
          "Number of 2d data components must be positive. Your ncomponents=" + nsamples);
    }
    data= new double[row][col][nsamples+2];  // x, y, and components
    setScale(0,col,0,row);
  }

  /**
   * Creates a new Data2D object with the same grid points and the given number of samples.
   *
   * @param nsamples number of samples dataset.
   * @return the newly created  Data2D
   */
  public Data2D createData2d(int nsamples){
    Data2D data2d=new Data2D(data.length, data[0].length, nsamples+2);
    data2d.setScale(left,right,bottom,top);
    return data2d;
  }

  /**
 * Sets th horizontal and vertical scale in world units.
 * @param _left
 * @param _right
 * @param _bottom
 * @param _top
 */
public void setScale(double _left, double _right, double _bottom, double _top){
  left=_left;
  right=_right;
  bottom=_bottom;
  top=_top;
  int row=data.length;
  int col=data[0].length;
  dx=0; // special case if #col==1
  if(col>1)dx=(right-left)/(col);
  dy=0;   // special ase if #row==1
  if(row>1)dy=(bottom-top)/(row);  // note that dy is usualy negative
  double y=top+dy/2;
  for(int i=0;i<row;i++){
    double x=left+dx/2;
    for(int j=0;j<col;j++){
      data[i][j][0]=x;  // x location
      data[i][j][1]=y;  // y location
      x += dx;            // inside col loop is inc x
    }
    y += dy;              // start new row so dec y
  }
}

  /**
   * Returns the minimum and maximum values of the first data component.
   * @return {zmin,zmax}
   */
  public double[] getZRange() {
    double zmin=data[0][0][2];
    double zmax=zmin;
    for (int i = 0; i <data.length; i++)
      for (int j = 0; j < data[0].length; j++) {
    double v=data[i][j][2];
    if(v>zmax) zmax=v;
    if(v<zmin) zmin=v;
      }
      return new double[] {zmin,zmax};
  }

  /**
   * Gets the vertex closest to the specified location
   */
  public double[] getVertex(double x, double y) {
    int nx=(int)Math.floor((x-left)/dx);
    nx=Math.max(0,nx);                 // cannot be less than 0
    nx=Math.min(nx,data[0].length-1);  // cannot be greater than last element
    int ny=(int)Math.floor(-(top-y)/dy);
    ny=Math.max(0,ny);                 // cannot be less than 0
    ny=Math.min(ny,data.length-1);     // cannot be greater than last element
    return data[ny][nx];
  }

/**
 * Estimates the value of the sample at an untabulated point, (x,y).
 *
 * Interpolate uses bilinear interpolation on the grid.  Although the interpolating
 * function is continous across the grid boundaries, the gradient changes discontinuously
 * at the grid square boundaries.
 *
 * @param x  the untabulated x
 * @param y  the untabulated y
 * @param sample the sample to be interpolated
 * @return the interpolated sample
 */
public double interpolate(double x, double y, int sample) {
  int i=(int)((y-data[0][0][1])/dy);
  i=Math.max(0,i);
  i=Math.min(data.length-2,i);
  int j=(int)((x-data[0][0][0])/dx);
  j=Math.max(0,j);
  j=Math.min(data[0].length-2,j);  // the x index cannnot be greater than # cols
  double t=(x-data[i][j][0])/dx;
  double u=(y-data[i][j][1])/dy;
  return (1-t)*(1-u)*data[i][j][sample+2]+
         t*(1-u)*data[i][j+1][sample+2]+
         t*u*data[i+1][j+1][sample+2]+
         (1-t)*u*data[i+1][j][sample+2];
}

/**
 * Estimates all samples at an untabulated point, (x,y).
 *
 * Interpolate uses bilinear interpolation on the grid.  Although the interpolating
 * function is continous across the grid boundaries, the gradient changes discontinuously
 * at the grid square boundaries.
 *
 * @param x  the untabulated x
 * @param y  the untabulated y
 * @param samples the array that will contain the interpolated values
 * @return the interpolated array
 */
public double[] interpolate(double x, double y, double[] samples) {
  int i=(int)((y-data[0][0][1])/dy);
  i=Math.max(0,i);
  i=Math.min(data.length-2,i);
  int j=(int)((x-data[0][0][0])/dx);
  j=Math.max(0,j);
  j=Math.min(data[0].length-2,j);  // the x index cannnot be greater than # cols
  double t=(x-data[i][j][0])/dx;
  double u=(y-data[i][j][1])/dy;
  samples[0]= (1-t)*(1-u)*data[i][j][2]+
             t*(1-u)*data[i][j+1][2]+
             t*u*data[i+1][j+1][2]+
             (1-t)*u*data[i+1][j][2];
  samples[1]= (1-t)*(1-u)*data[i][j][3]+
             t*(1-u)*data[i][j+1][3]+
             t*u*data[i+1][j+1][3]+
             (1-t)*u*data[i+1][j][3];
  samples[2]= (1-t)*(1-u)*data[i][j][4]+
             t*(1-u)*data[i][j+1][4]+
             t*u*data[i+1][j+1][4]+
             (1-t)*u*data[i+1][j][4];
  return samples;
}


  /**
   * Gets the array containing the data.
   *
   * @return the data
   */
  public double[][][] getData(){
    return data;
  }

  /**
   * Gets the x value for the first column in the grid.
   * @return  the leftmost x value
   */
  public final double getLeft(){ return left;}

  /**
   * Gets the x value for the right column in the grid.
   * @return  the rightmost x value
   */
  public final double getRight(){ return right;}

  /**
   * Gets the y value for the first row of the grid.
   * @return  the topmost y value
   */
  public final double getTop(){ return top;}

  /**
   * Gets the y value for the last row of the grid.
   * @return the bottommost y value
   */
  public final double getBottom(){ return bottom;}

/**
 * Gets the change in x between grid columns.
 * @return the bottommost y value
 */
  public final double getDx(){ return dx;}

  /**
   * Gets the change in y between grid rows.
   * @return the bottommost y value
   */
  public final double getDy(){ return dy;}

}