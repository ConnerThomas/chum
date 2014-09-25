package com.intel.labs.istcpc.swl;

import java.awt.Rectangle;
import java.util.List;

public interface BenchActionListener {

	public void checkForAction(List<BenchObject> objects, List<Rectangle> gloveRects); 
}