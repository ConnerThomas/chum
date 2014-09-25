package com.intel.labs.istcpc.swl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Rect;

import processing.core.PImage;
import processing.data.XML;
import processing.event.KeyEvent;

	public class Scenario extends RTVisionDemo {
		private static final long serialVersionUID = -2458891275291135272L;

		//private static passed scenario path
		public static String scenarioPath = "c:/tmp/LabScenario";
		
		double ratio = 1.0;
		double vidratio = 1.0;
		int vidWidth = 240;
		
		String title = "<unknown>";
		int color;
		int stepCount = -1;
		
		XML pXml;
		XML currentStep;
		int stepNumber=-1;
		String stepName;
		PImage stepImage;
		Rect imageRect;
		List<ScenarioElement> els;
		
		public void setup() {
			loadScenarioInfo();
			super.setup();
			loadStep(1);
		}
		
		private void loadScenarioInfo() {
			pXml = loadXML(scenarioPath + File.separator + "scenario.xml");
			// Load the title
			title = pXml.getString("name");
			stepCount = pXml.getInt("stepcount");
			color = (int)Long.parseLong(pXml.getString("color"),16);
			els = new ArrayList<ScenarioElement>();
			imageRect = new Rect();
			
			
		}
		
		public void setupSizes() {
//			size(displayWidth, displayHeight);
			size(1024, 768);
			sizeVidWindows();
		}
		
		public void sizeVidWindows() {
			// We want the video windows in the bottom right (if they're showing)
			int vw = (int)(vidWidth*vidratio);
			int vh = (int)(vidWidth*480/640*vidratio);
			imageRect.x = (int)((width-stepImage.width*ratio)/2);
			imageRect.y = (int)((height-stepImage.height*ratio)/2);
			imageRect.width = (int)(stepImage.width*ratio);
			imageRect.height = (int)(stepImage.height*ratio);	
	//		System.out.println(imageRect.x + " " + imageRect.y);
			int x0 = width-vw;
			int y0_1 = height-vh;
			int y0_2 = height-2*vh;
			setROIMaskDisplayLocation(x0,y0_2,vw,vh);			
			setRGBDisplayLocation(x0,y0_1,vw,vh);			
		}

		private void loadStep(int i) {
			els.clear();
			act.clearActionListeners();
			stepNumber = i;
			currentStep = pXml.getChild("step" + i);
			System.out.println(" U see " + pXml);
			stepName = currentStep.getString("name");
			stepImage = this.loadImage(scenarioPath + File.separator + "img" + File.separator + currentStep.getString("img"));
			XML[] xa = currentStep.getChildren();
			for (int j=0; j<xa.length; j++) {
				if (xa[j].toString().trim().length() == 0) {
					continue; // No idea why I have to do this. But it's giving me blank guys
				}
				if (!xa[j].hasAttribute("type")) {
					continue; // skip it
				}
				ScenarioElement se = null;
				String type = xa[j].getString("type");
				if (type.equalsIgnoreCase("text")) {
					TextElement te = new TextElement(this);
					te.x = xa[j].getInt("x");
					te.y = xa[j].getInt("y");
					te.flag = xa[j].getString("flag"); // likely null, but text might be conditioned on a flag
					te.color = (int)Long.parseLong(xa[j].getString("color"),16);
					te.textSize = xa[j].getInt("size");
					te.text = xa[j].getContent();
					// update the text based on the timers that are available
					for (int k=0; k< act.timers.size(); k++) {
						te.text = te.text.replace(act.timers.get(k).name, "" + act.timers.get(k).getElapsedSeconds());
					}
					
					se = te;
				} else if (type.equalsIgnoreCase("in")) {
					ItemTriggerElement te = new ItemTriggerElement(this);
					te.x = xa[j].getInt("x");
					te.y = xa[j].getInt("y");
					te.color = (int)Long.parseLong(xa[j].getString("color"),16);
					te.text = xa[j].getContent();
					te.textSize = xa[j].getInt("size");
					te.setItems(xa[j].getString("item"));
					te.setmatchlim(xa[j].getString("matchlim"));
					se = te;
					act.addActionListener(te);
				} else if (type.equalsIgnoreCase("out")) {
					ItemMissingElement te = new ItemMissingElement(this);
					te.x = xa[j].getInt("x");
					te.y = xa[j].getInt("y");
					te.color = (int)Long.parseLong(xa[j].getString("color"),16);
					te.text = xa[j].getContent();
					te.textSize = xa[j].getInt("size");
					te.setItems(xa[j].getString("item"));
					se = te;
					act.addActionListener(te);
				} else if (type.equalsIgnoreCase("warning")) {
					WarningElement te = new WarningElement(this,xa[j].getString("flag"));
					te.x = xa[j].getInt("x");
					te.y = xa[j].getInt("y");
					te.color = (int)Long.parseLong(xa[j].getString("color"),16);
					te.text = xa[j].getContent();
					te.textSize = xa[j].getInt("size");
					te.setItems(xa[j].getString("item"));
					se = te;
					act.addActionListener(te);
				} else {
					System.out.println("Unknown type of element " + type + " !!! ignoring");
					continue;
				}
				// see if this element contains a timer
				String timerStr = xa[j].getString("timer");
				if (timerStr != null) {
					// Look and see if we have this timer already
					ActivityTimer t = act.getTimer(timerStr);
					if (t == null) {
						// doesn't exist, so create a new one
						t = new ActivityTimer(timerStr);
						act.timers.add(t);
					}
					if (xa[j].getString("autotrigger") != null) {
						t.trigger();
						se.autoTrigger = true;
						
					}
					se.timer = t;
				}
				els.add(se);
			}
			// Now add the 'go to next step' element
			SwipeGloveElement sge = new SwipeGloveElement(15,93,this);
			els.add(sge);
			act.addActionListener(sge);

		}
		
		private void loadAndRenderNextStep() {
			System.out.println("DOING STEP " + (stepNumber+1));
			loadStep(stepNumber+1);
		}

		private void checkNextUndoneThing() {
			// useful for debugging and for when things go wrong
			SwipeGloveElement sge = null;
			for (int i=0; i<els.size(); i++) {
				ScenarioElement se = els.get(i);
				if (se instanceof SwipeGloveElement) {
					sge = (SwipeGloveElement)se;
				} else if (se instanceof ItemTriggerElement) {
					ItemTriggerElement te = (ItemTriggerElement)se;
					if (te.complete == false) {
						// set this complete
						te.complete = true;
						checkForDoneness();
						return;
					}
				} else if (se instanceof ItemMissingElement) {
					ItemMissingElement te = (ItemMissingElement)se;
					if (te.complete == false) {
						// set this complete
						te.complete = true;
						checkForDoneness();
						return;
					}
				} else if (se instanceof WarningElement) {
					WarningElement te = (WarningElement)se;
					if (te.complete == false) {
						// set this complete
						te.complete = true;
						checkForDoneness();
						return;
					}
				}
			}
			
			if (sge != null) {
				sge.activelyLooking = true;
				sge.complete = true;
				checkForDoneness();
				System.out.println("HA! I think I did it.");
				return;
			}						
		}

		public void draw0() {
			background(color);
			sizeVidWindows();
			// Draw the image in the center of the window
			image(stepImage, imageRect.x, imageRect.y, imageRect.width,imageRect.height);
			// Draw the step name
			drawProportionalText(2,2,50,0xFF000000,"Step " + stepNumber + ": " + stepName);
			// Draw all of the step items
			for (int i=0; i<els.size(); i++) {
				els.get(i).draw(this,imageRect);
			}
		//	for (int i=0; i< act.timers.size(); i++) {
		//		System.out.println("--- " + act.timers.get(i).name + " " + act.timers.get(i).getElapsedSeconds());
		//	}
		// (int i=0; i< act.flags.size(); i++) {
		//		System.out.println("---XX " + act.flags.get(i));
		//	}
		}		
		
		// X and y are percentages of the size of the step image
		private void drawProportionalText(int x, int y, int fontSize, int color, String text) {
			int xpix = (int)(x*stepImage.width*ratio/100); // offset from image corner
			int ypix = (int)(y*stepImage.height*ratio/100);
			int fontSizePix = (int)(fontSize*1024/stepImage.width*ratio); // Will scale as well, just not a % of pixels
			stroke(color);			
			strokeWeight(3);
			fill(color);
			textSize(fontSizePix);
			text(text,imageRect.x+xpix,imageRect.y+ypix, imageRect.x+imageRect.width,imageRect.y+imageRect.height);
		}

		public void keyTyped(KeyEvent e) {
			super.keyTyped(e);
			if (e.getKey() == '4') { // decrease image size
				ratio -= .1;
			}
			if (e.getKey() == '5') { // increase image size
				ratio += .1;
			}
			if (e.getKey() == '6') { 
				frameRate((float)(frameRate*0.8));
			}
			if (e.getKey() == '7') { 
				frameRate((float)(frameRate*1.2));
			}
			if (e.getKey() == '2') { // decrease video size
				vidratio -= .1;
			}
			if (e.getKey() == '3') { // increase video size
				vidratio += .1;
			}
			if (e.getKey() == ' ') {
				System.out.println("Pretending the first undone thing was done...");
				checkNextUndoneThing();
			}
		}

		// One of our scenario elements thinks it has made progress. Recheck
		public void checkForDoneness() {
			SwipeGloveElement sge = null;
			boolean allDone = true;
			for (int i=0; i<els.size(); i++) {
				ScenarioElement se = els.get(i);
				if (se instanceof SwipeGloveElement) {
					sge = (SwipeGloveElement)se;
				} else if (se instanceof ItemTriggerElement) {
					ItemTriggerElement te = (ItemTriggerElement)se;
					if (te.complete == false) {
						allDone = false;
					}
				} else if (se instanceof ItemMissingElement) {
					ItemMissingElement te = (ItemMissingElement)se;
					if (te.complete == false) {
						allDone = false;
					}
				}
			}
			
			if (allDone && (sge.activelyLooking == false)) {
				// let's start the "Go onto next page"
				sge.activelyLooking = true;
			} else if (allDone && sge.activelyLooking && sge.complete) {
				// now we're really done. Onto the next step!
				loadAndRenderNextStep();
			}
			
			
		}


	}
		