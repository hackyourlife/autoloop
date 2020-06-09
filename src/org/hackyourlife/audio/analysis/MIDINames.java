package org.hackyourlife.audio.analysis;

public class MIDINames {
	public static final String[] NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
	public static final int[] NUMBERS = {
			/* A */ 9,
			/* B */ 11,
			/* C */ 0,
			/* D */ 2,
			/* E */ 4,
			/* F */ 5,
			/* G */ 7
	};

	public static String getNoteName(int note) {
		int octave = note / 12;
		return NAMES[note % 12] + (octave - 2);
	}

	public static int getNoteNumber(String name) {
		if(name.length() < 2 || name.length() > 4) {
			throw new IllegalArgumentException("invalid name");
		}

		String upper = name.toUpperCase();
		char c = upper.charAt(0);
		int key = 0;
		int octave = 0;
		int pos = 1;
		if(c < 'A' || c > 'G') {
			throw new IllegalArgumentException("invalid name");
		} else {
			key = NUMBERS[c - 'A'];
		}
		if(upper.charAt(1) == '#') {
			key++;
			pos++;
			if(upper.length() == 2) {
				throw new IllegalArgumentException("invalid name");
			}
		}
		if(upper.charAt(pos) == '-') {
			if(upper.length() == pos + 1) {
				throw new IllegalArgumentException("invalid name");
			}
			c = upper.charAt(pos + 1);
			if(c >= '0' && c <= '2') {
				octave = -(c - '0');
			} else {
				throw new IllegalArgumentException("invalid name");
			}
		} else if(upper.length() > pos + 1) {
			throw new IllegalArgumentException("invalid name");
		} else {
			c = upper.charAt(pos);
			if(c >= '0' && c <= '9') {
				octave = c - '0';
			} else {
				throw new IllegalArgumentException("invalid name");
			}
		}
		return key + (octave + 2) * 12;
	}
}
