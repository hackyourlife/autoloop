package org.hackyourlife.audio;

public class Normalizer {
	public static float[][] normalize(float[][] samples) {
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		float[][] normalized = new float[samples.length][samples[0].length];

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				if(samples[ch][i] > max) {
					max = samples[ch][i];
				}
				if(samples[ch][i] < min) {
					min = samples[ch][i];
				}
			}
		}

		float scale = Math.max(Math.abs(min), Math.abs(max));

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				normalized[ch][i] = samples[ch][i] / scale;
			}
		}

		return normalized;
	}

	public static float[][] normalize(int[][] samples) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		float[][] normalized = new float[samples.length][samples[0].length];

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				if(samples[ch][i] > max) {
					max = samples[ch][i];
				}
				if(samples[ch][i] < min) {
					min = samples[ch][i];
				}
			}
		}

		int scale = Math.max(Math.abs(min), Math.abs(max));

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				normalized[ch][i] = (float) ((double) samples[ch][i] / (double) scale);
			}
		}

		return normalized;
	}

	public static double[][] normalizeF64(int[][] samples) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		double[][] normalized = new double[samples.length][samples[0].length];

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				if(samples[ch][i] > max) {
					max = samples[ch][i];
				}
				if(samples[ch][i] < min) {
					min = samples[ch][i];
				}
			}
		}

		int scale = Math.max(Math.abs(min), Math.abs(max));

		for(int ch = 0; ch < normalized.length; ch++) {
			for(int i = 0; i < normalized[ch].length; i++) {
				normalized[ch][i] = (double) samples[ch][i] / (double) scale;
			}
		}

		return normalized;
	}
}
