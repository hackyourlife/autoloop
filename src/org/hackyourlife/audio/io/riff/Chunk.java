package org.hackyourlife.audio.io.riff;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hackyourlife.audio.io.FourCC;
import org.hackyourlife.audio.io.WordInputStream;
import org.hackyourlife.audio.io.WordOutputStream;

public abstract class Chunk {
	public static final int JUNK = 0x4b4e554a; // 'JUNK'

	private final int id;

	private static final Map<Integer, Class<? extends Chunk>> CHUNKS = new HashMap<>();

	static {
		CHUNKS.put(WaveFormatChunk.MAGIC, WaveFormatChunk.class);
		CHUNKS.put(InstrumentChunk.MAGIC, InstrumentChunk.class);
		CHUNKS.put(DataChunk.MAGIC, DataChunk.class);
		CHUNKS.put(SampleChunk.MAGIC, SampleChunk.class);
	}

	protected Chunk(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public abstract int size();

	public final int getChunkSize() {
		return size() + 8;
	}

	protected String getChunkName() {
		return FourCC.fourCC(Integer.reverseBytes(id));
	}

	protected final void write(WordOutputStream out) throws IOException {
		out.write32bit(id);
		out.write32bit(size());
		writeData(out);
	}

	protected abstract void writeData(WordOutputStream out) throws IOException;

	protected final void readChunk(WordInputStream in) throws IOException {
		int size = in.read32bit();
		long start = in.tell();
		readData(in, size);
		if((size & 1) != 0) {
			in.read8bit();
			size++;
		}
		long end = in.tell();
		if((int) (end - start) != size) {
			throw new IOException(
					"error parsing chunk: invalid size of " + getChunkName() + " chunk: " +
							(end - start) + " vs " + size);
		}
	}

	protected abstract void readData(WordInputStream in, int size) throws IOException;

	protected static final <T extends Chunk> T read(WordInputStream in) throws IOException {
		int type = in.read32bit();
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) CHUNKS.get(type);
		if(clazz == null) {
			int size = in.read32bit();
			if(type != JUNK) {
				System.out.println("unknown chunk type " + FourCC.fourCC(Integer.reverseBytes(type)) + " [" +
						size + " bytes]");
			}
			if((size & 1) != 0) {
				size++;
			}
			in.skip(Integer.toUnsignedLong(size));
			return null;
		}
		try {
			T chunk = clazz.newInstance();
			chunk.readChunk(in);
			return chunk;
		} catch(InstantiationException | IllegalAccessException e) {
			System.out.println("Cannot create instance: " + e);
			throw new IOException(
					"cannot create chunk of type " + FourCC.fourCC(Integer.reverseBytes(type)));
		}
	}
}
