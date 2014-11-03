package com.intel.labs.istcpc.swl;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import processing.core.*;
import processing.event.KeyEvent;
import processing.video.*;
import gab.opencv.*;

public class RTVisionDemo extends PApplet {

	private static final long serialVersionUID = -2458891275239135272L;
	
	public final static int FRAMES_TO_IGNORE = 5;  // Allow the white balancing to do it's thing
	public final static float CONTOUR_MIN_AREA = 400;
	public final static float HAND_MIN_AREA = 3500;
	public final static int PADDING = 10;

	//*** Some useful hues
	public final static int HUE_WORKSPACE_CORNER = 60;
	public final static int HUE_GLOVE = 100;
			
	// Little state machine for app's mode
	public final static int MODE_LOADING_MODELS = 0;
	public final static int MODE_FINDING_BG = 1;
	public final static int MODE_TRACKING_OBJECTS = 2;

	public int mode = MODE_LOADING_MODELS;

	public boolean findWorkspaceCorners = false;
	public int workspaceX1, workspaceY1, workspaceX2, workspaceY2;
	
	public static PApplet papp;
	
	// Variables for either camera or video
	Movie vid;
	boolean frameReady = false;
	Capture cam;
	long frameCnt = 0;
	
	boolean paused = false; // Only applies to replay mode
	int displayMsgFor=0;
	String displayMsg;

	PImage roi = null;
	long startTime = -1;
	
	PImage rgbImage; // The current frame in PImage
	Mat hlsMat;		// The current frame in Mats
	OpenCV rgbCV, roiCV,gloveCV;					// The openCV objects used to make the mats
	
    Mat hueMat;
    Mat lMat;
    Mat sMat;
    
	int displayROI = 0;  // 0 for none, 1 for background, 2 for glove, 3 for objects
    Rectangle rgbRect,roiRect;
    

	private boolean snapshotObject = false;         // Signals the system to save the next object as an image
	Random rand;
	
	BenchActivity  act;

	
	List<Rectangle> roiRects = new ArrayList<Rectangle>();

	boolean live = false;  								// This should be set in a GUI
	boolean replayFromImages = true; 					// This should be set in a GUI
	String replayVideo = "/Users/xkcd/Documents/workspace/Chum/data/example captures/danielVideo.mp4"; 				// This should be set in a GUI
	String replayImagePath = "/Users/xkcd/Documents/workspace/Chum/data/example captures/frames_w_error"; 	// This should be set in a GUI
	long nextImageNumber = 1;
	String knownObjectFolder = "/Users/xkcd/Documents/workspace/Chum/data/objects"; 		// This should be set in a GUI
	String gloveImagePath = "/Users/xkcd/Documents/workspace/Chum/data/ignore.png"; 		// This should be set in a GUI
	
	double[]  ignoreColorHist;
	double    ignoreHue; // if there is 1 color to ignore, what is it?
	
	public void setup() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		papp = this; // So others will be able to get at me and use opencv functions
		rand = new Random();
		rand.setSeed(new Date().getTime());
		
		rgbRect = new Rectangle(-1,-1,-1,-1);
		roiRect = new Rectangle(-1,-1,-1,-1);
	//	size(1,1);		
		loadModels();
		act = new BenchActivity();
		mode = MODE_FINDING_BG;
		workspaceX1=workspaceY1=workspaceX2=workspaceY2=-1;

		// open image stream differently if we're working from live or video
		if (!live) {
			if (!replayFromImages) {
			  vid = new Movie(this, replayVideo);
			  vid.loop();
			  vid.play();
			} else {
				frameRate(10);
			}
		} else {
			  String[] cameras = Capture.list();
			  
			  if (cameras.length == 0) {
			    println("There are no cameras available for capture.");
			    exit();
			  } else {    
				  
				  
			    println("Available cameras:");
			    for (int i = 0; i < cameras.length; i++) {
			      println(cameras[i]);
			    }
			    println("I'm hard-coded to use camera #1.  Edit RTVisionDemo to change that.");
			    
			    
			    // The camera can be initialized directly using an 
			    // element from the array returned by list():
			    cam = new Capture(this, cameras[1]); // was 1.  Might not be camera #1 for you.
			    cam.start();     
			  }      
		}
	}
	
	//********** Useful stuff to override if you want to reuse this class
	
	//***** Called the first time that a frame is read.  Override to change or disable
	public void adjustWindowSize(int width, int height) {
		size(width, height);  // size the drawing window		
	}

	//***** Tells the app where to draw the (marked up) RGB output. Pass in -1 for x
	//      if you won't want RGB to display
	public void setRGBDisplayLocation(int x, int y, int width, int height) {
		rgbRect.x = x;
		rgbRect.y = y;
		rgbRect.width = width;
		rgbRect.height = height;
	}

	public void setROIMaskDisplayLocation(int x, int y, int width, int height) {
		roiRect.x = x;
		roiRect.y = y;
		roiRect.width = width;
		roiRect.height = height;
	}
	
	private void loadModels() {
		Thing.loadThings(knownObjectFolder);
		loadIgnoreimage();		 
	}

	private void loadIgnoreimage() {
		PImage ig = loadImage(this.gloveImagePath);
		OpenCV ocv = new OpenCV(this,ig);
		Mat colorMat = ocv.getColor();
		Imgproc.cvtColor(colorMat,colorMat, Imgproc.COLOR_BGR2HLS_FULL);
		ignoreColorHist = Thing.histogram(colorMat,null);
		// for now keep it simple, assume glove is just 1 color
		ignoreHue = 0.0;
		for (int i=0; i<ignoreColorHist.length; i++) {
			ignoreHue += ignoreColorHist[i]*(i+0.5)*Thing.bucketSize;
		}
		System.out.println("The hue to try to ignore is " + ignoreHue);
		
	}
		
	public void draw0() {
		//background(255, 204, 128);
		
	}
	// Main looping callback. Don't actually have to draw in 'draw'
	public void draw() {
		draw0();
		switch (mode) {
		case MODE_LOADING_MODELS:
			return;  // not ready yet
		case MODE_FINDING_BG:
			learnBackground();
			break;
		case MODE_TRACKING_OBJECTS:
			trackObjects();
			break;
		}		
	}
		

	private boolean getNextImage() {
		if (live) { // Read from the camera and grab the next frame
		  if (cam.available() == true) {
			    cam.read();
			  } else {
				  return false;
			  }
			rgbImage = cam.get(); //(0,0,640,480);			
		} else { // read from the video
			if (replayFromImages) {
				String s = String.format("%05d", nextImageNumber);
				if (!paused) {
					nextImageNumber += 1;
				}
				rgbImage = this.loadImage(replayImagePath + File.separator + s + ".png"); 
			} else {
				// From a video that's already playing
				if (!frameReady) {
					return false;
				}
				vid.read();
				rgbImage = vid;
			}
		}
		// image loaded up.  Now create an hslMat because we'll need that
		if (rgbCV == null) {
			rgbCV = new OpenCV(this,rgbImage);
			roiCV = new OpenCV(this,rgbImage);
			gloveCV = new OpenCV(this,rgbImage);
		} else {
			rgbCV.loadImage(rgbImage);
		}
		Mat colorMat = rgbCV.getColor();
		hlsMat = new Mat(colorMat.rows(), colorMat.cols(), CvType.CV_8SC3);
		Imgproc.cvtColor(colorMat,hlsMat, Imgproc.COLOR_BGR2HLS_FULL);
		frameCnt++;
		return true;
	}
	
	public void movieEvent(Movie m) {
		frameReady = true;
	}
	
	public void setupSizes() {
		adjustWindowSize(rgbImage.width, rgbImage.height);  // size the drawing window
		setROIMaskDisplayLocation(0,0,rgbImage.width/2, rgbImage.height/2);			
		setRGBDisplayLocation(rgbImage.width/2,0,rgbImage.width/2, rgbImage.height/2);	
		displayROI = 3;
	}

	private void learnBackground() {
		if (!getNextImage()) {
			return;
		}
		
		//*** Initialize some things on frame #1 
		if (startTime == -1) {
			startTime = System.currentTimeMillis();
			setupSizes();
			hueMat = new Mat(rgbImage.height, rgbImage.width, CvType.CV_8SC1);
		    lMat = new Mat(rgbImage.height, rgbImage.width, CvType.CV_8SC1);
		    sMat = new Mat(rgbImage.height, rgbImage.width, CvType.CV_8SC1);
		}
		
		//*** Capture the background over the first few frames 
		if (frameCnt > FRAMES_TO_IGNORE) {
			act.setBackground(rgbImage,hlsMat);
			findAndSetWorkspaceCorners();
			mode = MODE_TRACKING_OBJECTS;
		}
		// Some drawing
		if (displayROI != 0) {
			image(rgbImage, rgbRect.x, rgbRect.y, rgbRect.width,rgbRect.height);
		}

	}
	
	private void findAndSetWorkspaceCorners() {
		if (!findWorkspaceCorners) {
			return;
		}
		// convert the background mat into HSV
		Mat colorMat = new OpenCV(this,rgbImage).getColor();
		Mat hsv_mat = new Mat(colorMat.rows(), colorMat.cols(), CvType.CV_8SC3);
		Imgproc.cvtColor(colorMat,hsv_mat, Imgproc.COLOR_BGR2HLS_FULL);
		Mat t_mat = new Mat(colorMat.rows(), colorMat.cols(), CvType.CV_8UC1);
		Core.inRange(hsv_mat, new Scalar(20,20,20),new Scalar(80,255,255), t_mat);
		for (int i=0;i<100;i++) {
			for (int j=0;j<100;j++) {
				println(i,j,hsv_mat.get(j, i)[0],hsv_mat.get(j, i)[1],hsv_mat.get(j, i)[2]);
			}
		}
		System.exit(1);
		
	}

	private boolean trackObjects() {
		if (!getNextImage()) {
			return false;
		}
		
		if (displayMsgFor > 0) {
			// Draw any status strings
			long color = 0x22000000;
			stroke(color);			
			strokeWeight(3);
			fill(color);
			textSize(30);
			text(displayMsg,width/100,height-height/25,width/4,height);
			displayMsgFor--;
		}
		
		if (displayROI != 0) {
			image(rgbImage, rgbRect.x, rgbRect.y, rgbRect.width,rgbRect.height);
		}


		//*** Steady state: Find the regions with objects in them
		
		// STEP 1: find regions different than the background. If you're like the original background, we don't care about you
		Core.absdiff(act.getHlsBackground(),this.hlsMat, act.diff);
		// With black background, we can't use 'H' channel meaningfully
	    Vector<Mat> p = new Vector<Mat>();  
	    Core.split(act.diff, p);
	    
		/*****  Object segmentation
		 *   So we can't use HUE. It's all over the place due to the black.
		 *   So we're using some combination of lightness and saturation to
		 *   pick out the interesting objects and hands
		 */
	    
		Core.multiply(p.get(2), new Scalar(3.0), p.get(2)); // H=0 L=1 S=2
		//Core.add(p.get(1), p.get(2), p.get(2));
	    Core.min(p.get(1), p.get(2), p.get(2));
		roiCV.setGray(p.get(2));
		
		roiCV.dilate();
		roiCV.dilate();
		roiCV.erode();
		roiCV.threshold(30); // This could be made dynamic
		
		// STEP 2:  find any areas with the color of the glove in them.  We don't care about gloves either
	    Vector<Mat> planes = new Vector<Mat>();  
	    Core.split(hlsMat, planes);
	    // HUE
	    Core.absdiff(planes.get(0), new Scalar(ignoreHue), hueMat); 
	    Imgproc.threshold(hueMat, hueMat, 5,255.0,Imgproc.THRESH_BINARY); // Hue must be close to (within 5 of) ignoreHue
	    // Lightness
	    Core.absdiff(planes.get(1), new Scalar(128.0), lMat);  
	    Imgproc.threshold(lMat, lMat, 118,255.0,Imgproc.THRESH_BINARY); // lightness must be in the middle 50%
	    // Saturation
	    Core.absdiff(planes.get(2), new Scalar(0), sMat);  
	    Imgproc.threshold(sMat, sMat, 55,255.0,Imgproc.THRESH_BINARY_INV); // Make any pixel with saturation > 1/4 be white (good)
	    
	    Core.max(hueMat, sMat, hueMat);
	    Core.max(hueMat, lMat, hueMat); 
	    // Huemat is now a very nice mask for taking out the glove
	    Core.absdiff(hueMat, new Scalar(255.0),sMat); // we need the inversion of the glove mask to find the outline of the glove
		gloveCV.setColor(sMat);
		gloveCV.erode();
		gloveCV.erode();
		gloveCV.dilate();
		gloveCV.dilate();
		
		PImage fooImg = gloveCV.getOutput();
	    
		// STEP 3:  Remove the gloves from our idea of what's interesting	
		Core.min(roiCV.getGray(),hueMat,roiCV.getGray());  // blast areas that match the glove

		roi = roiCV.getOutput();

		Mat maskMat = roiCV.getGray().clone();
		
		List<Rectangle> rects = new ArrayList<Rectangle>();


		// find the outline of the gloves
		
		rects.clear();
		for (Contour contour : gloveCV.findContours()) {
			float area = contour.area();
			if (area > HAND_MIN_AREA) {
				Rectangle r = contour.getBoundingBox();
				rects.add(r);
			}
		}

		List<Rectangle> gloveRects = new ArrayList<Rectangle>();
		
		for (Iterator<Rectangle> it = rects.iterator(); it.hasNext();) {
			Rectangle r1 = it.next();
			boolean keep = true;
			for (Iterator<Rectangle> it2 = rects.iterator(); it2.hasNext();) {
				Rectangle r2 = it2.next();
				if ((r2 != r1) && r2.contains(r1)) {
					keep = false;
					break;
				}
			}
			if (keep) {
				// add a bit of padding
				r1.x -= Math.min(r1.x,PADDING);
				r1.y -= Math.min(r1.y,PADDING);
				r1.width += Math.min(rgbImage.width-r1.width-r1.x-1,PADDING*2);
				r1.height += Math.min(rgbImage.height-r1.height-r1.y-1,PADDING*2);
				gloveRects.add(r1);
			}
		}

		// Find the objects
		rects.clear();
		for (Contour contour : roiCV.findContours()) {
			float area = contour.area();
			if (area > CONTOUR_MIN_AREA) {
				Rectangle r = contour.getBoundingBox();
				rects.add(r);
			}
		}
		
		roiRects.clear();
		for (Iterator<Rectangle> it = rects.iterator(); it.hasNext();) {
			Rectangle r1 = it.next();
			boolean keep = true;
			for (Iterator<Rectangle> it2 = rects.iterator(); it2.hasNext();) {
				Rectangle r2 = it2.next();
				if ((r2 != r1) && r2.contains(r1)) {
					keep = false;
					break;
				}
			}
			if (keep) {
				// add a bit of padding
				r1.x -= Math.min(r1.x,PADDING);
				r1.y -= Math.min(r1.y,PADDING);
				r1.width += Math.min(rgbImage.width-r1.width-r1.x-1,PADDING*2);
				r1.height += Math.min(rgbImage.height-r1.height-r1.y-1,PADDING*2);
				roiRects.add(r1);
			}
		}

		// Some drawing
		if (displayROI != 0) {
			switch (displayROI) {
			case 1: 
				break;
			case 2:
				image(fooImg, roiRect.x, roiRect.y, roiRect.width,roiRect.height);
				break;
			case 3:
				image(roi, roiRect.x, roiRect.y, roiRect.width,roiRect.height);
				break;
			}
		}
		
		//***  Draw green rects in the color images around gloves
		double ratio = ((double)rgbRect.width)/rgbImage.width;
		int sw = (int)(2*ratio);
		if (sw < 1) {
			sw = 1;
		}
		noFill();
		stroke(0, 255, 0);
		strokeWeight(sw);
		for (Iterator<Rectangle> it = gloveRects.iterator(); it.hasNext();) {
			Rectangle r = it.next();
			if (displayROI != 0) {
				this.rect(rgbRect.x+(int)(r.x*ratio), rgbRect.y+(int)(r.y*ratio), +(int)(r.width*ratio), +(int)(r.height*ratio));
			}
		}
		//*** Clip and draw the same regions
		for (Iterator<Rectangle> it = roiRects.iterator(); it.hasNext();) {
			Rectangle r = it.next();
			PImage subI = rgbImage.get(r.x,r.y,r.width,r.height);			
			// If we've asked the system to snapshot an object, do it now
			if (snapshotObject) {
				PImage spi = new PImage(subI.width*2,subI.height);
				spi.set(0, 0, subI);
				spi.set(subI.width,0,roi.get(r.x,r.y,r.width,r.height));
				String spath = knownObjectFolder + File.separator + "unknown" + File.separator + rand.nextInt(1000000)+".png";
				println("SAVED TO: " + spath);
				spi.save(spath);
				snapshotObject = false;
			}
		}
		act.updateBenchState(hlsMat,maskMat, gloveRects,roiRects);
		if (displayROI != 0) {
			act.outlineObjects(this,rgbRect, ratio);
		}
		return true;
	}
	
	public void keyTyped(KeyEvent e) {
		if (e.getKey() == 's') {  // Snapshot object. Save a picture of the next object found
			println("SNAPSHOT");
			snapshotObject = true;
		} else if (e.getKey() == '`') {  // Toggle Pause (only does things for replay mode)
			paused = !paused;
			if (paused) {
			   displayMsg = "Paused";
			} else {
			   displayMsg = "Unpaused";		
			}
			displayMsgFor = 15;
		} else if (e.getKey() == '1') {  // Toggle displaying ROI view
			displayROI = (displayROI + 1) % 4;
		}
	}
	
	// Some useful static methods
	static int[] hsl_to_bgr(int h, int s, int l) {
	    //
	    double fh = 6 * h / 256.0;
	    double fs = s / 255.0;
	    double fl = l / 255.0;
	    double chroma = (1.0 - Math.abs(2.0 * fl - 1.0)) * fs;
	    double x = chroma * (1.0 - Math.abs((fh % 2.0) - 1.0));
	    double fb = 0.0, fg = 0.0, fr = 0.0;
	    switch((int) Math.floor(fh)) {
	    case 0: fr = chroma; fg = x; break;
	    case 1: fr = x; fg = chroma; break;
	    case 2: fg = chroma; fb = x; break;
	    case 3: fg = x; fb = chroma; break;
	    case 4: fr = x; fb = chroma; break;
	    case 5: fr = chroma; fb = x; break;
	    }
	    fl -= chroma / 2.0;
	    fb += fl;
	    fg += fl;
	    fr += fl;
	    int rv[] = new int[3];
	    rv[0] = (int)(fb * 255.0);
	    rv[1] = (int)(fg * 255.0);
	    rv[2] = (int)(fr * 255.0);
	    return rv;
	}	

	static int rc(double f)
	{
	    // Round but don't go over 255.
	    //
	    if (f >= 255.0) return 255;
	    return (int)(f + 0.5);
	}

	static int bgr_to_h(int b, int g, int r) {
	    double maxbgr = max(max(b, g), r);
	    double minbgr = min(min(b, g), r);
	    double chroma = maxbgr - minbgr;
	    double hue;
	    if (chroma == 0.0) hue = 0.0;
	    else if (maxbgr == b) hue = 4.0 + (r - g) / chroma;
	    else if (maxbgr == g) hue = 2.0 + (b - r) / chroma;
	    else if (g < b) hue = 6.0 + (g - b) / chroma;
	    else hue = (g - b) / chroma;
	    hue = hue * 256.0 / 6.0;
	    return rc(hue);
	}		
}
