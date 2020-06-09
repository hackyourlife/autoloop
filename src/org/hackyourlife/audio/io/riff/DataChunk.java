package org.hackyourlife.audio.io.riff;

import java.io.IOException;

import org.hackyourlife.audio.io.WordInputStream;
import org.hackyourlife.audio.io.WordOutputStream;

public class DataChunk extends Chunk {
	public static final int MAGIC = 0x61746164; // 'data'

	private byte[] data;

	public DataChunk() {
		super(MAGIC);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public int size() {
		if(data == null) {
			return 0;
		} else {
			return data.length;
		}
	}

	@Override
	protected void writeData(WordOutputStream out) throws IOException {
		out.write(data);
	}

	@Override
	protected void readData(WordInputStream in, int size) throws IOException {
		data = new byte[size];
		in.read(data);
	}

}
