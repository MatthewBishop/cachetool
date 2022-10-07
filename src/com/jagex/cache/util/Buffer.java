package com.jagex.cache.util;

public class Buffer {

	public int position;
	public byte[] payload;

	public Buffer(int size) {
		this.payload = new byte[size];
		this.position = 0;
	}
	
	public Buffer(byte[] payload) {
		this.payload = payload;
		this.position = 0;
	}

	public int readUTriByte() {
		this.position += 3;
		return ((this.payload[this.position - 3] & 0xFF) << 16) + ((this.payload[this.position - 2] & 0xFF) << 8)
				+ (this.payload[this.position - 1] & 0xFF);
	}

	public int readUInt() {
		this.position += 4;
		return ((this.payload[this.position - 4] & 0xFF) << 24) + ((this.payload[this.position - 3] & 0xFF) << 16)
				+ ((this.payload[this.position - 2] & 0xFF) << 8) + (this.payload[this.position - 1] & 0xFF);
	}

	public int readUShort() {
		this.position += 2;
		return ((this.payload[this.position - 2] & 0xFF) << 8) + (this.payload[this.position - 1] & 0xFF);
	}

	public void writeInt(int i) {
		payload[position++] = (byte) (i >> 24);
		payload[position++] = (byte) (i >> 16);
		payload[position++] = (byte) (i >> 8);
		payload[position++] = (byte) i;
	}
	
	public void writeTriByte(int i) {
		payload[position++] = (byte) (i >> 16);
		payload[position++] = (byte) (i >> 8);
		payload[position++] = (byte) i;
	}
	
	public void writeShort(int i) {
		payload[position++] = (byte) (i >> 8);
		payload[position++] = (byte) i;
	}
	
	public void writeBytes(byte[] data) {
		writeBytes(data, 0, data.length);
	}
	
	public void writeBytes(byte[] data, int offset, int length) {
		for (int i = offset; i < offset + length; i++) {
			payload[position++] = data[i];
		}
	}

	public int readUByte() {
		return payload[position++] & 0xff;
	}
}
