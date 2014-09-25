package com.intel.labs.istcpc.swl;


import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Mat;

public class BenchObject {  
	List<Match> thingEstimates;
	boolean touchingHand = false;
	Rectangle loc = new Rectangle();

	List<Rectangle> path = null;
	public final static int MAX_PATH_SIZE = 100;

	public BenchObject(Mat hlsMat, Mat maskMat, Rectangle r, boolean touchingHand, List<BenchObject> lastObjectGen) {

		// let's figure out what this thing might be.
		thingEstimates = Thing.bestMatch(hlsMat.submat(r.y,r.y+r.height,r.x,r.x+r.width), maskMat.submat(r.y,r.y+r.height,r.x,r.x+r.width));
		loc = r;
		this.touchingHand = touchingHand;
		// sort estimates
		Collections.sort(thingEstimates);
		int keepUntil = 999;
		double was = -1.0;
		for (int i=0; i<thingEstimates.size();i++) {
			double conf = thingEstimates.get(i).confidence;
			if ((conf < (was - BenchActivity.CONF_DELTA)) || (conf < BenchActivity.PLAUS_CONFIDENCE)) {
				keepUntil = i;
				break;
			}
			was = conf;
		}
		while (thingEstimates.size() > keepUntil) {
			thingEstimates.remove(keepUntil);
		}
		
		smooth(lastObjectGen);
		if (path == null) {
			path = new ArrayList<Rectangle>();
		}
		path.add(loc);
		if (path.size() > MAX_PATH_SIZE) {
			path.remove(0);
		}
	}

	private void smooth(List<BenchObject> lastObjectGen) {
		for (Iterator<BenchObject> it = lastObjectGen.iterator(); it.hasNext();) {
			BenchObject bo = it.next();
			if (loc.intersects(bo.loc) && similarMatch(bo.thingEstimates)) {
				path = bo.path;
			}
		}
	}

	private boolean similarMatch(List<Match> otherThingEstimates) {
		for (Iterator<Match> it = thingEstimates.iterator(); it.hasNext();) {
			Match bo = it.next();
			for (Iterator<Match> it2 = otherThingEstimates.iterator(); it2.hasNext();) {
				Match bo2 = it2.next();
				if (bo.thing.name.equals(bo2.thing.name) && (Math.abs(bo2.confidence - bo.confidence) < BenchActivity.CONF_DELTA)) {
					return true;  // pretty darned close!
				}
			}		
		}
		return false;
	}

	public Rectangle getLocation() {
		return loc;
	}
	


}
