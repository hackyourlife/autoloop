package org.hackyourlife.audio.analysis;

public class Frequency {
	public static double freqToMIDInote(double freq) {
		// Make the calculation relative to A440 (A4), note number 69.
		return(69.0 + (12.0 * (Math.log(freq / 440.0) / Math.log(2.0))));
	}

	public static double MIDInoteToFreq(double dMIDInote) {
		return(440.0 * Math.pow(2.0, (dMIDInote - 69.0) / 12.0));
	}
}
