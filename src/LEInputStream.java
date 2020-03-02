import java.io.IOException;
import java.io.InputStream;

public class LEInputStream extends WordInputStream {
	public LEInputStream(InputStream parent) {
		super(parent);
	}

	public LEInputStream(InputStream parent, long offset) {
		super(parent, offset);
	}

	@Override
	public int read8bit() throws IOException {
		return read();
	}

	@Override
	public short read16bit() throws IOException {
		byte[] buf = new byte[2];
		read(buf);
		return Endianess.get16bitLE(buf);
	}

	@Override
	public int read32bit() throws IOException {
		byte[] buf = new byte[4];
		read(buf);
		return Endianess.get32bitLE(buf);
	}

	@Override
	public long read64bit() throws IOException {
		byte[] buf = new byte[8];
		read(buf);
		return Endianess.get64bitLE(buf);
	}
}
