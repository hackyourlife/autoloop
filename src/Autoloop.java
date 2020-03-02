import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Autoloop {
	public static class Loop {
		public final int start;
		public final int end;
		public final double error;

		public Loop(int start, int end, double error) {
			if(end <= start) {
				throw new IllegalArgumentException();
			}

			this.start = start;
			this.end = end;
			this.error = error;
		}

		@Override
		public String toString() {
			return "Loop[start=" + start + ";end=" + end + ";error=" + error + "]";
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if(args.length < 5) {
			System.out.println("Usage: Autoloop file.wav skip step minlen tail [threadcnt]");
			return;
		}
		try(InputStream in = new FileInputStream(args[0])) {
			Wave wav = new Wave(in);
			int[][] samples = wav.getSamples();
			double[][] normalized = new double[samples.length][samples[0].length];
			for(int ch = 0; ch < normalized.length; ch++) {
				for(int i = 0; i < normalized[ch].length; i++) {
					normalized[ch][i] = (double) samples[ch][i] / (double) Integer.MAX_VALUE;
				}
			}
			int skip = Integer.parseInt(args[1]);
			int step = Integer.parseInt(args[2]);
			int minlen = Integer.parseInt(args[3]);
			int tail = Integer.parseInt(args[4]);
			if(samples.length == 1) {
				System.out.printf("%d channel, %d samples\n", samples.length, samples[0].length);
			} else {
				System.out.printf("%d channels, %d samples\n", samples.length, samples[0].length);
			}
			Loop loop;
			if(args.length == 6) {
				int threadcnt = Integer.parseInt(args[5]);
				loop = loop(normalized, skip, step, minlen, tail, threadcnt);
			} else {
				loop = loop(normalized, skip, step, minlen, tail);
			}
			if(loop != null) {
				double duration = (loop.end - loop.start) / (double) wav.getSampleRate();
				System.out.printf("best match: %d - %d [%1.2f sec]\n", loop.start, loop.end, duration);
				double err = estimate(normalized, loop.start, loop.end, tail);
				System.out.printf("error: %s\n", err);
			} else {
				System.out.println("failed to find loop");
			}
		}
	}

	public static Loop loop(double[][] samples, int skip, int step, int minLength, int tailLength) {
		int maxlen = samples[0].length - tailLength;
		Loop best = null;
		double besterr = Double.MAX_VALUE;
		for(int start = skip; start < maxlen; start += step) {
			System.out.print("\rstart: " + start + " / " + maxlen);
			System.out.flush();
			for(int end = start + minLength; end < maxlen; end++) {
				double error = estimate(samples, start, end, tailLength);
				if(best == null || error < besterr) {
					best = new Loop(start, end, error);
					besterr = error;
				}
			}
		}
		System.out.println();
		// TODO: add threshold
		return best;
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
		private final double[][] samples;
		private final int minLength;
		private final int tailLength;
		private final int maxlen;
		private final Queue<Task> tasks;

		private volatile Loop loop;

		public ComputeThread(double[][] samples, int minLength, int tailLength, Queue<Task> tasks) {
			this.samples = samples;
			this.minLength = minLength;
			this.tailLength = tailLength;
			this.tasks = tasks;
			maxlen = samples[0].length - tailLength;
		}

		@Override
		public void run() {
			double besterr = Double.MAX_VALUE;
			Loop best = null;
			while(!tasks.isEmpty()) {
				Task task = tasks.remove();
				int start = task.getStart();
				for(int end = start + minLength; end < maxlen; end++) {
					double error = estimate(samples, start, end, tailLength);
					if(best == null || error < besterr) {
						best = new Loop(start, end, error);
						besterr = error;
					}
				}
			}
			this.loop = best;
		}

		public Loop getLoop() {
			return loop;
		}
	}

	public static Loop loop(double[][] samples, int skip, int step, int minLength, int tailLength, int threadcnt)
			throws InterruptedException {
		int maxlen = samples[0].length - tailLength - skip;
		ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<>();
		ComputeThread[] threads = new ComputeThread[threadcnt];

		for(int start = skip; start < maxlen; start += step) {
			tasks.add(new Task(start));
		}

		// spawn threads
		for(int i = 0; i < threadcnt; i++) {
			threads[i] = new ComputeThread(samples, minLength, tailLength, tasks);
			threads[i].start();
		}

		// wait for results
		for(Thread t : threads) {
			t.join();
		}

		// collect results
		Loop loop = null;
		for(ComputeThread t : threads) {
			Loop l = t.getLoop();
			if(loop == null || l.error < loop.error) {
				loop = l;
			}
		}

		return loop;
	}

	public static double estimate(double[][] samples, int start, int end, int tailLength) {
		double error = 0;
		int channels = samples.length;
		for(int ch = 0; ch < channels; ch++) {
			for(int i = 0; i < tailLength; i++) {
				double a = samples[ch][start + i];
				double b = samples[ch][end + i];
				double diff = (a - b) * (a - b);
				error += diff;
			}
		}
		if(!Double.isFinite(error)) {
			System.out.println("Error is not finite");
		}
		return error;
	}
}
