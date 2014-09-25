package com.intel.labs.istcpc.swl;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import org.opencv.core.Rect;

import processing.core.PApplet;
import processing.core.PImage;

public class WarningElement extends TextElement implements BenchActionListener {
	private String[] itemNames;
	String flagName;
	
	int count;
	int negincr;
	int posincr;
	int lim;
	int neglim;

	boolean complete = false;
	
	static PImage warning;
	
	public WarningElement(Scenario _parent,String _flagName) {
		super(_parent);
		flagName = _flagName;
		count = -10;
		neglim = -10;
		negincr = -1;
		posincr = 1;
		lim = 0;
	}
	
	public void setItems(String items) {
		itemNames = items.split(",");
	}
	
	public void draw(PApplet canvas, Rect rRect) {
		if (warning == null) {
			loadImages(canvas);
		}

		PImage img;
		if (complete) {
			img = warning;
			drawProportionalImage(canvas,rRect,x,y,(int)(textSize*1.1) /* just looks right */,img);
			drawProportionalText(canvas,rRect,x + (int)(textSize/6),y,textSize,color,text);
		}
	}


	protected void drawProportionalImage(PApplet canvas, Rect rRect, int x, int y, int size, PImage img) {
		int xpix = (int)(x*rRect.width/100); // offset from image corner
		int ypix = (int)(y*rRect.height/100);
		int imgWidthPix = (int)(size*rRect.width/1024); // Will scale as well, just not a % of pixels
		int imgHeightPix = (int)(imgWidthPix*img.height/img.width);
		canvas.noFill();
		canvas.image(img,rRect.x+xpix,rRect.y+ypix, imgWidthPix,imgHeightPix);
		
	}


	private void loadImages(PApplet canvas) {
		warning = canvas.loadImage(Scenario.scenarioPath + File.separator + "img" + File.separator + "warning.png");
	}


	
	public void checkForAction(List<BenchObject> objects,  List<Rectangle> gloveRects) {
		if (complete) {
			return; // we're done. No need to check anymore
		}
		boolean seen = false;
		for (int i=0; i<objects.size(); i++) {
			BenchObject bo = objects.get(i);
			if (bo.thingEstimates.size() > 0) {
				Match m = bo.thingEstimates.get(0);
				for (int j=0; j<itemNames.length; j++) {
					if ((m.thing.name.equalsIgnoreCase(itemNames[j])) && (m.confidence > 0.95)) {
						seen = true;
					}
				}
			}
		}
		if (seen) {
			count += posincr;
		} else {
			count += negincr;
		}
			
		if (count < neglim) {
			count = neglim;
		}
		if (count >= lim) {
			count = lim;
			// Trigger the event.
			complete = true;
			// Add the fact that we triggered to the list of flags
			parent.act.flags.add(flagName);
			parent.checkForDoneness();
		}
	}	
}
