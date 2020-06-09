package org.hackyourlife.audio.analysis;

public class Loop implements Comparable<Loop> {
	public final int start;
	public final int end;
	public final int length;
	public final float error;

	public Loop(int start, int end, float error) {
		if(end <= start) {
			throw new IllegalArgumentException();
		}

		this.start = start;
		this.end = end;
		this.length = end - start;
		this.error = error;
	}

	@Override
	public int compareTo(Loop other) {
		if(other == null) {
			return -1;
		} else {
			return Double.compare(error, other.error);
		}
	}

	@Override
	public String toString() {
		return "Loop[start=" + start + ";end=" + end + ";error=" + error + "]";
	}
}
