/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;

/**
 * An animation performs repetitive calculations in a separate thread.
 * Prior to starting the thread, the control should invoke initializeAnimation().
 * The thread is started when the control invokes startAnimation().
 *
 * @author Joshua Gould
 * @author Wolfgang Christian
 * @version 1.0
 */
public interface Animation {
  /**
   * Sets the object that controls this animation.
   * @param control
   */
  public void setControl(Control control);

  /**
   * Starts the animation thread.
   */
  public void startAnimation();


  public void showMP();

  /**
   * Stops the animation thread.
   */
  public void stopAnimation();

  /**
   * Initializes the animation.
   */
  public void initializeAnimation();

  /**
   * Resets the animation to a known initial state.
   */
  public void resetAnimation();

  /**
   * Performs a single animation step.
   */
  public void stepAnimation();

}

