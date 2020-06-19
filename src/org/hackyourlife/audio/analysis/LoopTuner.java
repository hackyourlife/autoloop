package org.hackyourlife.audio.analysis;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LoopTuner {
	private static class Task {
		private final int start;
		private final int end;

		public Task(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	private static class ComputeThread extends Thread {
		private final float[][] samples;
		private final int offset;
		private final int tailLength;
		private final int maxlen;
		private final Queue<Task> tasks;

		private volatile Loop[] loops;

		public ComputeThread(float[][] samples, int offset, int tailLength, int count, Queue<Task> tasks) {
			this.samples = samples;
			this.offset = offset;
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
					int structureEnd = task.getEnd();
					int searchStart = structureEnd - offset;
					int searchEnd = structureEnd + offset;
					if(searchEnd > maxlen) {
						searchEnd = maxlen;
					}
					for(int end = searchStart; end < searchEnd; end++) {
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

	public static Loop[] loop(float[][] samples, int offset, int tailLength, int loopcnt, Loop[] structureLoops,
			int threadcnt) throws InterruptedException {
		ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
		ComputeThread[] threads = new ComputeThread[threadcnt];

		for(Loop loop : structureLoops) {
			tasks.add(new Task(loop.start, loop.end));
		}

		// spawn threads
		for(int i = 0; i < threadcnt; i++) {
			threads[i] = new ComputeThread(samples, offset, tailLength, loopcnt, tasks);
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
