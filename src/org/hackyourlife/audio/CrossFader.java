package org.hackyourlife.audio;

public class CrossFader {
	public static int[][] crossfade(int[][] a, int[][] b, int from, int to) {
		if(a.length != b.length) {
			throw new IllegalArgumentException("channel count mismatch");
		}

		int nch = a.length;
		int samplecnt = a[0].length;
		int len = to - from;
		int[][] result = new int[nch][samplecnt];
		for(int ch = 0; ch < nch; ch++) {
			if(a[ch].length != samplecnt || b[ch].length != samplecnt) {
				throw new IllegalArgumentException("sample count mismatch");
			}

			for(int i = 0; i < samplecnt; i++) {
				if(i < from) {
					result[ch][i] = a[ch][i];
				} else if(i > to) {
					result[ch][i] = b[ch][i];
				} else {
					int pos = i - from;
					double f = pos / (double) len;
					int partA = (int) (a[ch][i] * (1 - f));
					int partB = (int) (b[ch][i] * f);
					result[ch][i] = partA + partB;
				}
			}
		}

		return result;
	}

	public static void crossfade(int[][] a, int[][] b, int start) {
		if(a.length != b.length) {
			throw new IllegalArgumentException("channel count mismatch");
		}

		int nch = a.length;
		int samplecnt = a[0].length;

		int len = b[0].length;
		if(start + len > samplecnt) {
			throw new IllegalArgumentException("invalid start position");
		}

		for(int ch = 0; ch < nch; ch++) {
			for(int src = start, pos = 0; pos < len; src++, pos++) {
				double f = pos / (double) len;
				int partA = (int) (a[ch][src] * (1.0 - f));
				int partB = (int) (b[ch][pos] * f);
				a[ch][src] = partA + partB;
			}
		}
	}
}
