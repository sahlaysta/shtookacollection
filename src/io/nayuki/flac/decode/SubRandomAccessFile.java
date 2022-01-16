package io.nayuki.flac.decode;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//a sub random access file inside a random access file
final class SubRandomAccessFile {

	final RandomAccessFile raf;
	final long offset;
	final long length;
	boolean close;
	
	SubRandomAccessFile(RandomAccessFile raf, long offset, long length)
			throws IOException {
		this.raf = raf;
		this.offset = offset;
		this.length = length;
		this.close = false;
		raf.seek(offset);
	}
	
	SubRandomAccessFile(File file) throws IOException {
		this(new RandomAccessFile(file, "r"), 0, -1);
		this.close = true;
	}
	
	long length() throws IOException {
		return length == -1 ? raf.length() : length;
	}
	
	void seek(long pos) throws IOException {
		raf.seek(pos + offset);
	}
	
	//read and calculate end of file behavior
	int read(byte[] b, int off, int len) throws IOException {
		if (length == -1)
			return raf.read(b, off, len);
		
		//calculate bytes that will be read
		int blen = (int)(length - (raf.getFilePointer() - offset));
		if (blen <= 0)
			return -1;
		else if (len > blen)
			return raf.read(b, off, blen);
		else
			return raf.read(b, off, len);
	}
	
	void close() throws IOException {
		if (close)
			raf.close();
	}
}
