import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Wave {
	public static final int RIFF = 0x46464952;
	public static final int WAVE = 0x45564157;
	public static final int FMT = 0x20746D66;
	public static final int DATA = 0x61746164;

	public static final int PCM = 0x0001;

	private int format;
	private int channels;
	private int sampleRate;
	private int bitsPerSample;
	private int frameSize;

	private byte[] rawSamples;

	public Wave() {
		format = PCM;
		channels = 1;
		sampleRate = 44100;
		bitsPerSample = 8;
		frameSize = 1;
		rawSamples = new byte[0];
	}

	public Wave(InputStream in) throws IOException {
		this();
		read(in);
	}

	public int getChannels() {
		return channels;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public int getSampleCount() {
		if(getBitsPerSample() != 16) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		return rawSamples.length / (2 * getChannels());
	}

	public byte[] getRawSamples() {
		return rawSamples;
	}

	public short[] get16bitSamples() {
		if(getBitsPerSample() != 16) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		short[] samples = new short[rawSamples.length / 2];
		for(int i = 0; i < samples.length; i++) {
			samples[i] = Endianess.get16bitLE(rawSamples, 2 * i);
		}
		return samples;
	}

	public short[] get16bitMono() {
		short[] samples = get16bitSamples();
		if(getChannels() == 1) {
			return samples;
		} else {
			int nch = getChannels();
			short[] result = new short[samples.length / nch];
			for(int i = 0; i < result.length; i++) {
				int sum = 0;
				for(int ch = 0; ch < nch; ch++) {
					sum += samples[i * nch + ch];
					sum = Math.min(sum, Short.MAX_VALUE);
					sum = Math.max(sum, Short.MIN_VALUE);
				}
				result[i] = (short) sum;
			}
			return result;
		}
	}

	public short[] get16bitMono(int start, int end) {
		short[] samples = get16bitSamples();
		if(getChannels() == 1) {
			return samples;
		} else {
			int nch = getChannels();
			short[] result = new short[end - start];
			for(int i = 0; i < result.length; i++) {
				int sum = 0;
				for(int ch = 0; ch < nch; ch++) {
					sum += samples[(i + start) * nch + ch];
					sum = Math.min(sum, Short.MAX_VALUE);
					sum = Math.max(sum, Short.MIN_VALUE);
				}
				result[i] = (short) sum;
			}
			return result;
		}
	}

	public int[][] getSamples() {
		int nch = getChannels();
		if(getBitsPerSample() == 8) {
			int[][] samples = new int[nch][rawSamples.length / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = rawSamples[off] << 24;
				}
			}
			return samples;
		} else if(getBitsPerSample() == 16) {
			int[][] samples = new int[nch][rawSamples.length / 2 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get16bitLE(rawSamples, 2 * off) << 16;
				}
			}
			return samples;
		} else if(getBitsPerSample() == 24) {
			int[][] samples = new int[nch][rawSamples.length / 3 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get24bitLE(rawSamples, 3 * off) << 8;
				}
			}
			return samples;
		} else {
			throw new IllegalStateException(
					"cannot get samples for " + getBitsPerSample() + "bit sample data");
		}
	}

	public void read(InputStream data) throws IOException {
		WordInputStream in = new LEInputStream(data);
		int magic = in.read32bit();
		if(magic != RIFF) {
			throw new IOException("not a RIFF file");
		}

		int fileSize = in.read32bit();
		if(fileSize < 0x24) {
			throw new IOException("invalid file size");
		}

		int riffType = in.read32bit();
		if(riffType != WAVE) {
			throw new IOException("not a RIFF WAVE file");
		}

		boolean hasFMT = false;
		boolean hasDATA = false;
		while(!hasFMT || !hasDATA) {
			int chunk = in.read32bit();
			int chunkSize = in.read32bit();
			switch(chunk) {
			case FMT: {
				if(chunkSize != 16) {
					throw new IOException("invalid fmt chunk length");
				}
				format = in.read16bit();
				channels = in.read16bit();
				sampleRate = in.read32bit();
				int bytesPerSecond = in.read32bit();
				frameSize = in.read16bit();
				bitsPerSample = in.read16bit();

				if(frameSize != channels * ((bitsPerSample + 7) / 8)) {
					throw new IOException("invalid frame size");
				}

				if(bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24) {
					throw new IOException("invalid bits per sample");
				}

				if(bytesPerSecond != sampleRate * frameSize) {
					throw new IOException("invalid bytes/second");
				}
				hasFMT = true;
				break;
			}
			case DATA: {
				rawSamples = new byte[chunkSize];
				int size = in.read(rawSamples);
				rawSamples = Arrays.copyOf(rawSamples, size); // truncate data size
				hasDATA = true;
				break;
			}
			default:
				in.skip(chunkSize);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Wave[channels=%d,sampleRate=%d,bits=%d]", channels, sampleRate, bitsPerSample);
	}
}
