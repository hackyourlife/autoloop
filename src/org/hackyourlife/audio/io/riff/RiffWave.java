package org.hackyourlife.audio.io.riff;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hackyourlife.audio.io.Endianess;
import org.hackyourlife.audio.io.WordInputStream;
import org.hackyourlife.audio.io.WordOutputStream;

public class RiffWave extends Riff {
	public static final int MAGIC = 0x45564157; // 'WAVE'

	private final Map<Integer, Chunk> chunks = new HashMap<>();

	public RiffWave() {
		super(MAGIC);
	}

	@SuppressWarnings("unchecked")
	public <T extends Chunk> T get(int id) {
		return (T) chunks.get(id);
	}

	public void set(Chunk c) {
		chunks.put(c.getId(), c);
	}

	public int getSampleRate() {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		return c.getSampleRate();
	}

	public void setSampleRate(int sampleRate) {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		c.setSampleRate(sampleRate);
	}

	public int getChannels() {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		return c.getChannels();
	}

	public void setChannels(int channels) {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		c.setChannels((short) channels);
	}

	public short getSampleFormat() {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		return c.getFormat();
	}

	public void setSampleFormat(short sampleFormat) {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		c.setFormat(sampleFormat);
	}

	public short getBitsPerSample() {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		return c.getBitsPerSample();
	}

	public void setBitsPerSample(int bitsPerSample) {
		WaveFormatChunk c = get(WaveFormatChunk.MAGIC);
		c.setBitsPerSample((short) bitsPerSample);
	}

	private DataChunk getData() {
		return get(DataChunk.MAGIC);
	}

	public short[] get16bitSamples() {
		if(getBitsPerSample() != 16 || getSampleFormat() != WaveFormatChunk.WAVE_FORMAT_PCM) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		byte[] rawSamples = getData().getData();
		short[] samples = new short[rawSamples.length / 2];
		for(int i = 0; i < samples.length; i++) {
			samples[i] = Endianess.get16bitLE(rawSamples, 2 * i);
		}
		return samples;
	}

	public void set16bitSamples(short[] samples) {
		byte[] rawSamples = new byte[samples.length * 2];
		for(int i = 0; i < samples.length; i++) {
			Endianess.set16bitLE(rawSamples, 2 * i, samples[i]);
		}
		getData().setData(rawSamples);
		setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
		setBitsPerSample(16);
	}

	public void set16bitSamples(float[] samples) {
		if(getBitsPerSample() != 16) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		byte[] rawSamples = new byte[samples.length * 2];
		for(int i = 0; i < samples.length; i++) {
			float sample = samples[i];
			sample *= Short.MAX_VALUE;
			short value = (short) Math.min(Math.max(sample, Short.MAX_VALUE), Short.MIN_VALUE);
			Endianess.set16bitLE(rawSamples, 2 * i, value);
		}
		getData().setData(rawSamples);
		setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
		setBitsPerSample(16);
	}

	public void set24bitSamples(int[] samples) {
		if(getBitsPerSample() != 16) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		byte[] rawSamples = new byte[samples.length * 3];
		for(int i = 0; i < samples.length; i++) {
			int value = samples[i] >> 8;
			Endianess.set24bitLE(rawSamples, 3 * i, value);
		}
		getData().setData(rawSamples);
		setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
		setBitsPerSample(24);
	}

	public void set24bitSamples(float[] samples) {
		if(getBitsPerSample() != 16) {
			throw new IllegalStateException(
					"cannot get 16bit samples for " + getBitsPerSample() + "bit sample data");
		}
		byte[] rawSamples = new byte[samples.length * 3];
		for(int i = 0; i < samples.length; i++) {
			int value = (int) (samples[i] * 16777216);
			Endianess.set24bitLE(rawSamples, 3 * i, value);
		}
		getData().setData(rawSamples);
		setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
		setBitsPerSample(24);
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
		int bits = getBitsPerSample();
		int format = getSampleFormat();
		byte[] rawSamples = getData().getData();
		if(bits == 8) {
			int[][] samples = new int[nch][rawSamples.length / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = rawSamples[off] << 24;
				}
			}
			return samples;
		} else if(bits == 16) {
			int[][] samples = new int[nch][rawSamples.length / 2 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get16bitLE(rawSamples, 2 * off) << 16;
				}
			}
			return samples;
		} else if(bits == 24) {
			int[][] samples = new int[nch][rawSamples.length / 3 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get24bitLE(rawSamples, 3 * off) << 8;
				}
			}
			return samples;
		} else if(bits == 32) {
			int[][] samples = new int[nch][rawSamples.length / 4 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					int sample = Endianess.get32bitLE(rawSamples, 4 * off);
					if(format == WaveFormatChunk.WAVE_FORMAT_IEEE_FLOAT) {
						float value = Float.intBitsToFloat(sample);
						assert value >= -1.0f && value <= 1.0f;
						samples[ch][i] = (int) (value * Integer.MAX_VALUE);
					} else {
						samples[ch][i] = sample;
					}
				}
			}
			return samples;
		} else {
			throw new IllegalStateException("cannot get samples for " + bits + "bit sample data");
		}
	}

	public float[][] getFloatSamples() {
		int nch = getChannels();
		int bits = getBitsPerSample();
		int format = getSampleFormat();
		byte[] rawSamples = getData().getData();
		if(bits == 8) {
			float[][] samples = new float[nch][rawSamples.length / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = rawSamples[off] / 128.0f;
				}
			}
			return samples;
		} else if(bits == 16) {
			float[][] samples = new float[nch][rawSamples.length / 2 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get16bitLE(rawSamples, 2 * off) / 32768.0f;
				}
			}
			return samples;
		} else if(bits == 24) {
			float[][] samples = new float[nch][rawSamples.length / 3 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					samples[ch][i] = Endianess.get24bitLE(rawSamples, 3 * off) / 16777216.0f;
				}
			}
			return samples;
		} else if(bits == 32) {
			float[][] samples = new float[nch][rawSamples.length / 4 / nch];
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					int value = Endianess.get32bitLE(rawSamples, 4 * off);
					if(format == WaveFormatChunk.WAVE_FORMAT_IEEE_FLOAT) {
						samples[ch][i] = Float.intBitsToFloat(value);
					} else {
						samples[ch][i] = value / (float) 0x80000000L;
					}
				}
			}
			return samples;
		} else {
			throw new IllegalStateException("cannot get samples for " + bits + "bit sample data");
		}
	}

	public void setSamples(int[][] samples) {
		if(samples == null) {
			throw new NullPointerException("no samples");
		}
		int nch = samples.length;
		if(nch < 1) {
			throw new IllegalArgumentException("need at least one channel");
		}
		int bits = getBitsPerSample();
		int format = getSampleFormat();
		int samplecnt = samples[0].length;
		byte[] rawSamples = new byte[nch * samplecnt * (bits / 8)];
		setChannels(nch);
		if(bits == 8) {
			setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					rawSamples[off] = (byte) (samples[ch][i] >> 24);
				}
			}
		} else if(bits == 16) {
			setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					Endianess.set16bitLE(rawSamples, 2 * off, (short) (samples[ch][i] >> 16));
				}
			}
		} else if(bits == 24) {
			setSampleFormat(WaveFormatChunk.WAVE_FORMAT_PCM);
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					Endianess.set24bitLE(rawSamples, 3 * off, samples[ch][i] >> 8);
				}
			}
		} else if(bits == 32) {
			for(int ch = 0; ch < nch; ch++) {
				for(int i = 0; i < samples[ch].length; i++) {
					int off = i * nch + ch;
					int sample = samples[ch][i];
					if(format == WaveFormatChunk.WAVE_FORMAT_IEEE_FLOAT) {
						float value = (float) (sample / (double) Integer.MAX_VALUE);
						sample = Float.floatToRawIntBits(value);
					}
					Endianess.set32bitLE(rawSamples, 4 * off, sample);
				}
			}
		} else {
			throw new IllegalStateException("cannot get samples for " + bits + "bit sample data");
		}
		getData().setData(rawSamples);
	}

	@Override
	public int size() {
		int size = 0;
		for(Chunk c : chunks.values()) {
			size += c.getChunkSize();
		}
		return size;
	}

	@Override
	protected void readData(WordInputStream in, int size) throws IOException {
		chunks.clear();

		long start = in.tell();
		for(long pos = in.tell(); (pos - start) < Integer.toUnsignedLong(size); pos = in.tell()) {
			Chunk c = Chunk.read(in);
			if(c != null) {
				set(c);
			}
		}

		if(get(WaveFormatChunk.MAGIC) == null) {
			throw new IOException("no fmt chunk found");
		}
		if(get(DataChunk.MAGIC) == null) {
			throw new IOException("no data chunk found");
		}
	}

	@Override
	protected void writeData(WordOutputStream out) throws IOException {
		get(WaveFormatChunk.MAGIC).write(out);
		for(Chunk c : chunks.values()) {
			int id = c.getId();
			switch(id) {
			case WaveFormatChunk.MAGIC:
			case DataChunk.MAGIC:
				break;
			default:
				c.write(out);
			}
		}
		get(DataChunk.MAGIC).write(out);
	}
}
