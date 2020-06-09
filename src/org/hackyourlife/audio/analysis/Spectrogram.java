package org.hackyourlife.audio.analysis;

import java.util.Arrays;

import org.hackyourlife.audio.dsp.FFT;

public class Spectrogram {
	public static float[][][] spectrogram(float[][] samples, int fftSize, int stepSize, int windowType,
			int windowSize) {
		int nch = samples.length;
		int samplecnt = samples[0].length;
		int fftcount = samplecnt / stepSize;
		if((samplecnt % stepSize) != 0) {
			fftcount++;
		}
		float[][][] result = new float[nch][fftcount][fftSize / 2];

		double[] window = FFT.makeWindow(windowType, fftSize, windowSize);

		double[] fft = new double[fftSize];
		double[] phase = new double[fftSize];
		for(int i = 0, n = 0; i < samplecnt; i += stepSize, n++) {
			for(int ch = 0; ch < nch; ch++) {
				fill(fft, samples, ch, i, windowSize);
				Arrays.fill(phase, 0);
				FFT.applyWindow(fft, window);
				FFT.magnitudePhaseFFT(fft, phase);
				finish(result, fft, ch, n);
			}
		}

		return result;
	}

	private static void fill(double[] out, float[][] samples, int ch, int start, int count) {
		for(int i = 0; i < count; i++) {
			int idx = start + i;
			if(idx < samples[ch].length) {
				out[i] = samples[ch][idx];
			} else {
				out[i] = 0;
			}
		}
	}

	private static void finish(float[][][] out, double[] fft, int ch, int n) {
		for(int i = 0; i < fft.length / 2; i++) {
			out[ch][n][i] = (float) fft[i];
		}
	}
}
