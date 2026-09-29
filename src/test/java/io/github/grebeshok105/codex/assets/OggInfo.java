package io.github.grebeshok105.codex.assets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal Ogg Vorbis duration reader for the asset contract test. The sample rate is the
 * little-endian int at offset 12 of the Vorbis identification packet ({@code \x01vorbis});
 * the duration is the last Ogg page's granule_position divided by that rate.
 */
public final class OggInfo {
	private static final byte[] OGG_CAPTURE = {'O', 'g', 'g', 'S'};
	private static final byte[] VORBIS_ID = {0x01, 'v', 'o', 'r', 'b', 'i', 's'};

	private OggInfo() {}

	public static long durationMs(Path ogg) throws IOException {
		byte[] data = Files.readAllBytes(ogg);
		int idAt = indexOf(data, VORBIS_ID, 0);
		if (idAt < 0) {
			throw new IOException(ogg + " has no Vorbis identification packet");
		}
		long rate = littleEndianInt(data, idAt + 12);
		if (rate <= 0) {
			throw new IOException(ogg + " has non-positive Vorbis sample rate " + rate);
		}
		int lastPage = lastIndexOf(data, OGG_CAPTURE);
		if (lastPage < 0) {
			throw new IOException(ogg + " has no Ogg pages");
		}
		long granule = littleEndianLong(data, lastPage + 6);
		return granule * 1000 / rate;
	}

	private static int indexOf(byte[] data, byte[] needle, int from) {
		outer: for (int i = Math.max(from, 0); i + needle.length <= data.length; i++) {
			for (int j = 0; j < needle.length; j++) {
				if (data[i + j] != needle[j]) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}

	private static int lastIndexOf(byte[] data, byte[] needle) {
		for (int i = data.length - needle.length; i >= 0; i--) {
			boolean found = true;
			for (int j = 0; j < needle.length; j++) {
				if (data[i + j] != needle[j]) {
					found = false;
					break;
				}
			}
			if (found) {
				return i;
			}
		}
		return -1;
	}

	private static long littleEndianInt(byte[] data, int offset) {
		return (data[offset] & 0xFFL)
				| ((data[offset + 1] & 0xFFL) << 8)
				| ((data[offset + 2] & 0xFFL) << 16)
				| ((data[offset + 3] & 0xFFL) << 24);
	}

	private static long littleEndianLong(byte[] data, int offset) {
		long value = 0;
		for (int i = 7; i >= 0; i--) {
			value = (value << 8) | (data[offset + i] & 0xFFL);
		}
		return value;
	}
}
