package com.intel.labs.istcpc.swl;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import org.opencv.core.Rect;

import processing.core.PApplet;
import processing.core.PImage;

public class ItemMissingElement extends TextElement implements BenchActionListener {
	private String[] itemNames;
	
	int count = -8;
	int negincr = -3;
	int posincr = 1;
	int lim = 0;
	int neglim = -8;
	
	int estLim = 1; // Only match the most likely object estimate. increase to go "deeper" and use the 2nd or 3rd guess as a match
	
	boolean complete = false;
	
	static PImage checked, unchecked, badchecked;
	
	public ItemMissingElement(Scenario scenario) {
		super(scenario);
	}

	public void setItems(String items) {
		itemNames = items.split(",");
	}
	
	public void draw(PApplet canvas, Rect rRect) {
		if (checked == null) {
			loadImages(canvas);
		}

		PImage img;
		int myColor;
		if (complete) {
			img = checked;
			myColor = 0xFF00B200;
		} else {
			img = unchecked;
			myColor = 0xFFB20000;	
		}

		drawProportionalImage(canvas,rRect,x,y,(int)(textSize*1.1) /* just looks right */,img);
		drawProportionalText(canvas,rRect,x + (int)(textSize/6),y,textSize,myColor,text);
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
		checked = canvas.loadImage(Scenario.scenarioPath + File.separator + "img" + File.separator + "checked.png");
		unchecked = canvas.loadImage(Scenario.scenarioPath + File.separator + "img" + File.separator + "unchecked.png");
		badchecked = canvas.loadImage(Scenario.scenarioPath + File.separator + "img" + File.separator + "badchecked.png");		
	}


	
	public void checkForAction(List<BenchObject> objects,  List<Rectangle> gloveRects) {
		if (complete) {
			return; // we're done. No need to check anymore
		}
		if (gloveRects.size() > 0) {
			// Gloves can obscure. So let's ignore frames with gloves in them
			return;
		}
		
		boolean seen = false;
		for (int i=0; i<objects.size(); i++) {
			BenchObject bo = objects.get(i);
			for (int estIdx=0; estIdx < Math.min(bo.thingEstimates.size(), estLim); estIdx++) {
				Match m = bo.thingEstimates.get(estIdx);
				for (int j=0; j<itemNames.length; j++) {
					if ((m.thing.name.equalsIgnoreCase(itemNames[j])) && (m.confidence > BenchActivity.PLAUS_CONFIDENCE)) {
						seen = true;
					}
				}
			}
		}
		if (seen) {
			count += negincr;
		} else {
			count += posincr;
		}
			
		if (count < neglim) {
			count = neglim;
		}
		if (count >= lim) {
			count = lim;
			// Trigger the event.
			complete = true;
			if ((timer != null) && (autoTrigger == false)) {
				timer.trigger();
			}
			parent.checkForDoneness();
		}
	}

	public void setmatchlim(String lim) {
		if (lim == null) {
			return;
		}
		estLim = Integer.parseInt(lim);
		
	}	
}
