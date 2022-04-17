/* 
 * FLAC library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

package io.nayuki.flac.decode;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;


/**
 * A FLAC input stream based on a {@link RandomAccessFile}.
 */
public final class SeekableFileFlacInput extends AbstractFlacLowLevelInput {
	
	/*---- Fields ----*/
	
	// The underlying byte-based input stream to read from.
	private SubRandomAccessFile sraf;
	
	
	
	/*---- Constructors ----*/
	
	public SeekableFileFlacInput(File file) throws IOException {
		super();
		Objects.requireNonNull(file);
		this.sraf = new SubRandomAccessFile(file);
	}
	
	//sahlaysta custom constructor
	public SeekableFileFlacInput(SubRandomAccessFile sraf) throws IOException {
		this.sraf = sraf;
	}
	
	
	
	/*---- Methods ----*/
	
	public long getLength() {
		try {
			return sraf.length();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public void seekTo(long pos) throws IOException {
		sraf.seek(pos);
		positionChanged(pos);
	}
	
	
	protected int readUnderlying(byte[] buf, int off, int len) throws IOException {
		return sraf.read(buf, off, len);
	}
	
	
	// Closes the underlying RandomAccessFile stream (very important).
	public void close() throws IOException {
		if (sraf != null) {
			sraf.close();
			sraf = null;
			super.close();
		}
	}
	
}
