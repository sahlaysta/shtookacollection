package sahlaysta.shtooka;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine.Info;

import io.nayuki.flac.common.StreamInfo;
import io.nayuki.flac.decode.FlacDecoder;

/**
 * Manages and plays audio voice clips from a Shtooka
 * .tar collection file completely with random access
 * file (the voice clips are not put in memory).
 * Those files are downloadable on Shtooka's page:
 * http://shtooka.net/download.php
 * 
 * <p>Extends {@link RandomAccessFile} (read only)
 * and should also be closed with the {@link #close()} method.
 * The operations of a Shtooka Collection will no
 * longer work and will throw
 * {@link UnsupportedOperationException}
 * after it is closed.
 * 
 * @author sahlaysta
 * @see ShtookaVoiceClip
 * */
public class ShtookaCollection extends RandomAccessFile {
	
	/** The offset data and voice clips of
	 * this Shtooka Collection. */
	protected ShtookaVoiceClip[] voiceClips;
	
	//true if closed
	private boolean closed;
	
	
	//Constructors
	/** Opens random access file to the specified
	 * file and initializes a Shtooka Collection.
	 * @param file the file of the Shtooka Collection
	 * @throws IOException if an I/O error occurs
	 * */
	public ShtookaCollection(String file) throws IOException {
		super(file, "r");
		init();
	}
	
	/** Opens random access file to the specified
	 * file and initializes a Shtooka Collection.
	 * @param file the file of the Shtooka Collection
	 * @throws IOException if an I/O error occurs
	 * */
	public ShtookaCollection(File file) throws IOException {
		super(file, "r");
		init();
	}
	
	/** Opens random access file to the specified
	 * file and initializes a Shtooka Collection.
	 * @param file the file of the Shtooka Collection
	 * @throws IOException if an I/O error occurs
	 * @throws RuntimeException if the specified URL is not
	 * formatted strictly according to RFC2396 and cannot
	 * be converted to a URI
	 * */
	public ShtookaCollection(URL file) throws IOException {
		super(urlToFile(file), "r");
		init();
	}
	private static File urlToFile(URL url) {
		try {
			return Paths.get(url.toURI()).toFile();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	//Init offset data
	private static final class Node {//.tar entry read node
		final String filename;
		final long offset, size;
		Node(String filename, long offset, long size) {
			this.filename = filename;
			this.offset = offset;
			this.size = size;
		}
	}
	//read all .tar file entries and obtain voice clip offsets
	private final XBufferedInputStream xbis
		= new XBufferedInputStream(
			Channels.newInputStream(
				getChannel()));
	private final Reader reader = new InputStreamReader(xbis, StandardCharsets.UTF_8);
	private void init() throws IOException {
		List<Node> nodes = new ArrayList<>();
		
		//go to file start
		seek(0);
		xbis.resetBuffer();
		
		//tar entry header
		byte[] b = new byte[135];
		
		//read .tar file
		boolean tagsRead = false;
		List<Tag> tags = new ArrayList<>();
		while (true) {
			//Read tar entry header
			int bytesRead = read(b, 0, b.length);
			if (bytesRead != b.length)
				break;
			
			//Read tar entry filename
			String filename = readName(b);
			
			//Read tar entry file size
			int size = readSize(b);
			
			//Skip the rest of the header
			skipBytes(377);
			
			/* Add offset data to the
			 * result / read tags */
			String lc = filename.toLowerCase();
			long offset = getFilePointer();
			if (lc.endsWith(".flac")) {
				//add result
				nodes.add(new Node(filename, offset, size));
			} else if (!tagsRead && lc.equals("flac/index.xml")) {
				//read tags from index.xml
				readTags(tags, "flac/");
				tagsRead = true;
				seek(offset);
			}
			
			//Skip file
			skipBytes(size);
			
			//Go to next non-zero byte
			long pos = getFilePointer();
			long zeroes = 0;
			xbis.resetBuffer();
			while (xbis.read() == 0)
				zeroes++;
			seek(pos + zeroes);
		}
		
		if (!tagsRead)
			throw new IllegalArgumentException(
				"Failed to read tags: flac/index.xml file not found in .tar");
		
		//reset seek and buffer
		seek(0);
		xbis.resetBuffer();
		
		//create voice clips array
		ShtookaVoiceClip[] arr = new ShtookaVoiceClip[nodes.size()];
		List<String> names = new ArrayList<>();
		for (int i = 0; i < arr.length; i++) {
			names.clear();
			Node node = nodes.get(i);
			//add voice clip name from tags
			for (Tag tag: tags)
				if (Util.stringsEqual(node.filename, tag.filename))
					names.add(tag.swactext);
			//names string array
			String[] namesArr = new String[names.size()];
			for (int j = 0; j < namesArr.length; j++)
				namesArr[j] = names.get(j);
			//put result voice clip
			arr[i] = new ShtookaVoiceClip(
				this, node.filename, node.offset, node.size, namesArr);
		}
		this.voiceClips = arr;
	}
	private String readName(byte[] b) throws IOException {
		//reads a 100 char string (tar format)
		int len = 100;
		for (int i = 0; i < len; i++) {
			if (b[i] == 0) {
				len = i;
				break;
			}
		}
		return new String(b, 0, len, StandardCharsets.UTF_8);
	}
	private int readSize(byte[] b) throws IOException {
		//convert octal number (tar format)
		int result = 0;
		for (int i = b.length - 11, len = b.length; i < len; i++)
			result = result * 8 - Character.digit((char)b[i], 8);
		return -result;
	}
	private static final char[] FN_DELIM = "file path=\"".toCharArray();
	private static final char[] TX_DELIM = "swac_text=\"".toCharArray();
	private static final class Tag {//shtooka entry tag class
		final String filename, swactext;
		Tag(String filename, String swactext) {
			this.filename = filename;
			this.swactext = swactext;
		}
	}
	private void readTags(List<Tag> tags, String filenamePrefix) throws IOException {
		//parse the index.xml
		xbis.resetBuffer();
		
		int level = 0;//the xml hierarchy level <>
		
		//read tags in index.xml
		while (true) {
			//skip the XML string filename delimiter
			level = xmlReadUntil(FN_DELIM, level);
			if (level == -1) //XML end of file cond
				break;
			//read the filename string
			String filename = filenamePrefix + xmlReadStr(new StringBuilder(17));
			
			//skip the XML string swac text delimiter
			level = xmlReadUntil(TX_DELIM, level);
			if (level == -1) //XML end of file cond
				break;
			//read the swac text string
			String swactext = xmlReadStr(new StringBuilder()).toLowerCase();
			
			//add tags to list
			tags.add(new Tag(filename, swactext));
		}
	}
	private int xmlReadUntil(char[] delim, int level) throws IOException {
		/* skips bytes until the char array delimiter
		 * is met, while updating the XML hierarchy level */
		e: while (true) {
			for (int c: delim) {
				int r = reader.read();
				if (r != c) {
					if (r == '<') {
						level++;
					} else if (r == '>') {
						if (level <= 0) //xml eof
							return -1;
						level--;
					}
					continue e;//delimiter not met
				}
			}
			return level;//delimiter met
		}
	}
	private String xmlReadStr(StringBuilder sb) throws IOException {
		//reads a string to the stringbuilder from XML
		while (true) {
			int c = reader.read();
			if (c == '\"')
				break;
			sb.appendCodePoint(c);
		}
		return sb.toString();
	}
	
	
	//Public operations
	/** Closes this Shtooka Collection to the
	 * tar file. The operations of this
	 * Shtooka Collection will no longer work
	 * afterwards.
	 * @throws IOException if an I/O error occurs
	 * */
	@Override
	public void close() throws IOException {
		reader.close();
		xbis.close();
		super.close();
		voiceClips = null;
		closed = true;
	}
	
	/** Returns {@code true} if this Shtooka Collection
	 * has been closed.
	 * @return {@code true} if this Shtooka Collection
	 * has been closed
	 * @see #close() */
	public boolean isClosed() {
		return closed;
	}
	
	
	//Shtooka Collection closed check
	void checkClosed() {
		if (isClosed())
			throw new UnsupportedOperationException(
				"Shtooka Collection has been closed");
	}
	
	/** Returns a new array of all of the
	 * Shtooka Voice Clips of this collection.
	 * @return a new array of all voice clips
	 * @throws UnsupportedOperationException if this
	 * Shtooka Collection has been closed */
	public ShtookaVoiceClip[] getVoiceClips() {
		checkClosed();
		ShtookaVoiceClip[] result = new ShtookaVoiceClip[voiceClips.length];
		for (int i = 0; i < result.length; i++)
			result[i] = voiceClips[i];
		return result;
	}
	
	/** Returns the number of Shtooka Voice Clips
	 * in this Shtooka Collection.
	 * @return the number of voice clips
	 * @throws UnsupportedOperationException if this
	 * Shtooka Collection has been closed */
	public int getVoiceClipCount() {
		checkClosed();
		return voiceClips.length;
	}
	
	/** Returns a Shtooka Voice Clip from this
	 * Shtooka Collection by its name. Usually,
	 * a voice clip's name is what the
	 * speaker pronounces in the voice clip.
	 * @param name the voice clip name
	 * @return a voice clip with the specified
	 * name, or {@code null} if none
	 * @throws UnsupportedOperationException if this
	 * Shtooka Collection has been closed */
	public ShtookaVoiceClip getVoiceClip(String name) {
		checkClosed();
		String namelc = name.toLowerCase();
		for (ShtookaVoiceClip svc: voiceClips)
			for (String s: svc.names)
				if (Util.stringsEqual(namelc, s))
					return svc;
		return null;
	}
	
	/** Returns the Shtooka Voice Clips from this
	 * Shtooka Collection that have the specified
	 * name. Usually, a voice clip's name is what
	 * the speaker pronounces in the voice clip.
	 * @param name the voice clip name
	 * @return an array of voice clips with the
	 * specified name, or an empty array if no
	 * voice clips have the specified name
	 * @throws UnsupportedOperationException if this
	 * Shtooka Collection has been closed */
	public ShtookaVoiceClip[] getVoiceClips(String name) {
		checkClosed();
		String namelc = name.toLowerCase();
		List<ShtookaVoiceClip> result = new ArrayList<>();
		for (ShtookaVoiceClip svc: voiceClips) {
			for (String s: svc.names) {
				if (Util.stringsEqual(namelc, s)) {
					result.add(svc);
					continue;
				}
			}
		}
		//list to array
		ShtookaVoiceClip[] arr = new ShtookaVoiceClip[result.size()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = result.get(i);
		return arr;
	}
	
	/** Plays the audio from this Shtooka Collection of
	 * the specified voice clip to the system audio output.
	 * @param voiceClip the voice clip audio to play
	 * @throws UnsupportedOperationException if this
	 * Shtooka Collection has been closed, or if audio
	 * from this Shtooka Collection is already currently
	 * playing on a separate thread
	 * @throws IllegalArgumentException if the owner
	 * Shtooka Collection of the specified voice clip
	 * is not this Shtooka Collection
	 * @throws IOException if an I/O error occurs
	 * @throws RuntimeException if an error occurs
	 * decoding FLAC audio */
	public void playVoiceClip(ShtookaVoiceClip voiceClip) throws IOException {
		checkClosed();
		if (voiceClip.owner != this)
			throw new IllegalArgumentException("Bad voice clip owner");
		try {
			playFlac(voiceClip);
		} catch (LineUnavailableException e) {
			throw new RuntimeException(e);
		}
	}
	
	private final AtomicBoolean playing = new AtomicBoolean(false);
	private void playFlac(ShtookaVoiceClip svc)
			throws IOException, LineUnavailableException {
		
		//check if audio is playing in other thread
		boolean err;
		synchronized (playing) {
			err = playing.get();
		}
		if (err)
			throw new UnsupportedOperationException(
				"Audio from this Shtooka Collection is already"
				+ " currently playing on a separate thread.");
		playing.set(true);
		
		//play flac audio
		/* Code for FLAC decoder is obtained from:
		 * FLAC library (Java)
		 * 
		 * Copyright (c) Project Nayuki
		 * https://www.nayuki.io/page/flac-library-java
		 * */
		
		/*-- Initialization code --*/
		//(custom constructor)
		FlacDecoder decoder = new FlacDecoder(this, svc.offset, svc.size);
		
		// Process header metadata blocks
		while (decoder.readAndHandleMetadataBlock() != null);
		StreamInfo streamInfo = decoder.streamInfo;
		if (streamInfo.numSamples == 0) {
			decoder.close();
			throw new IllegalArgumentException("Unknown audio length");
		}
		
		// Start Java sound output API
		AudioFormat format = new AudioFormat(
			streamInfo.sampleRate, streamInfo.sampleDepth,
			streamInfo.numChannels, true, false);
		Info info = new Info(SourceDataLine.class, format);
		SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
		line.open(format);
		line.start();
			
		/*-- Audio play loop --*/
		/* Decode and write audio data, handle seek requests,
		 * wait for seek when end of stream reached */
		int bytesPerSample = streamInfo.sampleDepth / 8;
			
		/* Buffers for data created and discarded within each
		 * loop iteration, but allocated outside the loop */
		int[][] samples = new int[streamInfo.numChannels][65536];
		byte[] sampleBytes = new byte[
			65536 * streamInfo.numChannels * bytesPerSample];

		while (true) {
				
			// Decode next audio block
			int blockSamples = decoder.readAudioBlock(samples, 0);
				
			/* Convert samples to channel-interleaved bytes
			 * in little endian */
			int sampleBytesLen = 0;
			for (int i = 0; i < blockSamples; i++) {
				for (int ch = 0; ch < streamInfo.numChannels; ch++) {
					int val = samples[ch][i];
					for (int j = 0; j < bytesPerSample; j++, sampleBytesLen++)
						sampleBytes[sampleBytesLen] = (byte)(val >>> (j << 3));
				}
			}
			line.write(sampleBytes, 0, sampleBytesLen);
				
			// End after audio finished playing
			if (line.available() == line.getBufferSize())
				break;
		}
		
		decoder.close();
		
		//
		synchronized (playing) {
			playing.set(false);
		}
	}
}
