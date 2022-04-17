package sahlaysta.shtooka;

import java.io.IOException;

/**
 * A single playable Shtooka voice clip
 * from a Shtooka Collection
 * of voice clips.
 * 
 * <p>If the owner Shtooka Collection of this
 * voice clip is closed, this voice clip will
 * fail to play.
 * 
 * @author sahlaysta
 * @see ShtookaCollection
 * */
public class ShtookaVoiceClip {
	
	/** The owner Shtooka Collection of this voice clip.
	 * @see ShtookaCollection */
	public final ShtookaCollection owner;
	
	/** The .tar entry filename of this voice clip. */
	public final String filename;
	
	/** The .tar entry file offset of this voice clip. */
	public final long offset;
	
	/** The .tar entry file size of this voice clip. */
	public final long size;
	
	/** The tag names of this Shtooka Voice Clip. Usually,
	 * the tag name of a voice clip is what the speaker
	 * pronounces in the voice clip. Most have only one.
	 * If this voice clip has no names, then this array
	 * is empty. */
	public final String[] names;
	
	/** Constructs a Shtooka Voice Clip defining each field.
	 * @param owner the Shtooka Collection owner of the voice clip
	 * @param filename the .tar entry filename
	 * @param offset the .tar entry file offset
	 * @param size the .tar entry file size
	 * @param names the names of the voice clips
	 * @see #owner
	 * @see #filename
	 * @see #offset
	 * @see #size
	 * @see #names
	 * */
	public ShtookaVoiceClip(
			ShtookaCollection owner,
			String filename,
			long offset,
			long size,
			String[] names) {
		this.owner = owner;
		this.filename = filename;
		this.offset = offset;
		this.size = size;
		this.names = names;
	}
	
	/** Plays the audio of this Shtooka Voice Clip to the
	 * system audio output.
	 * @throws UnsupportedOperationException if the owner
	 * Shtooka Collection of this Shtooka Voice Clip has
	 * been closed, or if audio from the owner
	 * Shtooka Collection is already currently playing on
	 * a separate thread
	 * @throws IOException if an I/O error occurs
	 * @throws RuntimeException if an error occurs
	 * decoding FLAC audio
	 * @see ShtookaCollection#playVoiceClip(ShtookaVoiceClip)
	 * */
	public void play() throws IOException {
		owner.playVoiceClip(this);
	}

}
