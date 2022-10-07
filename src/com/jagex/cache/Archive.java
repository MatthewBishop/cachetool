package com.jagex.cache;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.jagex.cache.util.Buffer;
import com.jagex.cache.util.JagBZip2OutputStream;

import org.itadaki.bzip2.BZip2InputStream;

public class Archive {

	private byte[] buffer;

	private int entries;
	
	private int[] identifiers;

	private int[] extractedSizes;

	private int[] sizes;

	private int[] indices;

	private boolean extracted;

	private byte[][] files;
	
	public Archive(byte[] data) {
		Buffer buffer = new Buffer(data);
		int decompressedSize = buffer.readUTriByte();
		int compressedSize = buffer.readUTriByte();
		if (compressedSize != decompressedSize) {
			byte[] output = new byte[decompressedSize];
			byte[] input = new byte[compressedSize];
			System.arraycopy(data, 6, input, 0, compressedSize);
			unbzip2(input, output);
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

	private byte[] getFileAt(int index) {
		byte[] dataBuffer = new byte[this.extractedSizes[index]];
		if (!this.extracted) {
			unbzip2(this.buffer, dataBuffer);
		} else {
			System.arraycopy(this.buffer, this.indices[index], dataBuffer, 0, this.extractedSizes[index]);
		}
		return dataBuffer;
	}

	public byte[] getEntry(String name) {
		int identifier = getHash(name);
		for (int file = 0; file < this.entries; file++) {
			if (this.identifiers[file] == identifier)
				return getFileAt(file);
		}
		return null;
	}

	private static int getHash(String name) {
		int identifier = 0;
		name = name.toUpperCase();
		for (int index = 0; index < name.length(); index++)
			identifier = identifier * 61 + name.charAt(index) - 32;
		return identifier;
	}

	public void updateFile(String name, byte[] data) {
		int identifier = getHash(name);	
		for (int file = 0; file < entries; file++) {
			if (identifiers[file] == identifier) {
				files[file] = data;
				break;
			}
		}
	}

	//Unbzip2s the compressed array and places the result into the uncompressed array.
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
