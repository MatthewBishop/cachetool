package com.jagex.cache;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.jagex.cache.util.BZip2Decompressor;
import com.jagex.cache.util.Buffer;
import com.jagex.cache.util.JagBZip2OutputStream;

import org.itadaki.bzip2.BZip2InputStream;

/**
 * A container used in the cache, which stores sub-containers, called entries.
 * <p>
 * Archives may be compressed in whole, or have their individual entries compressed. Both types use Bzip2 for
 * compression.
 * 
 * @author Advocatus, Major(naming, some documentation)
 * 
 */
public class Archive {

	/**
	 * The amount of entries in this Archive.
	 */
	private int entries;

	/**
	 * Whether or not this Archive was compressed as a whole: if false, decompression will be performed on each of the
	 * individual entries.
	 */
	private boolean extracted;

	/**
	 * The raw (i.e. decompressed) sizes of each of the entries in this Archive.
	 */
	private int[] extractedSizes;

	/**
	 * The identifiers (i.e. hashed names) of each of the entries in this Archive.
	 */
	private int[] identifiers;
	private int[] indices;

	/**
	 * The buffer containing the decompressed data in this Archive.
	 */
	private byte[] buffer;

	/**
	 * The compressed sizes of each of the entries in this Archive.
	 */
	private int[] sizes;
	
	/**
	 * An array of all the files within this archive.
	 */
	private byte[][] files;
	
	/**
	 * Creates a new {@link Archive} from an array of data.
	 * @param data The array of data.
	 */
	public Archive(byte[] data) {
		Buffer buffer = new Buffer(data);
		int decompressedSize = buffer.readUTriByte();
		int compressedSize = buffer.readUTriByte();
		if (compressedSize != decompressedSize) {
			byte[] output = new byte[decompressedSize];
			//byte[] input = new byte[compressedSize];
			//System.arraycopy(data, 6, input, 0, compressedSize);
			//unbzip2(input, output);
			BZip2Decompressor.decompress(output, decompressedSize, data, compressedSize, 6);
			this.buffer = output;
			buffer = new Buffer(this.buffer);
			this.extracted = true;
		} else {
			this.buffer = data;
			this.extracted = false;
		}
		this.entries = buffer.readUShort();
		identifiers = new int[entries];
		extractedSizes = new int[entries];
		sizes = new int[entries];
		indices = new int[entries];
		files = new byte[entries][];
		int offset = buffer.position + this.entries * 10;
		for (int file = 0; file < this.entries; file++) {
			this.identifiers[file] = buffer.readUInt();
			this.extractedSizes[file] = buffer.readUTriByte();
			this.sizes[file] = buffer.readUTriByte();
			this.indices[file] = offset;
			offset += this.sizes[file];
			this.files[file] = getEntry(file);
		}
	}

	/**
	 * Gets an entry by name within the archive.
	 * @param name The name.
	 * @return The entry.
	 */
	public byte[] getEntry(String name) {
		int identifier = getHash(name);
		for (int file = 0; file < this.entries; file++) {
			if (this.identifiers[file] == identifier)
				return getEntry(file);
		}
		return null;
	}

	/**
	 * Updates the entry data within the archive for a specific name.
	 * @param name The name.
	 * @param data The updated entry data.
	 */
	public void updateEntry(String name, byte[] data) {
		int identifier = getHash(name);	
		for (int file = 0; file < entries; file++) {
			if (identifiers[file] == identifier) {
				files[file] = data;
				break;
			}
		}
	}

	/**
	 * Recompiles the contents of the archive. This needs to be called after updating entry data.
	 * @author Advocatus, derived from Tom's Cache Suite.
	 * @return The recompiled archive. 
	 * @throws IOException if there is an error compressing archive contents with bzip2.
	 */
	public byte[] recompile() throws IOException {
		byte[] compressedWhole = compileUncompressed();
		int compressedWholeDecompressedSize = compressedWhole.length;
		compressedWhole = JagBZip2OutputStream.bz2Compress(compressedWhole);
		int compressedWholeSize = compressedWhole.length;
		byte[] compressedIndividually = compileCompressed();
		int compressedIndividuallySize = compressedIndividually.length;
		boolean compressedAsWhole = false;
		if (compressedWholeSize < compressedIndividuallySize)
			compressedAsWhole = true;
		
		Buffer finalBuf = new Buffer(compressedAsWhole ? (compressedWhole.length + 6) : compressedIndividually.length + 6);
		if (compressedAsWhole) {
			finalBuf.writeTriByte(compressedWholeDecompressedSize);
			finalBuf.writeTriByte(compressedWholeSize);
			finalBuf.writeBytes(compressedWhole);
		} else {
			finalBuf.writeTriByte(compressedIndividuallySize);
			finalBuf.writeTriByte(compressedIndividuallySize);
			finalBuf.writeBytes(compressedIndividually);
		}
		return finalBuf.payload;
	}

	/**
	 * Gets the array of all archive contents.
	 * @return The array of all contents.
	 * @author Advocatus, derived from Tom's Cache Suite.
	 */
	private byte[] compileUncompressed() {
		int fileBufSize = 0;
		
		for (int i = 0; i < this.entries; i++) {
			this.extractedSizes[i] = this.files[i].length;
			this.sizes[i] = this.files[i].length;
			fileBufSize += sizes[i];
		}
		
		Buffer fileBuf = new Buffer(fileBufSize);

		for (int i = 0; i < this.entries; i++) {
			fileBuf.writeBytes(this.files[i]);
		}
		
		byte[] filesSection = fileBuf.payload;

		Buffer fileInfo = new Buffer(2 + (entries *10));
		fileInfo.writeShort(this.entries);
		for (int j = 0; j < this.entries; j++) {
			fileInfo.writeInt(this.identifiers[j]);
			fileInfo.writeTriByte(this.extractedSizes[j]);
			fileInfo.writeTriByte(this.sizes[j]);
		}
		byte[] fileInfoSection = fileInfo.payload;
		
		Buffer finalBuffer = new Buffer(fileInfoSection.length + filesSection.length);
		finalBuffer.writeBytes(fileInfoSection);
		finalBuffer.writeBytes(filesSection);
		return finalBuffer.payload;
	}

	/**
	 * Gets the array of all archive contents compressed.
	 * @return The array of all contents.
	 * @throws IOException if there is an error compressing archive contents with bzip2.
	 */
	private byte[] compileCompressed() throws IOException {
		int fileBufSize = 0;
		byte[][] compresseds = new byte[this.entries][];
		
		for (int i = 0; i < this.entries; i++) {
			this.extractedSizes[i] = this.files[i].length;
			byte[] compressed = JagBZip2OutputStream.bz2Compress(this.files[i]);
			this.sizes[i] = compressed.length;
			compresseds[i] = compressed;
			fileBufSize += sizes[i];
		}
		
		Buffer fileBuf = new Buffer(fileBufSize);
		for (int i = 0; i < this.entries; i++) {
			fileBuf.writeBytes(compresseds[i]);
		}
		byte[] filesSection = fileBuf.payload;	
		
		Buffer fileInfo = new Buffer(2 + (entries *10));
		fileInfo.writeShort(this.entries);
		for (int j = 0; j < this.entries; j++) {
			fileInfo.writeInt(this.identifiers[j]);
			fileInfo.writeTriByte(this.extractedSizes[j]);
			fileInfo.writeTriByte(this.sizes[j]);
		}
		byte[] fileInfoSection = fileInfo.payload;

		Buffer finalBuffer = new Buffer(fileInfoSection.length + filesSection.length);
		finalBuffer.writeBytes(fileInfoSection);
		finalBuffer.writeBytes(filesSection);
		return finalBuffer.payload;
	}

	/**
	 * Gets a entry data at a specific index.
	 * @param index The index.
	 * @return The entry data.
	 */
	private byte[] getEntry(int index) {
		byte[] dataBuffer = new byte[this.extractedSizes[index]];
		if (!this.extracted) {
			BZip2Decompressor.decompress(dataBuffer, this.extractedSizes[index], this.buffer, this.sizes[index], this.indices[index]);

		//	unbzip2(this.buffer, dataBuffer);
		} else {
			System.arraycopy(this.buffer, this.indices[index], dataBuffer, 0, this.extractedSizes[index]);
		}
		return dataBuffer;
	}

	/**
	 * Gets the unique identifier for the archive content.
	 * @param name The name.
	 * @return The identifier.
	 */
	private static int getHash(String name) {
		int identifier = 0;
		name = name.toUpperCase();
		for (int index = 0; index < name.length(); index++)
			identifier = identifier * 61 + name.charAt(index) - 32;
		return identifier;
	}

	/**
	 * Unbzip2s the compressed array and places the result into the uncompressed array.
	 * @param compressed The compressed data
	 * @param uncompressed The uncompressed array.
	 */
	private static void unbzip2(byte[] compressed, byte[] uncompressed) {
		//Jagex uses headerless bzip2. Add 4 byte header.
		byte[] newCompressed = new byte[compressed.length + 4];
		newCompressed[0] = 'B';
		newCompressed[1] = 'Z';
		newCompressed[2] = 'h';
		newCompressed[3] = '1';
		System.arraycopy(compressed, 0, newCompressed, 4, compressed.length);

		DataInputStream is = new DataInputStream(new BZip2InputStream(new ByteArrayInputStream(newCompressed), false));
		try {
			try {
				is.readFully(uncompressed);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
