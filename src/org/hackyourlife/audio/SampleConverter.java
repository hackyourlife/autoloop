package org.hackyourlife.audio;

public class SampleConverter {
	public static int[] mono(int[][] samples) {
		if(samples == null) {
			throw new NullPointerException("NULL not allowed");
		}
		if(samples.length == 0) {
			throw new IllegalArgumentException("not enough channels");
		}

		int nch = samples.length;
		int[] mono = new int[samples[0].length];
		for(int ch = 0; ch < nch; ch++) {
			if(samples[ch].length != mono.length) {
				throw new IllegalArgumentException("sample count mismatch");
			}
			for(int i = 0; i < mono.length; i++) {
				mono[i] += samples[ch][i] / nch;
			}
		}

		return mono;
	}

	public static double[] mono(double[][] samples) {
		if(samples == null) {
			throw new NullPointerException("NULL not allowed");
		}
		if(samples.length == 0) {
			throw new IllegalArgumentException("not enough channels");
		}

		int nch = samples.length;
		double[] mono = new double[samples[0].length];
		for(int ch = 0; ch < nch; ch++) {
			if(samples[ch].length != mono.length) {
				throw new IllegalArgumentException("sample count mismatch");
			}
			for(int i = 0; i < mono.length; i++) {
				mono[i] += samples[ch][i] / nch;
			}
		}

		return mono;
	}
}
