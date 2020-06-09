package org.hackyourlife.audio.io.riff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hackyourlife.audio.io.WordInputStream;
import org.hackyourlife.audio.io.WordOutputStream;

public class SampleChunk extends Chunk {
	public static final int MAGIC = 0x6c706d73; // 'smpl'

	private int manufacturer;
	private int product;
	private int samplePeriod;
	private int midiUnityNote;
	private int midiPitchFraction;
	private int smpteFormat;
	private int smpteOffset;
	private byte[] samplerData = new byte[0];

	private final List<SampleLoop> sampleLoops = new ArrayList<>();

	public SampleChunk() {
		super(MAGIC);
	}

	public byte[] getManufacturer() {
		if(manufacturer == 0) {
			return new byte[0];
		} else if((manufacturer & 0xFF000000) == 0x01000000) {
			return new byte[] { (byte) manufacturer };
		} else if((manufacturer & 0xFF000000) == 0x03000000) {
			return new byte[] { (byte) (manufacturer >> 16), (byte) (manufacturer >> 8),
					(byte) manufacturer };
		} else {
			throw new IllegalStateException(
					"invalid manufacturer 0x" + Integer.toUnsignedString(manufacturer, 16));
		}
	}

	public void clearManufacturer() {
		manufacturer = 0;
	}

	public void setManufacturer(byte manufacturer) {
		this.manufacturer = 0x01 | Byte.toUnsignedInt(manufacturer);
	}

	public void setManufacturer(byte m1, byte m2, byte m3) {
		this.manufacturer = 0x03 | (Byte.toUnsignedInt(m1) << 16) | (Byte.toUnsignedInt(m2) << 8) |
				Byte.toUnsignedInt(m3);
	}

	public int getProduct() {
		return product;
	}

	public void setProduct(int product) {
		this.product = product;
	}

	public int getSamplePeriod() {
		return samplePeriod;
	}

	public void setSamplePeriod(int samplePeriod) {
		this.samplePeriod = samplePeriod;
	}

	public int getMidiUnityNote() {
		return midiUnityNote;
	}

	public void setMidiUnityNote(int midiUnityNote) {
		if(midiUnityNote < 0 || midiUnityNote > 127) {
			throw new IllegalArgumentException("invalid midi note");
		}
		this.midiUnityNote = midiUnityNote;
	}

	public int getMidiPitchFraction() {
		return midiPitchFraction;
	}

	public void setMidiPitchFraction(int midiPitchFraction) {
		this.midiPitchFraction = midiPitchFraction;
	}

	public int getSmpteFormat() {
		return smpteFormat;
	}

	public void setSmpteFormat(int smpteFormat) {
		this.smpteFormat = smpteFormat;
	}

	public int getSmpteOffset() {
		return smpteOffset;
	}

	public void setSmpteOffset(int smpteOffset) {
		this.smpteOffset = smpteOffset;
	}

	public void setSamplerData(byte[] samplerData) {
		if(samplerData == null) {
			this.samplerData = new byte[0];
		} else {
			this.samplerData = samplerData;
		}
	}

	public List<SampleLoop> getSampleLoops() {
		return Collections.unmodifiableList(sampleLoops);
	}

	public void addSampleLoop(SampleLoop loop) {
		sampleLoops.add(loop);
	}

	public void removeSampleLoop(SampleLoop loop) {
		sampleLoops.remove(loop);
	}

	public void clearSampleLoops() {
		sampleLoops.clear();
	}

	@Override
	public int size() {
		return 36 + sampleLoops.size() * 24 + samplerData.length;
	}

	@Override
	protected void writeData(WordOutputStream out) throws IOException {
		out.write32bit(manufacturer);
		out.write32bit(product);
		out.write32bit(samplePeriod);
		out.write32bit(midiUnityNote);
		out.write32bit(midiPitchFraction);
		out.write32bit(smpteFormat);
		out.write32bit(smpteOffset);
		out.write32bit(sampleLoops.size());
		out.write32bit(samplerData.length);
		for(SampleLoop loop : sampleLoops) {
			loop.write(out);
		}
		out.write(samplerData);
	}

	@Override
	protected void readData(WordInputStream in, int size) throws IOException {
		long pos = in.tell();
		manufacturer = in.read32bit();
		product = in.read32bit();
		samplePeriod = in.read32bit();
		midiUnityNote = in.read32bit();
		midiPitchFraction = in.read32bit();
		smpteFormat = in.read32bit();
		smpteOffset = in.read32bit();
		int loopCount = in.read32bit();
		int dataSize = in.read32bit();
		sampleLoops.clear();
		for(int i = 0; i < loopCount; i++) {
			SampleLoop loop = new SampleLoop();
			loop.read(in);
			sampleLoops.add(loop);
		}
		samplerData = new byte[dataSize];
		in.read(samplerData);
		long end = in.tell();
		long read = end - pos;
		if(read < size) {
			in.skip(size - read);
		}
	}

	public static class SampleLoop {
		public static final int LOOP_FORWARD = 0;
		public static final int ALTERNATING_LOOP = 1;
		public static final int LOOP_BACKWARD = 2;

		private int identifier;
		private int type;
		private int start;
		private int end;
		private int fraction;
		private int playCount;

		public SampleLoop() {
			type = LOOP_FORWARD;
		}

		public SampleLoop(int identifier, int type, int start, int end, int fraction, int playCount) {
			this.identifier = identifier;
			this.type = type;
			this.start = start;
			this.end = end;
			this.fraction = fraction;
			this.playCount = playCount;
		}

		public int getIdentifier() {
			return identifier;
		}

		public void setIdentifier(int identifier) {
			this.identifier = identifier;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public int getFraction() {
			return fraction;
		}

		public void setFraction(int fraction) {
			this.fraction = fraction;
		}

		public int getPlayCount() {
			return playCount;
		}

		public void setPlayCount(int playCount) {
			this.playCount = playCount;
		}

		public void read(WordInputStream in) throws IOException {
			identifier = in.read32bit();
			type = in.read32bit();
			start = in.read32bit();
			end = in.read32bit();
			fraction = in.read32bit();
			playCount = in.read32bit();
		}

		public void write(WordOutputStream out) throws IOException {
			out.write32bit(identifier);
			out.write32bit(type);
			out.write32bit(start);
			out.write32bit(end);
			out.write32bit(fraction);
			out.write32bit(playCount);
		}
	}
}
