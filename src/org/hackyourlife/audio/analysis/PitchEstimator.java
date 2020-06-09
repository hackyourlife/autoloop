package org.hackyourlife.audio.analysis;

import java.util.Arrays;

import org.hackyourlife.audio.SampleConverter;
import org.hackyourlife.audio.dsp.FFT;

public class PitchEstimator {
	public static double getHz(int n, int fftSize, int sampleRate) {
		return ((double) n / fftSize) * sampleRate;
	}

	public static int getBin(double f, int fftSize, int sampleRate) {
		double result = f / sampleRate * fftSize;
		return (int) Math.round(result);
	}

	public static double estimate(float[][] samples, int sampleRate) {
		double[][] tmp = new double[samples.length][samples[0].length];
		for(int ch = 0; ch < tmp.length; ch++) {
			for(int i = 0; i < tmp[ch].length; i++) {
				tmp[ch][i] = samples[ch][i];
			}
		}
		return estimate(tmp, sampleRate);
	}

	public static double estimate(double[][] samples, int sampleRate) {
		double[] mono = SampleConverter.mono(samples);
		if(mono.length < 128) {
			throw new IllegalArgumentException("not enough samples");
		}

		int fftSize = 131072;

		while(fftSize > mono.length) {
			fftSize /= 2;
		}

		double[] data = new double[fftSize];
		double[] window = FFT.makeWindow(FFT.HANN, fftSize, fftSize);
		double[] phase = new double[fftSize];

		int startsample = (mono.length - fftSize) / 2;
		if(startsample + fftSize > mono.length) {
			startsample = 0;
		}

		// prepare buffers
		Arrays.fill(phase, 0);
		System.arraycopy(mono, startsample, data, 0, fftSize);

		// perform FFT
		FFT.applyWindow(data, window);
		FFT.magnitudePhaseFFT(data, phase);

		// find fundamental
		// step 1: get max intensity
		double maxintensity = data[0];
		int maxindex = 0;
		for(int i = 0; i < data.length / 2; i++) {
			if(data[i] > maxintensity) {
				maxintensity = data[i];
				maxindex = i;
			}
		}

		// find first peak with at least max/10 intensity
		double threshold = maxintensity / 10;
		for(int i = getBin(Frequency.MIDInoteToFreq(0), fftSize, sampleRate); i < data.length / 2; i++) {
			if(data[i] >= threshold) {
				// found it, now continue as long as intensity increases
				for(i++; i < data.length / 2; i++) {
					if(data[i] < data[i - 1]) {
						// intensity decreases from here on -> return as result
						return getHz(i - 1, fftSize, sampleRate);
					}
				}
			}
		}

		return getHz(maxindex, fftSize, sampleRate);
	}
}
