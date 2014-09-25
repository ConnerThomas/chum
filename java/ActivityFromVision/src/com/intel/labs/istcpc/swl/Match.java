package com.intel.labs.istcpc.swl;

public class Match implements Comparable<Match> {
	Thing thing;
	double confidence;

	public Match(Thing t, double matchConf) {
		thing = t;
		confidence = matchConf;
	}

	public int compareTo(Match m) {
		return Double.compare(m.confidence,confidence);
	}

}
