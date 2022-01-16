package com.github.sahlaysta.shtooka;

import java.io.BufferedInputStream;
import java.io.InputStream;

//a bufferedinputstream with a resetable buffer
final class XBufferedInputStream extends BufferedInputStream {

	//Constructors
	XBufferedInputStream(InputStream in) {
		super(in);
	}
	XBufferedInputStream(InputStream in, int size){
		super(in, size);
	}
	
	//Reset buffer
	void resetBuffer() {
		count = pos = 0;
	}
}