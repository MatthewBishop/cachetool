package com.jagex.cache.tools;

import com.jagex.cache.Index;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

/**
 * A cache represents a collection of files used by Jagex in Runescape.
 * <p>
 * 
 * @author Advocatus
 *
 */
public class TarnishCache {

	public static void main(String[] args) throws Exception {
		TarnishCache cache = new TarnishCache("./dest/");

		cache.clean("./clean/");
	}

	private Index[] indices = new Index[6];

	/**
	 * Loads a cache from a target directory.
	 * @param directory The directory.
	 * @throws FileNotFoundException Unable to access the directory.
	 */
	public TarnishCache(String directory) throws FileNotFoundException {
		RandomAccessFile cache = new RandomAccessFile(directory  + "main_file_cache.dat", "rw");
		RandomAccessFile[] indexes = new RandomAccessFile[6];
		for (int index = 0; index < 6; index++) {
			indexes[index] = new RandomAccessFile(directory + "main_file_cache.idx" + index, "rw");
		}

		for (int index = 0; index < 6; index++) {
			indices[index] = new Index(indexes[index], cache, index + 1, 0xffffff);
		}
	}

	/**
	 * Does a clean rebuild of the cache.
	 * @param directory The directory of the cleaned cache.
	 * @throws FileNotFoundException Unable to access the directory.
	 */
	public void clean(String directory) throws FileNotFoundException {
		RandomAccessFile cache = new RandomAccessFile(directory  + "main_file_cache.dat", "rw");
		RandomAccessFile[] indexes = new RandomAccessFile[6];
		for (int index = 0; index < 6; index++) {
			indexes[index] = new RandomAccessFile(directory + "main_file_cache.idx" + index, "rw");
		}

		Index[] indices = new Index[6];
		for (int index = 0; index < 6; index++) {
			indices[index] = new Index(indexes[index], cache, index + 1, 0xffffff);
		}

		int[] toPack = { 0 };
		for(int index : toPack) {
			for(int i = 0; i < this.indices[index].getFileCount(); i++) {
				byte[] orig = this.indices[index].decompress(i);
				indices[index].put(orig, i, orig.length);
			}
		}
	}
}
