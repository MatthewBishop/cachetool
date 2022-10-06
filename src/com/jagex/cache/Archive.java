package com.jagex.cache;

import java.io.IOException;

import com.jagex.cache.util.BZip2Decompressor;
import com.jagex.cache.util.Buffer;
import com.jagex.cache.util.JagBZip2OutputStream;

public class Archive {

	byte[] buffer;

	private int entries;
	
	private int[] identifiers;

	private int[] extractedSizes;

	private int[] sizes;

	private int[] indices;

	boolean extracted;

	private byte[][] files;
	
	public Archive(byte[] data) {
		Buffer buffer = new Buffer(data);
		int decompressedSize = buffer.readUTriByte();
		int compressedSize = buffer.readUTriByte();
		if (compressedSize != decompressedSize) {
			byte[] output = new byte[decompressedSize];
			BZip2Decompressor.decompress(output, decompressedSize, data, compressedSize, 6);
			// TODO unbzip2(data, output); WIP switching bzip decompressor
			
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
			this.files[file] = getFileAt(file);
		}
	}

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

	private byte[] compileUncompressed() throws IOException {
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

	public byte[] getFileAt(int at) {
		byte[] dataBuffer = new byte[this.extractedSizes[at]];
		if (!this.extracted) {
			BZip2Decompressor.decompress(dataBuffer, this.extractedSizes[at], this.buffer, this.sizes[at], this.indices[at]);
			// TODO unbzip2(this.buffer, dataBuffer); WIP switching bzip decompressor
		} else {
			System.arraycopy(this.buffer, this.indices[at], dataBuffer, 0, this.extractedSizes[at]);
		}
		return dataBuffer;
	}

	public byte[] getFile(int identifier) {
		for (int k = 0; k < this.entries; k++) {
			if (this.identifiers[k] == identifier)
				return getFileAt(k);
		}
		return null;
	}

	public int getIdentifierAt(int at) {
		return this.identifiers[at];
	}

	public int getDecompressedSize(int at) {
		return this.extractedSizes[at];
	}

	public int getTotalFiles() {
		return this.entries;
	}

	public byte[] getEntry(String identStr) {
		int identifier = 0;
		identStr = identStr.toUpperCase();
		for (int j = 0; j < identStr.length(); j++)
			identifier = identifier * 61 + identStr.charAt(j) - 32;
		return getFile(identifier);
	}

	public static int getHash(String s) {
		int identifier = 0;
		s = s.toUpperCase();
		for (int j = 0; j < s.length(); j++)
			identifier = identifier * 61 + s.charAt(j) - 32;
		return identifier;
	}

	public void renameFile(int index, int newName) {
		this.identifiers[index] = newName;
	}

	public void updateFile(int index, byte[] data) {
		files[index] = data;
	}

	public void updateFile(String identStr, byte[] data) {
		files[indexOf(identStr)] = data;
	}
	
	public int indexOf(String name) {
		return indexOf(getHash(name));
	}

	public int indexOf(int hash) {
		for (int i = 0; i < entries; i++) {
			if (identifiers[i] == hash) {
				return i;
			}
		}
		return -1;
	}

}
