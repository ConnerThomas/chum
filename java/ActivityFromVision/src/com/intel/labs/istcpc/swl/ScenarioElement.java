package com.intel.labs.istcpc.swl;

import org.opencv.core.Rect;

import processing.core.PApplet;

public class ScenarioElement  {
	int x, y;
	ActivityTimer timer = null;
	boolean autoTrigger = false;
	
	public void draw(PApplet canvas, Rect rRect) {
		// Do nothing by default
	}
}
