package org.hackyourlife.audio.io.riff;

import java.io.IOException;

import org.hackyourlife.audio.io.WordInputStream;
import org.hackyourlife.audio.io.WordOutputStream;

public class InstrumentChunk extends Chunk {
	public static final int MAGIC = 0x74736e69; // 'inst'

	private byte unshiftedNote;
	private byte fineTune;
	private byte gain;
	private byte lowNote;
	private byte highNote;
	private byte lowVelocity;
	private byte highVelocity;

	public InstrumentChunk() {
		super(MAGIC);
		fineTune = 0;
		gain = 0;
		lowNote = 0;
		highNote = 127;
		lowVelocity = 0;
		highVelocity = 127;
	}

	@Override
	public int size() {
		return 7;
	}

	@Override
	protected void writeData(WordOutputStream out) throws IOException {
		out.write8bit(unshiftedNote);
		out.write8bit(fineTune);
		out.write8bit(gain);
		out.write8bit(lowNote);
		out.write8bit(highNote);
		out.write8bit(lowVelocity);
		out.write8bit(highVelocity);
	}

	@Override
	protected void readData(WordInputStream in, int size) throws IOException {
		unshiftedNote = (byte) in.read8bit();
		fineTune = (byte) in.read8bit();
		gain = (byte) in.read8bit();
		lowNote = (byte) in.read8bit();
		highNote = (byte) in.read8bit();
		lowVelocity = (byte) in.read8bit();
		highVelocity = (byte) in.read8bit();
		if(size > size()) {
			in.skip(size - size());
		}
	}
}
