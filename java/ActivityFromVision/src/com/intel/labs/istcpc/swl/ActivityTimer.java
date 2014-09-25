package com.intel.labs.istcpc.swl;

import java.util.Date;

public class ActivityTimer {
	public Date startTime,endTime;
	public String name;
	
	public ActivityTimer(String _name) {
		name = _name;
	}

	public long getElapsedSeconds() {
		if ((startTime == null) || (endTime == null)) {
			return -1;
		}
		return (endTime.getTime()-startTime.getTime())/1000;
	}
	
	public void trigger() { // Either start or stop the timer based on its state
		if (startTime == null) {
			startTime = new Date();
		} else if (endTime == null) {
			endTime = new Date();
		}
	}

}
