package com.intel.labs.istcpc.swl;

	import java.awt.Rectangle;
	import java.util.List;

	import org.opencv.core.Rect;

	import processing.core.PApplet;

	public class SwipeGloveElement extends TextElement implements BenchActionListener {
		int count=-5;
		int negincr=-1;
		int posincr=1;
		int lim=0;
		int neglim=-5;
		int none_seen = 0;

		boolean complete = false;
		boolean activelyLooking = false;
		
		public SwipeGloveElement(int _x, int _y, Scenario _parent) {
			super(_parent);
			x = _x;
			y = _y;
			parent = _parent;
			textSize = 30;
			color = 0xFFFFFF00;
			
			text = ">>> Swipe over workspace to continue...";
		}

		public void draw(PApplet canvas, Rect rRect) {
			if (activelyLooking)  {
				drawProportionalText(canvas,rRect,x,y,textSize,color,text);
			}
		}

		public void checkForAction(List<BenchObject> objects,  List<Rectangle> gloveRects) {
	//		System.out.println("Checking " + activelyLooking + " " + complete);
			if (!activelyLooking || complete) {
				return;
			}
			if (gloveRects.size() == 0) {
				none_seen++;
			}
			// First we have to have seen NO gloves. (give them time to move gloves off of the screen
			if (none_seen < 10) {
				return;
			}
			// Ok, now look for the right number of glove frames
			if (gloveRects.size() > 0) {
				count += posincr;
				System.out.println("Whoo!!!!" + count);
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
				parent.checkForDoneness();
			}
		}	
	}

