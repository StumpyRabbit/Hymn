/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.common.reedsolomon;

import java.util.Arrays;

import club.callistohouse.utils.IntUtil;


public class GaloisField {

	public static byte[] intsToBytes(int[] ints) {
		byte[] byteArray = new byte[ints.length];
		for(int i = 0; i < ints.length; i++)
			byteArray[i] = (byte) (ints[i] & 0xFF);
		return byteArray;
	}
	public static int[] bytesToInts(byte[] bytes) {
		int[] intArray = new int[bytes.length];
		for(int i = 0; i < bytes.length; i++)
			intArray[i] = ((int)bytes[i]) & 0xff;
		return intArray;
	}

	private GenericGF galoisFieldImpl;
	private int numberOfCodeSymbols;
	private int numberOfDataSymbols;
	private int ecSymbols;
	private int symbolSizeInBits;

	public GaloisField(GenericGF galoisFieldImpl, int codeSymbols, int dataSymbols, int symbolSizeInBits) {
		this.galoisFieldImpl = galoisFieldImpl;
		this.numberOfCodeSymbols = codeSymbols;
		this.numberOfDataSymbols = dataSymbols;
		this.ecSymbols = codeSymbols - dataSymbols;
		this.symbolSizeInBits = symbolSizeInBits;
	}

	public GenericGF getFieldImpl() { return galoisFieldImpl; }
	public ReedSolomonEncoder getEncoder() { return new ReedSolomonEncoder(this); }
	public ReedSolomonDecoder getDecoder() { return new ReedSolomonDecoder(this); }

	public String toString() {
		return "GaloisField(" + galoisFieldImpl.getSize() + ", " 
				+ getNumberOfChunkCodeSymbols() + "/" + getNumberOfChunkDataSymbols()
				+ " <" + getSymbolSizeInBits() +" bitsPerSymbol>)";
	}

	public int getNumberOfChunkCodeSymbols() { return numberOfCodeSymbols; }
	public int getNumberOfChunkDataSymbols() { return numberOfDataSymbols; }
	public int getNumberOfBlockCodeBytes() { return (int) (numberOfCodeSymbols * 4 / getSymbolsPerByte()); }
	public int getNumberOfBlockDataBytes() { return (int) (numberOfDataSymbols * 4 / getSymbolsPerByte()); }
	public int getNumberOfBlockCodeSymbols() { return numberOfCodeSymbols * 4; }
	public int getNumberOfBlockDataSymbols() { return numberOfDataSymbols * 4; }
	public int getECSymbols() { return ecSymbols; }
	public int getSymbolSizeInBits() { return symbolSizeInBits; }
	public int getSymbolsPerByte() { return 8 / symbolSizeInBits; }

	/*
	 * count blocks
	 * deinterleave total encoded bytes
	 * verify length
	 * build total unencoded bytes array
	 * for each block
	 * 		copy encoded block from deinterleaved encoded bytes
	 * 		decode encoded block to data block
		 * 		copy data block to padded data bytes
	 * unpadding data
	 */
	public byte[] decode(byte[] bytes) throws ReedSolomonException {
		return intsToBytes(decode(bytesToInts(bytes)));
	}
	public int[] decode(int[] intData) throws ReedSolomonException {
		// count blocks
		int blockCount = countOfCodeByteBlocks(intData.length);

		// de-interleave total encoded bytes
		int[] unencodedSymbols = convertToSymbols(intData);
		int[] deinterleaved = deinterleave(unencodedSymbols, blockCount);
		int[] deinterleavedBytes = convertFromSymbols(deinterleaved);

		// verify length
		if(blockCount * getNumberOfBlockCodeBytes() != deinterleavedBytes.length) {
			throw new IllegalArgumentException("deinterleavedBytes length is wrong - " 
					+ " blockCount: " + blockCount 
					+ " blockCodeBytesPerBlock: " + getNumberOfBlockCodeBytes() 
					+ " length: " + deinterleavedBytes.length);
		}
		// build total unencoded bytes array
		int[] totalUnencodedBytes = new int[blockCount * getNumberOfBlockDataBytes()];

		// loop through the blocks and decode them into the totalUnencodedBytes array
		for(int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
			// copy encoded block from deinterleaved encoded bytes
			int[] currentEncodedBlock = Arrays.copyOfRange(
					deinterleavedBytes, 
					blockIndex*getNumberOfBlockCodeBytes(), 
					(blockIndex + 1) * getNumberOfBlockCodeBytes());
			// decode encoded block to data block
			int[] decodedBlock = decodeBlock(currentEncodedBlock);
			// copy data block to padded data bytes
			System.arraycopy(
					decodedBlock, 
					0, 
					totalUnencodedBytes, 
					blockIndex * getNumberOfBlockDataBytes(), 
					getNumberOfBlockDataBytes());
		}
		// unpad bytes array
		return unpadBytes(totalUnencodedBytes);
	}

	protected int[] decodeBlock(int[] encodedBytesBlock) throws ReedSolomonException {

		if(encodedBytesBlock.length != getNumberOfBlockCodeBytes())
			throw new IllegalArgumentException("bad encodedBlock length");
		int[] toBeProcessedBlock = convertToSymbols(encodedBytesBlock);
		int[] decodedBlock = new int[getNumberOfBlockDataSymbols()];
		int[] encodedChunk = new int[getNumberOfChunkCodeSymbols()];
		int[] decodedChunk;

		for(int chunkIndex = 0; chunkIndex < 4; chunkIndex++) {
			int from = chunkIndex*getNumberOfChunkCodeSymbols();
			int to = (chunkIndex + 1)*getNumberOfChunkCodeSymbols();
			int[] chunk = Arrays.copyOfRange(toBeProcessedBlock, from, to);

			System.arraycopy(chunk, 0, encodedChunk, 0, getNumberOfChunkCodeSymbols());
			decodedChunk = getDecoder().decode(encodedChunk);
			for(int i = 0; i < decodedChunk.length; i++) {
				decodedChunk[i] = (int)( decodedChunk[i]) & 0xFF;
			}
			System.arraycopy(
					decodedChunk, 
					0, 
					decodedBlock, 
					chunkIndex * getNumberOfChunkDataSymbols(), 
					getNumberOfChunkDataSymbols());
		}
		return convertFromSymbols(decodedBlock);
	}

	/*
	 * padding
	 * count blocks
	 * size of padding check
	 * build total encoded bytes array
	 * for each block
	 * 		copy data block from padded
	 * 		encode data block
	 * 		copy encoded block to total encoded bytes
	 * interleave total encoded bytes
	 */
	public byte[] encode(byte[] bytes) throws ReedSolomonException {
		return intsToBytes(encode(bytesToInts(bytes)));
	}
	public int[] encode(int[] bytes) {
		// pad the bytes to align to block boundary
		int[] paddedBytes = padBytes(bytes, getNumberOfBlockDataBytes());
		// count blocks
		int blockCount = countOfDataByteBlocks(paddedBytes.length);
		// verify length
		if(paddedBytes.length != blockCount * getNumberOfBlockDataBytes())
			throw new IllegalArgumentException("paddedBytes length is wrong");
		// create an encoded byte array
		int[] totalEncodedBytes = new int[blockCount * getNumberOfBlockCodeBytes()];
		int[] encodedBlock;
		// loop through the blocks and encode them into the totalEncodedBytes array
		for(int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
			// copy bytes to currentUnencodedBlock
			int[] currentUnencodedBlock = Arrays.copyOfRange(
					paddedBytes, 
					blockIndex*getNumberOfBlockDataBytes(), 
					(blockIndex + 1) * getNumberOfBlockDataBytes());
			// encode to encodedBlock
			encodedBlock = encodeBlock(currentUnencodedBlock);
			// copy bytes to totalEncodedBytes
			System.arraycopy(
					encodedBlock, 
					0, 
					totalEncodedBytes, 
					blockIndex * getNumberOfBlockCodeBytes(), 
					getNumberOfBlockCodeBytes());
		}
		// interleave blocks
		int[] encodedSymbols = convertToSymbols(totalEncodedBytes);
		int[] interleaved = interleave(encodedSymbols, blockCount);
		return convertFromSymbols(interleaved);
		// no interleaving
//		return totalEncodedBytes;
	}

	/*
	 * Steps to encode a block
	 * 		1. convert bytes to nibble ints
	 * 		2. for each chunk encode with RS Encoder
	 * 		3. convert nibbles back to bytes
	 */
	protected int[] encodeBlock(int[] unencodedBytesBlock) {

		if(unencodedBytesBlock.length != getNumberOfBlockDataBytes())
			throw new IllegalArgumentException("bad unencodedBlock length");
		int[] toBeProcessedBlock = convertToSymbols(unencodedBytesBlock);
		int[] encodedBlock = new int[getNumberOfBlockCodeSymbols()];
		int[] encodedChunk;
		int[] decodedChunk = new int[getNumberOfChunkDataSymbols()];
		for(int chunkIndex = 0; chunkIndex < 4; chunkIndex++) {
			int from = chunkIndex*getNumberOfChunkDataSymbols();
			int to = (chunkIndex + 1)*getNumberOfChunkDataSymbols();
			int[] chunk = Arrays.copyOfRange(toBeProcessedBlock, from, to);

			System.arraycopy(chunk, 0, decodedChunk, 0, getNumberOfChunkDataSymbols());
			encodedChunk = getEncoder().encode(decodedChunk);
			System.arraycopy(
					encodedChunk, 
					0, 
					encodedBlock, 
					chunkIndex * getNumberOfChunkCodeSymbols(), 
					getNumberOfChunkCodeSymbols());
		}
		return convertFromSymbols(encodedBlock);
	}

	public int[] interleave(int[] toBeInterleaved, int blockCount) {
		int bytesPerBlock = toBeInterleaved.length / 4 / blockCount;
		int[][] table = new int[4][bytesPerBlock];
		int[] interleavedBlock = new int[toBeInterleaved.length / blockCount];
		int[] interleaved = new int[toBeInterleaved.length];

		int sourceIndex, targetIndex;
		for(int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
			int[] encodedBlock = Arrays.copyOfRange(
					toBeInterleaved, 
					blockIndex * getNumberOfBlockCodeSymbols(), 
					(blockIndex + 1) * getNumberOfBlockCodeSymbols());
			for(int chunk = 0; chunk < 4; chunk++) {
				for(int index = 0; index < bytesPerBlock; index++) {
					try {
						sourceIndex = index+(bytesPerBlock*chunk);
						table[chunk][index] = encodedBlock[sourceIndex];
					} catch(ArrayIndexOutOfBoundsException e) {}
				}
			}
			for(int index = 0; index < bytesPerBlock; index++) {
				for(int chunk = 0; chunk < 4; chunk++) {
					targetIndex = index+(bytesPerBlock*chunk);
//					targetIndex = chunk + index * 4;
					interleavedBlock[targetIndex] = table[chunk][index];
				}
			}
			System.arraycopy(
					interleavedBlock, 
					0, 
					interleaved, 
					toBeInterleaved.length / blockCount * blockIndex, 
					interleavedBlock.length);
		}
		return interleaved;
	}

	public int[] deinterleave(int[] toBeDeinterleaved, int blockCount) {
		int bytesPerBlock = toBeDeinterleaved.length / 4 / blockCount;
		int[][] table = new int[4][bytesPerBlock];
		int[] deinterleavedBlock = new int[toBeDeinterleaved.length / blockCount];
		int[] deinterleaved = new int[toBeDeinterleaved.length];

		int sourceIndex, targetIndex;
		for(int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
			int[] encodedBlock = Arrays.copyOfRange(
					toBeDeinterleaved, 
					blockIndex * getNumberOfBlockCodeSymbols(), 
					(blockIndex + 1) * getNumberOfBlockCodeSymbols());
			for(int index = 0; index < bytesPerBlock; index++) {
				for(int chunk = 0; chunk < 4; chunk++) {
//					sourceIndex = chunk + index * 4;
					sourceIndex = index+(bytesPerBlock*chunk);
					table[chunk][index] = encodedBlock[sourceIndex];
				}
			}
			for(int chunk = 0; chunk < 4; chunk++) {
				for(int index = 0; index < bytesPerBlock; index++) {
					targetIndex = index+(bytesPerBlock*chunk);
					deinterleavedBlock[targetIndex] = table[chunk][index];
				}
			}
			System.arraycopy(
					deinterleavedBlock, 
					0, 
					deinterleaved, 
					toBeDeinterleaved.length / blockCount * blockIndex, 
					deinterleavedBlock.length);
		}
		return deinterleaved;
	}

	public int[] convertToSymbols(int[] intData) throws IllegalArgumentException {
		int[] toBeProcessedBlock;
		int symbolSize = getSymbolSizeInBits();
		if(symbolSize == 4)
			toBeProcessedBlock = bytesToNibbles(intData);
		else if (symbolSize == 8) {
			toBeProcessedBlock = intData;
		} else {
			throw new IllegalArgumentException("wrong symbol size: " + symbolSize);
		}
		return toBeProcessedBlock;
	}

	public int[] convertFromSymbols(int[] deinterleaved) throws IllegalArgumentException {
		int[] toBeProcessedBlock;
		int symbolSize = getSymbolSizeInBits();
		if(symbolSize == 4)
			toBeProcessedBlock = nibblesToBytes(deinterleaved);
		else if (symbolSize == 8) {
			toBeProcessedBlock = deinterleaved;
		} else {
			throw new IllegalArgumentException("wrong symbol size: " + symbolSize);
		}
		return toBeProcessedBlock;
	}

	public byte[] convertToSymbols(byte[] byteData) throws IllegalArgumentException {
		byte[] toBeProcessedBlock;
		int symbolSize = getSymbolSizeInBits();
		if(symbolSize == 4)
			toBeProcessedBlock = bytesToNibbles(byteData);
		else if (symbolSize == 8) {
			toBeProcessedBlock = byteData;
		} else {
			throw new IllegalArgumentException("wrong symbol size: " + symbolSize);
		}
		return toBeProcessedBlock;
	}

	public byte[] convertFromSymbols(byte[] deinterleaved) throws IllegalArgumentException {
		byte[] toBeProcessedBlock;
		int symbolSize = getSymbolSizeInBits();
		if(symbolSize == 4)
			toBeProcessedBlock = nibblesToBytes(deinterleaved);
		else if (symbolSize == 8) {
			toBeProcessedBlock = deinterleaved;
		} else {
			throw new IllegalArgumentException("wrong symbol size: " + symbolSize);
		}
		return toBeProcessedBlock;
	}

	public int[] bytesToNibbles(int[] intData) {
		int[] nibbles = new int[intData.length * 2];
		for(int byteIndex = 0; byteIndex < intData.length; byteIndex++) {
			int nibbleIndex = byteIndex * 2;
			nibbles[nibbleIndex] = (byte) (((int)intData[byteIndex] & 0xF0) >>> 4);
			nibbles[nibbleIndex + 1] = (byte) (intData[byteIndex] & 0x0F);
		}
		return nibbles;
	}
	public int[] nibblesToBytes(int[] deinterleaved) {
		int nibbleIndex;
		int nibble;
		int highNibble;
		int lowNibble;
		int[] ints = new int[deinterleaved.length / 2];
		for(int byteIndex = 0; byteIndex < ints.length; byteIndex++) {
			nibbleIndex = byteIndex * 2;
			nibble = deinterleaved[nibbleIndex] & 0x0F;
			highNibble = nibble << 4;
			lowNibble = deinterleaved[nibbleIndex + 1] & 0x0F;
			ints[byteIndex] = (int)(highNibble | lowNibble);
		}
		return ints;
	}

	public byte[] bytesToNibbles(byte[] intData) {
		byte[] nibbles = new byte[intData.length * 2];
		for(int byteIndex = 0; byteIndex < intData.length; byteIndex++) {
			int nibbleIndex = byteIndex * 2;
			nibbles[nibbleIndex] = (byte) (((int)intData[byteIndex] & 0xF0) >>> 4);
			nibbles[nibbleIndex + 1] = (byte) (intData[byteIndex] & 0x0F);
		}
		return nibbles;
	}
	public byte[] nibblesToBytes(byte[] deinterleaved) {
		int nibbleIndex;
		int nibble;
		int highNibble;
		int lowNibble;
		byte[] bytes = new byte[deinterleaved.length / 2];
		for(int byteIndex = 0; byteIndex < bytes.length; byteIndex++) {
			nibbleIndex = byteIndex * 2;
			nibble = deinterleaved[nibbleIndex] & 0x0F;
			highNibble = nibble << 4;
			lowNibble = deinterleaved[nibbleIndex + 1] & 0x0F;
			bytes[byteIndex] = (byte)(highNibble | lowNibble);
		}
		return bytes;
	}

	public int[] padBytes(int[] bytes, int unencodedBlockLengthInBytes) {
		int[] paddedBytes;
		int lengthOfLengthField = 4;
		int offsetLength;
		// calculate the data length with the length field
		int byteLengthOfDataWithLengthField = bytes.length + lengthOfLengthField;
		
		if (unencodedBlockLengthInBytes != byteLengthOfDataWithLengthField % unencodedBlockLengthInBytes) {
			offsetLength = unencodedBlockLengthInBytes - byteLengthOfDataWithLengthField % unencodedBlockLengthInBytes;
			paddedBytes = new int[byteLengthOfDataWithLengthField + offsetLength];
			System.arraycopy(bytes, 0, paddedBytes, 4, bytes.length);
		} else {
			paddedBytes = new int[byteLengthOfDataWithLengthField];
			System.arraycopy(bytes, 0, paddedBytes, 4, bytes.length);
		}
		int[] lengthBytes = bytesToInts(IntUtil.intToByteArray(bytes.length));
		System.arraycopy(lengthBytes, 0, paddedBytes, 0, 4);
		return paddedBytes;
	}

	public int[] unpadBytes(int[] totalUnencodedBytes) {
		int lengthOfLengthField = 4;
		// pull data length bytes
		int[] lengthBytes = Arrays.copyOf(totalUnencodedBytes, lengthOfLengthField);
		// convert to length
		int length = IntUtil.byteArrayToInt(lengthBytes);
		// unpad bytes array
		return Arrays.copyOfRange(totalUnencodedBytes, 4, 4 + length);
	}

	public int countOfDataByteBlocks(int length) {
		return countOfBlocks(
				getNumberOfBlockDataBytes(), 
				length);
	}

	public int countOfCodeByteBlocks(int length) {
		return countOfBlocks(
				getNumberOfBlockCodeBytes(), 
				length);
	}

	protected int countOfBlocks(int blockLength, int length) {
		int probedBlockLength = blockLength;
		while(length > probedBlockLength)
			probedBlockLength += blockLength;
		return probedBlockLength/blockLength;
	}
}
