package org.hackyourlife.audio.analysis;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpectraLoop {
	private static class Task {
		private final int start;

		public Task(int start) {
			this.start = start;
		}

		public int getStart() {
			return start;
		}
	}

	private static class ComputeThread extends Thread {
		private final float[][][] samples;
		private final int minLength;
		private final int tailLength;
		private final int maxlen;
		private final Queue<Task> tasks;

		private volatile Loop[] loops;

		public ComputeThread(float[][][] samples, int minLength, int tailLength, int count, Queue<Task> tasks) {
			this.samples = samples;
			this.minLength = minLength;
			this.tailLength = tailLength;
			this.tasks = tasks;
			maxlen = samples[0].length - tailLength;
			loops = new Loop[count];
		}

		@Override
		public void run() {
			Loop[] best = new Loop[loops.length];
			try {
				while(!tasks.isEmpty()) {
					Task task = tasks.remove();
					int start = task.getStart();
					for(int end = start + minLength; end < maxlen; end++) {
						float error = estimate(samples, start, end, tailLength);
						int worst = -1;
						float worsterror = -Float.MAX_VALUE;
						for(int i = 0; i < best.length; i++) {
							if(best[i] == null) {
								best[i] = new Loop(start, end, error);
								break;
							} else if(best[i].error > worsterror) {
								worst = i;
								worsterror = best[i].error;
							}
						}
						if(worst != -1) {
							best[worst] = new Loop(start, end, error);
						}
					}
				}
			} catch(NoSuchElementException e) {
				// tasks.remove() failed, swallow
			}
			this.loops = best;
		}

		public Loop[] getLoops() {
			return loops;
		}
	}

	public static Loop[] loop(float[][] samples, int skip, int step, int minLength, int tailLength, int loopcnt,
			int fftSize, int stepSize, int windowType, int windowSize, int threadcnt)
			throws InterruptedException {
		float[][][] fft = Spectrogram.spectrogram(samples, fftSize, stepSize, windowType, windowSize);
		int maxlen = samples[0].length - tailLength - skip;
		ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
		ComputeThread[] threads = new ComputeThread[threadcnt];

		for(int start = skip; start < maxlen; start += step) {
			tasks.add(new Task(start));
		}

		// spawn threads
		for(int i = 0; i < threadcnt; i++) {
			threads[i] = new ComputeThread(fft, minLength, tailLength, loopcnt, tasks);
			threads[i].start();
		}

		// wait for results
		for(Thread t : threads) {
			t.join();
		}

		// collect results
		Loop[] loops = new Loop[loopcnt * threadcnt];
		int i = 0;
		for(ComputeThread t : threads) {
			Loop[] l = t.getLoops();
			System.arraycopy(l, 0, loops, i, l.length);
			i += l.length;
		}

		// remove null results
		int nonnull = 0;
		for(Loop loop : loops) {
			if(loop != null) {
				nonnull++;
			}
		}

		Loop[] result;
		if(nonnull == 0) {
			return null;
		} else if(nonnull != loops.length) {
			result = new Loop[nonnull];
			int j;
			for(i = 0, j = 0; i < loops.length; i++) {
				if(loops[i] != null) {
					Loop loop = loops[i];
					result[j++] = new Loop(loop.start * stepSize, loop.end * stepSize, loop.error);
				}
			}
		} else {
			result = loops;
			for(i = 0; i < loops.length; i++) {
				Loop loop = loops[i];
				result[i] = new Loop(loop.start * stepSize, loop.end * stepSize, loop.error);
			}
		}

		// sort result according to score
		Arrays.sort(result);

		return result;
	}

	public static float estimate(float[][][] samples, int start, int end, int tailLength) {
		float error = 0;
		int channels = samples.length;
		for(int ch = 0; ch < channels; ch++) {
			for(int i = 0; i < tailLength; i++) {
				for(int f = 0; f < samples[ch][0].length; f++) {
					float a = samples[ch][start + i][f];
					float b = samples[ch][end + i][f];
					float diff = (a - b) * (a - b);
					error += diff;
				}
			}
		}
		if(!Double.isFinite(error)) {
			System.out.println("Error is not finite");
		}
		return error;
	}
}
