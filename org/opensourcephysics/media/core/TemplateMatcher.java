/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.awt.*;
import java.awt.image.*;

import org.opensourcephysics.display.*;
import org.opensourcephysics.tools.DatasetCurveFitter;
import org.opensourcephysics.tools.UserFunction;

/**
 * A class to find the best match of a template image in a target image.
 * The match location is estimated to sub-pixel accuracy by assuming the 
 * distribution of match scores near a peak is Gaussian. 
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class TemplateMatcher {
	
  // instance fields
	private BufferedImage input, template;
	private Shape mask;
  private int[] templatePixels, templateR, templateG, templateB;
  private boolean[] isPixelTransparent;
  private int[] targetPixels;
  private int wTemplate, hTemplate; // width and height of the template image
  private int wTarget, hTarget; // width and height of the target image
  private int wTest, hTest; // width and height of the tested image
  private TPoint p = new TPoint(); // for general use in methods
  private double largeNumber = 1.0E20; // bigger than any expected difference
  private DatasetCurveFitter fitter; // used for Gaussian fit
  private Dataset dataset; // used for Gaussian fit
  private UserFunction f; // used for Gaussian fit
  private double[] pixelOffsets = {-1, 0, 1}; // used for Gaussian fit
  private double[] xValues = new double[3]; // used for Gaussian fit
  private double[] yValues = new double[3]; // used for Gaussian fit
  private double peakHeight, peakWidth; // peak height and width of most recent match
  private int trimLeft, trimTop;

  /**
   * Constructs a TemplateMatcher object. If a mask shape is specified, then
   * only pixels that are entirely inside the mask are included in the template.
   * 
   * @param image the image to match
   * @param maskShape a shape to define inside pixels (may be null)
   */
  public TemplateMatcher(BufferedImage image, Shape maskShape) {
  	input = image;
  	mask = maskShape;
    getTemplate();
    // set up the Gaussian curve fitter
		dataset = new Dataset();
		fitter = new DatasetCurveFitter(dataset);
    fitter.setAutofit(true);
    f = new UserFunction("gaussian"); //$NON-NLS-1$
    f.setParameters(new String[] {"a", "b", "c"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		new double[] {1, 0, 1});
    f.setExpression("a*exp(-(x-b)^2/c)", new String[] {"x"}); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Gets the template. Includes only pixels inside the mask, if any.
   *
   * @return the template
   */
  public BufferedImage getTemplate() {
  	if (template == null) {
  		// create ARGB template image
      wTemplate = input.getWidth();
      hTemplate = input.getHeight();
	    template = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
	    template.createGraphics().drawImage(input, 0, 0, null);
	    templatePixels = new int[wTemplate * hTemplate];
	    template.getRaster().getDataElements(0, 0, wTemplate, hTemplate, templatePixels);
	  	if (mask != null) {
		    // pixels outside mask should be transparent
	    	for (int i = 0; i < templatePixels.length; i++) {
	      	boolean inside = true;
	    		// pixel is inside only if all corners are inside
	    		int x = i%wTemplate, y = i/wTemplate;
	    		for (int j = 0; j < 2; j++) {
	    			for (int k = 0; k < 2; k++) {
	    	    	p.setLocation(x+j, y+k);
	    	    	inside = inside && mask.contains(p);
	    			}
	    		}
	      	if (!inside)
	      		templatePixels[i] = templatePixels[i] & (0 << 24); // set alpha to zero (transparent)
	    	}
		    template.getRaster().setDataElements(0, 0, wTemplate, hTemplate, templatePixels);
	    }
	  	// trim transparent edges from template
	  	int trimRight=0, trimBottom=0;
	  	// left edge
	  	boolean transparentEdge = true;
	  	while (transparentEdge && trimLeft < wTemplate) {
	  		for (int line = 0; line < hTemplate; line++) {
	  			int i = line*wTemplate+trimLeft;
	  			transparentEdge = transparentEdge && ((templatePixels[i] >> 24) & 0xff) == 0;	
	  		}
	  		if (transparentEdge) trimLeft++;
	  	}
	  	// right edge
	  	transparentEdge = true;
	  	while (transparentEdge && (trimLeft+trimRight) < wTemplate) {
	  		for (int line = 0; line < hTemplate; line++) {
	  			int i = (line+1)*wTemplate-1-trimRight;
	  			transparentEdge = transparentEdge && ((templatePixels[i] >> 24) & 0xff) == 0;	
	  		}
	  		if (transparentEdge) trimRight++;
	  	}
	  	// top edge
	  	transparentEdge = true;
	  	while (transparentEdge && trimTop < hTemplate) {
	  		for (int col = 0; col < wTemplate; col++) {
	  			int i = trimTop*wTemplate+col;
	  			transparentEdge = transparentEdge && ((templatePixels[i] >> 24) & 0xff) == 0;	
	  		}
	  		if (transparentEdge) trimTop++;
	  	}
	  	// bottom edge
	  	transparentEdge = true;
	  	while (transparentEdge && (trimTop+trimBottom) < hTemplate) {
	  		for (int col = 0; col < wTemplate; col++) {
	  			int i = (hTemplate-1-trimBottom)*wTemplate+col;
	  			transparentEdge = transparentEdge && ((templatePixels[i] >> 24) & 0xff) == 0;	
	  		}
	  		if (transparentEdge) trimBottom++;
	  	}
	  	if (trimLeft+trimRight+trimTop+trimBottom > 0) {
	  		wTemplate -= (trimLeft+trimRight);
	  		hTemplate -= (trimTop+trimBottom);
		    BufferedImage bi = new BufferedImage(wTemplate, hTemplate, BufferedImage.TYPE_INT_ARGB);
		    bi.createGraphics().drawImage(template, -trimLeft, -trimTop, null);
	  		template = bi;
		    templatePixels = new int[wTemplate * hTemplate];
		    template.getRaster().getDataElements(0, 0, wTemplate, hTemplate, templatePixels);
	  	}
	    // use templateRGB and templateTransparent arrays for faster matching
	    templateR = new int[wTemplate * hTemplate];
	    templateG = new int[wTemplate * hTemplate];
	    templateB = new int[wTemplate * hTemplate];
	    isPixelTransparent = new boolean[wTemplate * hTemplate];
	    for (int i = 0; i < templatePixels.length; i++) {
	      int pixel = templatePixels[i];
	      templateR[i] = (pixel >> 16) & 0xff;		// red
	      templateG[i] = (pixel >>  8) & 0xff;		// green
	      templateB[i] = (pixel      ) & 0xff;		// blue
	      isPixelTransparent[i] = ((pixel >> 24) & 0xff)==0;		// alpha
	    }
  	}
  	return template;
  }
  	
  /**
   * Gets the template location at which the best match occurs. May return null.
   *
   * @param target the image to search
   * @param searchRect the rectangle to search within the target image
   * @return the optimized template location at which the best match, if any, is found
   */
  public TPoint getMatchLocation(BufferedImage target, Rectangle searchRect) {
    wTarget = target.getWidth();
    hTarget = target.getHeight();
    // determine insets needed to accommodate template
    int left = wTemplate/2, right = left;
    if (wTemplate%2>0) right++;
    int top = hTemplate/2, bottom = top;
    if (hTemplate%2>0) bottom++;
    // trim search rectangle if necessary
  	searchRect.x = Math.max(left, Math.min(wTarget-right, searchRect.x));
  	searchRect.y = Math.max(top, Math.min(hTarget-bottom, searchRect.y));
  	searchRect.width = Math.min(wTarget-searchRect.x-right, searchRect.width);
  	searchRect.height = Math.min(hTarget-searchRect.y-bottom, searchRect.height);
  	if (searchRect.width <= 0 || searchRect.height <= 0) {
  		peakHeight = Double.NaN;
  		peakWidth = Double.NaN;
  		return null;
  	}
  	// set up test pixels to search (rectangle plus template)
  	int xMin = Math.max(0, searchRect.x-left);
  	int xMax = Math.min(wTarget, searchRect.x+searchRect.width+right);
  	int yMin = Math.max(0, searchRect.y-top);
  	int yMax = Math.min(hTarget, searchRect.y+searchRect.height+bottom);
  	wTest = xMax-xMin;
  	hTest = yMax-yMin;
    if (target.getType() != BufferedImage.TYPE_INT_RGB) {
    	BufferedImage image = new BufferedImage(wTarget, hTarget, BufferedImage.TYPE_INT_RGB);
      image.createGraphics().drawImage(target, 0, 0, null);
      target = image;
    }
    targetPixels = new int[wTest * hTest];
    target.getRaster().getDataElements(xMin, yMin, wTest, hTest, targetPixels);
    // find the rectangle point with the minimum difference
    double matchDiff = largeNumber; // larger than typical differences
    int xMatch=0, yMatch=0;
    double avgDiff = 0;
  	for (int x = 0; x <= searchRect.width; x++) {
  		for (int y = 0; y <= searchRect.height; y++) {
    		double diff = getDifferenceAtTestPoint(x, y);
    		avgDiff += diff;
    		if (diff < matchDiff) {
    			matchDiff = diff;
    			xMatch = x;
    			yMatch = y;
    		}
    	}
    }
  	avgDiff /= (searchRect.width*searchRect.height);
		peakHeight = avgDiff/matchDiff-1;
		peakWidth = Double.NaN;
		double dx = 0, dy = 0;
		// if match is not exact, fit a Gaussian and find peak
		if (!Double.isInfinite(peakHeight)) {
			double w = 40/peakHeight;
			double rmsDev = 1;
			
			// fill data arrays
	  	xValues[1] = yValues[1] = peakHeight;
  		for (int i = -1; i < 2; i++) {
  			if (i == 0) continue;
				double diff = getDifferenceAtTestPoint(xMatch+i, yMatch); 
  			xValues[i+1] = avgDiff/diff-1; 
				diff = getDifferenceAtTestPoint(xMatch, yMatch+i); 
  			yValues[i+1] = avgDiff/diff-1; 
  		}
  		
  		// set approximate dx and dy values
  		double pull = 1/(xValues[1]-xValues[0]);
  		double push = 1/(xValues[1]-xValues[2]);
  		if (Double.isNaN(pull)) pull=1.0E10;
  		if (Double.isNaN(push)) push=1.0E10;
  		dx = 0.6*(push-pull)/(push+pull);
  		pull = 1/(yValues[1]-yValues[0]);
  		push = 1/(yValues[1]-yValues[2]);
  		if (Double.isNaN(pull)) pull=1.0E10;
  		if (Double.isNaN(push)) push=1.0E10;
  		dy = 0.6*(push-pull)/(push+pull);

  		// set x parameters and fit to x data
  		dataset.clear();
  		dataset.append(pixelOffsets, xValues);
  		for (int k = 0; k < 6; k++) {
	  		f.setParameterValue(0, peakHeight);
	  		f.setParameterValue(1, dx);
	  		f.setParameterValue(2, w*Math.pow(3, k));
    		rmsDev = fitter.fit(f);
	      if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
	      	
	      	dx = f.getParameterValue(1);
	    		peakWidth = f.getParameterValue(2);
	    		break;
	      }
  		}
      
  		if (!Double.isNaN(peakWidth)) {
	      // set y parameters and fit to y data
	  		dataset.clear();
	  		dataset.append(pixelOffsets, yValues);
    		for (int k = 0; k < 6; k++) {
  	  		f.setParameterValue(0, peakHeight);
  	  		f.setParameterValue(1, dy);
  	  		f.setParameterValue(2, w*Math.pow(3, k));
      		rmsDev = fitter.fit(f);
		      if (rmsDev < 0.01) { // fitter succeeded (3-point fit should be exact)
		      	dy = f.getParameterValue(1);
		      	peakWidth = (peakWidth+f.getParameterValue(2))/2;
		    		break;
		      }
    		}	    		
    		if (rmsDev > 0.01)
    			peakWidth = Double.NaN;
  		}
		}
		double xImage = xMatch+searchRect.x-left-trimLeft+dx;
		double yImage = yMatch+searchRect.y-top-trimTop+dy;
		return new TPoint(xImage, yImage);
  }
  
  /**
   * Returns the width and height of the peak for the most recent match.
   * The peak height is the ratio meanSqPixelDiff/matchSqPixelDiff.
   * The peak width is the mean of the vertical and horizontal Gaussian fit widths. 
   * This data can be used to determine whether a match is acceptable.
   * A peak height greater than 5 is a reasonable standard for acceptability.
   *  
   * Special cases:
   * 1. If the match is perfect, then the height is infinite and the width NaN.
   * 2. If the searchRect fell outside the target image, then no match was
   *    possible and the height is NaN. 
   * 3. If the Gaussian fit optimization was not successful (either horizontally 
   *    or vertically) then the width is NaN.
   *
   * @return double[2] {mean Gaussian width, height}
   */
  public double[] getMatchWidthAndHeight() {
  	return new double[] {peakWidth, peakHeight};
  }

//_____________________________ private methods _______________________

  /**
   * Gets the total difference between the template and test pixels 
   * at a specified test point. The test point is the point on the test
   * image where the top left corner of the template is located.
   * 
   * @param x the test point x-component
   * @param y the test point y-component
   */
  private double getDifferenceAtTestPoint(int x, int y) {
  	// for each pixel in template, get difference from corresponding test pixel
  	// return sum of these differences
    double diff = 0;
    for (int i = 0; i < wTemplate; i++) {
    	for (int j = 0; j < hTemplate; j++) {
    		int templateIndex = j*wTemplate+i;
    		int testIndex = (y+j)*wTest+x+i;
    		if (testIndex < 0 || testIndex >= targetPixels.length)
    			return Double.NaN; // may occur when doing Gaussian fit
      	if (!isPixelTransparent[templateIndex]) { // include only non-transparent pixels
      		int pixel = targetPixels[testIndex];
      		diff += getRGBDifference(pixel, templateR[templateIndex], templateG[templateIndex], templateB[templateIndex]);
      	}
    	}
    }
    return diff;
  }

  /**
   * Gets the difference between a pixel and a comparison set of rgb components.
   */
  private double getRGBDifference(int pixel, int r, int g, int b) {
    int rPix = (pixel >> 16) & 0xff;		// red
    int gPix = (pixel >>  8) & 0xff;		// green
    int bPix = (pixel      ) & 0xff;		// blue
    int dr = r-rPix;
	  int dg = g-gPix;
	  int db = b-bPix;
	  return dr*dr + dg*dg + db*db; // sum of squares of rgb differences
  }

}
