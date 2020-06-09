package org.hackyourlife.audio.analysis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hackyourlife.audio.Normalizer;
import org.hackyourlife.audio.io.riff.Riff;
import org.hackyourlife.audio.io.riff.RiffWave;

public class Autoloop {
	public static void main(String[] args) throws IOException, InterruptedException {
		if(args.length != 7) {
			System.out.println("Usage: Autoloop file.wav skip step minlen tail loopcount threadcnt");
			return;
		}

		RiffWave wav = loadWaveFile(args[0]);
		int[][] samples = wav.getSamples();
		float[][] normalized = Normalizer.normalize(samples);

		int skip = Integer.parseInt(args[1]);
		int step = Integer.parseInt(args[2]);
		int minlen = Integer.parseInt(args[3]);
		int tail = Integer.parseInt(args[4]);

		if(samples.length == 1) {
			System.out.printf("%d channel, %d samples\n", samples.length, samples[0].length);
		} else {
			System.out.printf("%d channels, %d samples\n", samples.length, samples[0].length);
		}

		Loop[] loops;
		int loopcnt = Integer.parseInt(args[5]);
		int threadcnt = Integer.parseInt(args[6]);
		loops = loop(normalized, skip, step, minlen, tail, loopcnt, threadcnt);

		if(loops != null) {
			for(Loop loop : loops) {
				float duration = (loop.end - loop.start) / (float) wav.getSampleRate();
				System.out.printf("loop: %d - %d [%1.2f sec, error=%s]\n", loop.start, loop.end,
						duration, loop.error / tail);
			}
		} else {
			System.out.println("failed to find loop");
		}
	}

	private static RiffWave loadWaveFile(String filename) throws IOException {
		try(InputStream in = new FileInputStream(filename)) {
			return Riff.read(in);
		}
	}

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
		private final float[][] samples;
		private final int minLength;
		private final int tailLength;
		private final int maxlen;
		private final Queue<Task> tasks;

		private volatile Loop[] loops;

		public ComputeThread(float[][] samples, int minLength, int tailLength, int count, Queue<Task> tasks) {
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
			int threadcnt) throws InterruptedException {
		int maxlen = samples[0].length - tailLength - skip;
		ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
		ComputeThread[] threads = new ComputeThread[threadcnt];

		for(int start = skip; start < maxlen; start += step) {
			tasks.add(new Task(start));
		}

		// spawn threads
		for(int i = 0; i < threadcnt; i++) {
			threads[i] = new ComputeThread(samples, minLength, tailLength, loopcnt, tasks);
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
					result[j++] = loops[i];
				}
			}
		} else {
			result = loops;
		}

		// sort result according to score
		Arrays.sort(result);

		return result;
	}

	public static float estimate(float[][] samples, int start, int end, int tailLength) {
		float error = 0;
		int channels = samples.length;
		for(int ch = 0; ch < channels; ch++) {
			for(int i = 0; i < tailLength; i++) {
				float a = samples[ch][start + i];
				float b = samples[ch][end + i];
				float diff = (a - b) * (a - b);
				error += diff;
			}
		}
		if(!Double.isFinite(error)) {
			System.out.println("Error is not finite");
		}
		return error;
	}
}
