package com.intel.labs.istcpc.swl;

import org.opencv.core.Rect;
import processing.core.PApplet;

public class TextElement extends ScenarioElement {
	int textSize;
	int color;
	String flag; // if non-null, this text element only displays if flag is set
	String text;
	Scenario parent;
	
	public TextElement(Scenario _parent) {
		parent = _parent;
	}
	
	public void draw(PApplet canvas, Rect rRect) {
		if ((flag == null) || parent.act.isFlagSet(flag)) {
			drawProportionalText(canvas,rRect,x,y,textSize,color,text);
		}
	}

	protected void drawProportionalText(PApplet canvas, Rect rRect, int x, int y, int fontSize, int color, String text) {
		int xpix = (int)(x*rRect.width/100); // offset from image corner
		int ypix = (int)(y*rRect.height/100);
		int fontSizePix = (int)(fontSize*rRect.width/1024); // Will scale as well, just not a % of pixels
		canvas.stroke(color);			
		canvas.strokeWeight(3);
		canvas.fill(color);
		canvas.textSize(fontSizePix);
		canvas.text(text,rRect.x+xpix,rRect.y+ypix, rRect.x+rRect.width,rRect.y+rRect.height);
	}

	
}
