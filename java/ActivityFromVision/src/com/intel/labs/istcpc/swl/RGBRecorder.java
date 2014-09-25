package com.intel.labs.istcpc.swl;

import java.io.File;

import processing.core.*;
import processing.video.Capture;

public class RGBRecorder extends PApplet {

	private static final long serialVersionUID = -2458891275239135273L;
	PImage rgbImage = null;
	
	Capture cam;
	
	String saveFolder = "c:/tmp/frames"; // assumed to exist
	
	long frame = 0;
	long startTime = -1;
	long yes = 0;
	long no = 0;
	
	boolean first = true;
	

	public void setup() {
		size(1,1);		
		  String[] cameras = Capture.list();
		  
		  if (cameras.length == 0) {
		    println("There are no cameras available for capture.");
		    exit();
		  } else {
			  
		    println("Available cameras:");
		    for (int i = 0; i < cameras.length; i++) {
		      println(cameras[i]);
		    }
		    
		    
		    // The camera can be initialized directly using an 
		    // element from the array returned by list():
		    cam = new Capture(this, cameras[1]);
		    cam.start();   
		  }
		
	}

	public static long getUnsignedInt(int x) {
	    return x & 0x00000000ffffffffL;
	}
	
	// Main looping callback. Don't actually have to draw in 'draw'
	public void draw() {
		if (!getNextImage()) {
			no++;
			return;
		}
		yes++;

		if (startTime == -1) {
			startTime = System.currentTimeMillis();
			size(rgbImage.width, rgbImage.height);  // size the drawing window
		}
		
		set(0, 0, rgbImage);
		
		frame++;
		

		// Save off the "double picture"
		String s = String.format("%05d", frame);
		this.save(saveFolder + File.separator + s + ".png");

		// Draw in the frame # and FPS
		fill(0, 102, 153);
		long now = System.currentTimeMillis()+1;
		double secs = (now - startTime)/1000.0;
		double fps = (frame) / secs;
		stroke(255, 255, 255);
		fill(255, 255, 255);
		text(frame + " " + fps, 50, 50, 250, 400);
		
	}
		
	private boolean getNextImage() {
	  if (cam.available() == true) {
		    cam.read();
		  } else {
			  return false;
		  }
		rgbImage = cam.get(0,0,640,480);			
		return true;
	}
	
	static int rc(float f)
	{
	    // Round but don't go over 255.
	    //
	    if (f >= 255.0) return 255;
	    return (int)(f + 0.5);
	}

/*
	static int[] bgr_to_hsl(int b, int g, int r) {
	    float maxbgr = max(max(b, g), r);
	    float minbgr = min(min(b, g), r);
	    float chroma = maxbgr - minbgr;
	    float hue;
	    if (chroma == 0.0) hue = 0.0;
	    else if (maxbgr == b) hue = 4.0 + (r - g) / chroma;
	    else if (maxbgr == g) hue = 2.0 + (b - r) / chroma;
	    else if (g < b) hue = 6.0 + (g - b) / chroma;
	    else hue = (g - b) / chroma;
	    hue = hue * 256.0 / 6.0;
	    float lightness = (maxbgr + minbgr) / 2.0;
	    float saturation;
	    if (chroma == 0.0) saturation = 0.0;
	    else saturation = chroma / (1.0 - fabsf(2.0 * lightness / 255.0 - 1.0));
	    cv::Vec3b res(rc(hue), rc(saturation), rc(lightness));
	    return res;
	}
*/

	
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


	
		 
}
