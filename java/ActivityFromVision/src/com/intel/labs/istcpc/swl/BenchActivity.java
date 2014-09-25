package com.intel.labs.istcpc.swl;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import processing.core.PApplet;
import processing.core.PImage;

/*
 * This class captures all the behaviors and assumptions of our vision-based recognition of activities performed
 * On a surface at a bench
 * 
 *  Assumptions:
 *     - The bench is a uniform color
 *     - The center of the camera falls on the bench (that is to say, the center pixels can be assumed to be benchtop.)
 *     - On startup the bench is clear of objects
 *     
 */
public class BenchActivity {

	//*** Image stuff
	int backgroundLum = -1;
	Mat hlsBackground;
	PImage background;
	public final static int CLOSE_ENOUGH = 20;  // 20 pixels is our "close" to the screen edge
	public final static int MIN_OVERLAP = 1000;  // Enough overlap for glove and wrist to be connected
	public final static double PLAUS_CONFIDENCE = 0.8;  // was .92
	public final static double HIGH_CONFIDENCE = 0.94;
	
	//public final static double MIN_CONFIDENCE = 0.9;
	public final static double CONF_DELTA = 0.03;  // Enough of a drop to separate clusters
	public Mat diff;
	
	public ArrayList<ActivityTimer> timers;
	public ArrayList<String> flags;
	
	//*** Stuff pertaining to the activity's state
	List<BenchObject> objects,lastGen; // objects we think are on the bench
	
	List <BenchActionListener> bals;
	
	public void addActionListener(BenchActionListener l) {
		bals.add(l);
	}
	
	public void clearActionListeners() {
		bals.clear();
	}
	
	public BenchActivity() {
		objects = new ArrayList<BenchObject>();
		lastGen = new ArrayList<BenchObject>();
		bals = new ArrayList<BenchActionListener>();
		timers = new ArrayList<ActivityTimer>();
		flags = new ArrayList<String>();
	}
	
	public Mat getHlsBackground() {
		return hlsBackground;
	}
	public PImage getBackground() {
		return background;
	}
	
	public void setBackground(PImage back, Mat cMat) {
		try {
			background = (PImage)back.clone();
			diff=Mat.zeros(cMat.rows(),cMat.cols(),CvType.CV_8SC3);  // make a nice empty mat for us to reuse alot to
		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}
		hlsBackground = cMat;
		
		// Figure out what the luminosity of the background is (to see if we're on a black surface). Average the hue of the 4 center pixels
		double lsum = 0.0;
		for (int i=0; i<1; i++) { 
			for (int j=0; j<1; j++) {
				lsum += cMat.get(cMat.width()/2, cMat.width()/2)[1]/4.0;
			}
		}
		backgroundLum = (int)lsum;
	}
	
	
	public void updateBenchState(Mat hlsMat, Mat maskMat, List<Rectangle> gloveRects, List<Rectangle> objectRects) {
		lastGen = objects;
		objects = new ArrayList<BenchObject>();
		
		// First: figure out if there is an object that is both overlapping the glove AND touching the edge of the screen.
		// That would be *people* (wrist, arm, etc). Set that rect to be ignored
		for (Iterator<Rectangle> it = objectRects.iterator(); it.hasNext();) {
			Rectangle r = it.next();
			boolean touchingHand = false;
			for (Iterator<Rectangle> it2 = gloveRects.iterator(); it2.hasNext();) {
				Rectangle r2 = it2.next();
				Rectangle overlap = r2.intersection(r);
				if (overlap.width*overlap.height > MIN_OVERLAP) {
					touchingHand = true;
				}
			}
			if (touchingHand && bordersEdge(r)) {
				// Ha!  This is a wrist. Ignore...
			} else {
				BenchObject bo = new BenchObject(hlsMat,maskMat,r, touchingHand,lastGen);
				objects.add(bo);
			}
		}	
		
		for (int i=0; i<bals.size(); i++) {
			bals.get(i).checkForAction(objects,gloveRects);
		}
	}

	private boolean bordersEdge(Rectangle r) {
		return isClose(r.x,0) || isClose(r.x+r.width,background.width) ||
				isClose(r.y,0) || isClose(r.y+r.height,background.height);
	}
	
	
	public boolean isClose(Point p1, Point p2) {
		return isClose(p1.x,p2.x) && isClose(p1.y,p2.y);
	}

	private boolean isClose(int x,int y) {
		return Math.abs(x-y) < CLOSE_ENOUGH;
	}

	// let's us draw pretty shapes on the screen
	public void outlineObjects(PApplet canvas, Rectangle rRect, double ratio) {
		canvas.noFill();
		
		for (Iterator<BenchObject> it = objects.iterator(); it.hasNext();) {
			BenchObject bo = it.next();
			Rectangle r = bo.getLocation();
			canvas.noFill();
			int sw = (int)(2*ratio);
			if (sw < 1) {
				sw = 1;
			}
			canvas.strokeWeight(sw);
			if (bo.touchingHand) {
				canvas.stroke(255, 255, 0);	
				canvas.strokeWeight(0);
			} else if (bo.thingEstimates.size() == 0) {
				canvas.stroke(200, 200, 200);			
				canvas.strokeWeight((float)0.1);
			} else {
				canvas.stroke(255, 0, 0);			
			}
			
			canvas.rect(rRect.x+(int)(r.x*ratio), rRect.y+(int)(r.y*ratio), +(int)(r.width*ratio), +(int)(r.height*ratio)); 
			if (bo.thingEstimates.size() == 0) {
				continue;
			}
			canvas.fill(255, 0, 0);
			int i=0;
			for (i=0; i<Math.min(1,bo.thingEstimates.size());i++) {
				double conf = bo.thingEstimates.get(i).confidence;
				if (conf >  HIGH_CONFIDENCE) {
					String s = "'" + bo.thingEstimates.get(i).thing.name + "' (" + String.format("%02d", (int)(100*conf)) + ")"; 
					if (i != 0) {
						s = " or " + s;
					}
					canvas.textSize((int)(18*ratio));
					canvas.text(s, 
						    rRect.x+(int)(r.x*ratio),
						    rRect.y+(int)((r.y+r.height+5+i*20)*ratio),
						    rRect.x+(int)((r.x+80)*ratio),
							rRect.y+(int)((r.y+r.height+200+i*20)*ratio));
				}
			}
		}
	}

	public ActivityTimer getTimer(String timerSought) {
		for (int i=0; i< timers.size(); i++) {
			if (timers.get(i).name.equalsIgnoreCase(timerSought)) {
				return timers.get(i);
			}
		}
		// Didn't find it
		return null;
	}

	public boolean isFlagSet(String flag) {
		for (int i=0; i< flags.size(); i++) {
			if (flags.get(i).equalsIgnoreCase(flag)) {
				return true;
			}
		}
		return false;
	}
	
	

	

}
