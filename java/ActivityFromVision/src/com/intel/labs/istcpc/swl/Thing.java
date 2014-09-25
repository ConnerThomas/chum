package com.intel.labs.istcpc.swl;

import gab.opencv.OpenCV;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import processing.core.PImage;

/*
 * I was going to call this an object as in "object recognition", but of course that's a terribly overloaded term.  
 * This class is intended to encapsulate the functionality we're going to use to recognize objects from vision.
 */
public class Thing {
	
	public static double MATCH_THRESHOLD = .8; // Needs to be at least this high to be the same object
	public static int bucketSize = 8; // Lets make this a power of 2, ok?
	public static Hashtable<String,Thing> things = new Hashtable<String,Thing>();
	
	public double[] colorHist;
	String name;
	String viewSpecificQuality; // Something extra about this view of the object. Is it on it's side? crushed? etc...
		
	
	public Thing(String name, String viewSpecificQuality, String imagePath) {
		this.name = name;
		System.out.println(imagePath);
		this.viewSpecificQuality = viewSpecificQuality;
		initializeHistogram(imagePath);
		// put this in the canonical list of objects
		things.put(imagePath, this);
	}
	
	
	// MAT is already assumed to be in hls
	public static List<Match> bestMatch(Mat hlsMat,Mat maskMat) {
		// Create a histogram for the object
		double[] imgHist = histogram(hlsMat,maskMat);
		Hashtable<String,Match> matches = new Hashtable<String,Match>();
		for (Enumeration<Thing> e = things.elements(); e.hasMoreElements();) {
			Thing t = e.nextElement();
			double matchConf = t.matchTo(imgHist);
			if (matchConf >= MATCH_THRESHOLD) {
				Match m = matches.get(t.name);
				if ((m == null) || (m.confidence < matchConf)) { // best match so far for this thing
					matches.put(t.name, new Match(t,matchConf));
				}
			}
		}
		ArrayList<Match> list = new ArrayList<Match>();
		for (Enumeration<String> en = matches.keys(); en.hasMoreElements();) {
			Match m = matches.get(en.nextElement());
			list.add(m);
		}

		return list;
	}

	private void initializeHistogram(String imagePath) {
		// The image is 2 parts, color on the left side and B&W mask on the right
		 OpenCV ocv = new OpenCV(RTVisionDemo.papp,imagePath);		
		Mat imgMat = ocv.getColor().submat(0, ocv.height, 0, ocv.width/2);
		ocv.threshold(30);
		Mat maskMat = ocv.getGray().submat(0, ocv.height, ocv.width/2, ocv.width);
		// The image part needs to be converted into an HSV representation
		Mat hsv_mat = new Mat(imgMat.rows(), imgMat.cols(), CvType.CV_8SC3);
		Imgproc.cvtColor(imgMat,hsv_mat, Imgproc.COLOR_BGR2HLS_FULL);
		colorHist = histogram(hsv_mat,maskMat);
	}
	
	// helper
	public void printHist() {
		for (int i=0; i<colorHist.length;i++) {
			System.out.println(i+ " : " + (int)(colorHist[i]*100));
		}		
	}
	
	public static void saveMat(Mat m, String filename) {
		OpenCV tocv = new OpenCV(RTVisionDemo.papp,m.cols(),m.rows());
		tocv.setColor(m);
		PImage pi = tocv.getOutput();
		pi.save(filename);
	}

	public static double[] histogram(Mat img,Mat mask)
	{
	    Vector<Mat> planes = new Vector<Mat>();  
	    Core.split(img, planes);
	    planes.setSize(1); // Ignore the L and S
	    MatOfInt histSize = new MatOfInt(256);
	    final MatOfFloat histRange = new MatOfFloat(0f, 256f);
	    boolean accumulate = false;
	    Mat b_hist = new  Mat();
	    if (mask == null) {
	    	mask = new Mat();  // no mask, use all pixels for hist
	    }
	    Imgproc.calcHist(planes, new MatOfInt(0),mask, b_hist, histSize, histRange, accumulate);
	   // println(b_hist.cols() + " " + b_hist.rows() + " ");
	    double s = 0.001;
	    double colorHist[] = new double[256/bucketSize];
	    for (int i=0; i<b_hist.rows(); i++) {
	//    	System.out.println(i + "," + i*360/255 + "," + b_hist.get(i, 0)[0]);
    		colorHist[i/bucketSize] += b_hist.get(i, 0)[0];
    		s += b_hist.get(i, 0)[0];
	    }

	    for (int i=0; i<colorHist.length; i++) {
	    	colorHist[i] /= s;
//	    	System.out.println(i + "," + colorHist[i]);	    	
	    }
//	    System.out.println("LEN Is " + planes.size());
	    return colorHist;
	}

	public double matchTo(Thing t) {
		return matchTo(t.colorHist);
	}
	
	public double matchTo(double[] colorHist) {
		double rv=1.0;
//		System.out.println("---- " + name);
		for (int i=0;i<colorHist.length;i++) {
//			System.out.println(i+ " " + (int)(this.colorHist[i]*100) + " <-> " + (int)(colorHist[i]*100));
			rv -= ((kern(this.colorHist,i)-kern(colorHist,i)))*((kern(this.colorHist,i)-kern(colorHist,i)));  // subtract for differences 
		}
//verb		System.out.println(rv + "\n");
		return rv;
	}
	
	// compute a simple kernel .5 weight to the left and right of the index and full value on the index.
	private static double kern(double[] arr, int idx) {
		double rv = arr[idx];
		if (idx > 0) {
			rv += 0.5*arr[idx-1];
		}
		if (idx < arr.length-1) {
			rv += 0.5*arr[idx+1];
		}
		return rv;
	}

	// Load up all the thing images in the passed in folder
	public static void loadThings(String knownObjectFolder) {
		File f = new File(knownObjectFolder);
		if (f.isDirectory()) {
			File[] fa = f.listFiles();
			for (int i=0; i<fa.length; i++) {
				if (fa[i].isDirectory()) {
					File[] fa2 = fa[i].listFiles();
					for (int j=0; j<fa2.length; j++) {
						String fp = fa2[j].getAbsolutePath();
						String fn = fa2[j].getName();
						if (fn.endsWith(".jpg") || fn.endsWith(".png")) {
							String qual = fn.split("\\.")[0];
							new Thing(fa[i].getName(),qual,fp);
							System.out.println("--- Loading: [" + fa[i].getName() + "] " + fp);
						}
					}
				}
			}
		} else {
			System.out.println("Bad knownObjectFolder passed in: " + knownObjectFolder);
			System.exit(1);
		}
	}
	
	

}
