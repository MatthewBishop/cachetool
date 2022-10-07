package com.jagex.cache;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;

import com.jagex.cache.util.Buffer;

/**
 * A cache represents a collection of files used by Jagex in Runescape.
 * <p>
 * 
 * @author Advocatus
 *
 */
public class Cache {
		
	public Index[] indices = new Index[5];
	private int[][] crcs = new int[4][];
	private int[][] versions = new int[4][];
	private CRC32 crc32 = new CRC32();
	
	/**
	 * Loads a cache from a target directory.
	 * @param directory The directory.
	 * @throws FileNotFoundException Unable to access the directory.
	 */
	public Cache(String directory) throws FileNotFoundException {
		RandomAccessFile cache = new RandomAccessFile(directory  + "main_file_cache.dat", "rw");
		RandomAccessFile[] indexes = new RandomAccessFile[5];
		for (int index = 0; index < 5; index++) {
			indexes[index] = new RandomAccessFile(directory + "main_file_cache.idx" + index, "rw");
		}

		for (int index = 0; index < 5; index++) {
			indices[index] = new Index(indexes[index], cache, index + 1, 0xffffff);
		}
		
		Archive versionlist = new Archive(indices[0].decompress(5));

		for (int type = 0; type < 4; type++) {
			byte[] data = versionlist.getEntry(VERSION_NAMES[type]);
			int count = data.length / 2;
			Buffer buffer = new Buffer(data);
			this.versions[type] = new int[count];

			for (int file = 0; file < count; file++) {
				this.versions[type][file] = buffer.readUShort();
			}
		}

		for (int type = 0; type < 4; type++) {
			byte[] data = versionlist.getEntry(CRC_NAMES[type]);
			int count = data.length / 4;
			Buffer buffer = new Buffer(data);
			this.crcs[type] = new int[count];

			for (int file = 0; file < count; file++) {
				this.crcs[type][file] = buffer.readUInt();
			}
		}
	}

	/**
	 * Iterates through all files in the cache and prints out the missing files.
	 */
	public void check() {
		int missing = 0;
		for (int type = 0; type < 4; type++) {
			int idx = type + 1;
			for (int m = 0; m < this.crcs[type].length; m++) {
				if (this.versions[type][m] == 0)
					continue;
				byte[] dat = this.indices[idx].decompress(m);
				if (!exists(dat)) {
					System.out.println("Missing " + TYPE_NAMES[idx] + ": " + m);
					missing++;
				}
			}
		}
		System.out.println("Total missing: " + missing);
	}
	
	/**
	 * Does a clean rebuild of the cache.
	 * @param directory The directory of the cleaned cache.
	 * @throws FileNotFoundException Unable to access the directory.
	 */
	public void clean(String directory) throws FileNotFoundException {
		RandomAccessFile cache = new RandomAccessFile(directory  + "main_file_cache.dat", "rw");
		RandomAccessFile[] indexes = new RandomAccessFile[5];
		for (int index = 0; index < 5; index++) {
			indexes[index] = new RandomAccessFile(directory + "main_file_cache.idx" + index, "rw");
		}

		Index[] indices = new Index[5];
		for (int index = 0; index < 5; index++) {
			indices[index] = new Index(indexes[index], cache, index + 1, 0xffffff);
		}
		
		for(int i = 1; i < 9; i++) {
			byte[] orig = this.indices[0].decompress(i);
			System.out.println(i);
			indices[0].put(orig, i, orig.length);
		}

		for (int type = 0; type < 4; type++) {
			int idx = type + 1;
			for (int m = 0; m < this.crcs[type].length; m++) {
				if (this.versions[type][m] == 0)
					continue;
				byte[] dat = this.indices[idx].decompress(m);
				if (exists(dat)) {
					indices[idx].put(dat, m, dat.length);
				}
			}
		}
			
	}
	
	/**
	 * Updates this cache with the contents of another. Only missing files are added. This also updates the internal crc and version information.
	 * <p>
	 * rebuild() must be called to save the changes.
	 * @param source The cache that is the source of the additional files.
	 */
	public void update(Cache source) {
		int added = 0;
		for (int type = 0; type < 4; type++) {
			int idx = type + 1;
			for (int m = 0; m < this.crcs[type].length; m++) {
				if (this.versions[type][m] == 0)
					continue;
				byte[] dat = this.indices[idx].decompress(m);
				if (!exists(dat)) {
					byte[] datnew = source.indices[idx].decompress(m);
					if (exists(datnew)) {
						this.crcs[type][m] = source.crcs[type][m];
						this.indices[idx].put(datnew, m, datnew.length);
						this.crcs[type][m] = source.crcs[type][m];
						this.versions[type][m] = source.versions[type][m];
						System.out.println("Added " + TYPE_NAMES[idx] + ": " + m);
						added++;
					}
				}
			}
		}
		System.out.println("Total added: " + added);
	}
	
	/**
	 * Rebuilds the version list information for the Cache.
	 */
	public void rebuild() {
		rebuild(false);
	}
	
	/**
	 * Rebuilds the version list information for the Cache.
	 * @param full The type of rebuild. If true, the version and crc values will recalculated from the cache contents.
	 */
	public void rebuild(boolean full) {
		byte[] orig = indices[0].decompress(5);

		Archive archive = new Archive(orig);
		
		for (int type = 0; type < 4; type++) {
			
			int index = type + 1;
			
			int size = this.versions[type].length;
			
			Buffer crc_buffer = new Buffer(size * 4);
			Buffer version_buffer = new Buffer(size * 2);
			
			if(full) {
				for (int file = 0; file < size; file++) {
					if(versions[type][file] == 0) {
						crc_buffer.writeInt(crcs[type][file]);
						version_buffer.writeShort(versions[type][file]);
						continue;
					}
					byte data[] = indices[index].decompress(file);
					if(data == null) {
						crc_buffer.writeInt(crcs[type][file]);
						version_buffer.writeShort(versions[type][file]);
						continue;
					}
					int caret = data.length - 2;
					int version = ((data[caret] & 0xff) << 8) + (data[caret + 1] & 0xff);
					crc32.reset();
			        crc32.update(data, 0, caret);
					int crc = (int) crc32.getValue();
					crc_buffer.writeInt(crc);
					version_buffer.writeShort(version);
				}
			} else {
				for (int file = 0; file < this.versions[type].length; file++) {
					crc_buffer.writeInt(crcs[type][file]);
					version_buffer.writeShort(versions[type][file]);		
				}
			}
			
			archive.updateEntry(VERSION_NAMES[type], version_buffer.payload);
			archive.updateEntry(CRC_NAMES[type], crc_buffer.payload);
		}
		
		try {
			byte[] rebuilt = archive.recompile();
			System.out.println("Updated version list - size was ("+orig.length+") and now is ("+rebuilt.length+")");
			indices[0].put(rebuilt, 5, rebuilt.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String[] TYPE_NAMES = new String[] { "archive", "model", "anim", "midi", "map" };
	private static String[] VERSION_NAMES = { "model_version", "anim_version", "midi_version", "map_version" };
	private static String[] CRC_NAMES = { "model_crc", "anim_crc", "midi_crc", "map_crc" };
		
	private static boolean exists(byte[] dat) {
		if (dat == null) {
			return false;
		} else if (dat.length == 0) {
			return false;
		}
		return true;
	}
}
