/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2017 Spooky Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.spookygames.gdx.sfx.desktop;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import net.spookygames.gdx.sfx.MusicDurationResolver;
import net.spookygames.gdx.sfx.SfxMusicLoader;
import net.spookygames.gdx.sfx.SfxSoundLoader;
import net.spookygames.gdx.sfx.SoundDurationResolver;

public class DesktopAudioDurationResolver implements MusicDurationResolver, SoundDurationResolver {

	@Override
	public float resolveMusicDuration(Music music, FileHandle musicFile) {
		return resolveFileDuration(musicFile);
	}

	@Override
	public float resolveSoundDuration(Sound sound, FileHandle soundFile) {
		return resolveFileDuration(soundFile);
	}

	private float resolveFileDuration(FileHandle file) {
		String ext = file.extension().toLowerCase();
		try {
			switch (ext) {
				case "wav":
					return wavDuration(file);
				case "mp3":
					return mp3Duration(file);
				case "ogg":
					return oggDuration(file);
				default:
					Gdx.app.error("gdx-sfx", "Unsupported audio format for duration: " + ext);
					return -1f;
			}
		} catch (Exception e) {
			Gdx.app.error("gdx-sfx", "Failed to get duration for " + file.toString(), e);
			return -1f;
		}
	}

	private float wavDuration(FileHandle file) throws UnsupportedAudioFileException, IOException {
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file.file());
		AudioFormat format = audioInputStream.getFormat();
		long frames = audioInputStream.getFrameLength();
		audioInputStream.close();
		return (float) frames / format.getFrameRate();
	}

	private float mp3Duration(FileHandle file) throws BitstreamException {
		Bitstream bitstream = new Bitstream(file.read());
		int length = (int) file.length();
		int streamPos = bitstream.header_pos();
		Header header = bitstream.readFrame();
		if (header == null) return -1f;
		if ((streamPos > 0) && (length != AudioSystem.NOT_SPECIFIED) && (streamPos < length))
			length -= streamPos;
		float totalMilliseconds = header.total_ms(length);
		return totalMilliseconds / 1000f;
	}

	private float oggDuration(FileHandle file) throws JOrbisException {
		String path = null;
		File javaFile = file.file();
		FileHandle tmpFile = null;

		if (javaFile.exists()) {
			path = javaFile.getAbsolutePath();
		} else {
			tmpFile = FileHandle.tempFile("gdx-sfx.ogg.");
			file.copyTo(tmpFile);
			path = tmpFile.file().getAbsolutePath();
		}

		VorbisFile vorbis = new VorbisFile(path);
		float durationInSeconds = vorbis.time_total(-1);

		if (tmpFile != null)
			tmpFile.delete();

		return durationInSeconds;
	}

	public static void initialize() {
		DesktopAudioDurationResolver resolver = new DesktopAudioDurationResolver();
		SfxMusicLoader.setDurationResolver(resolver);
		SfxSoundLoader.setDurationResolver(resolver);
	}
}
