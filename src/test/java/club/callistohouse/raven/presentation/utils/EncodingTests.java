/*******************************************************************************
 * The MIT License (MIT)
 *
 * Copyright (c) 2003, 2016 Robert Withers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ******************************************************************************
 * porcini/whisper would not be possible without the ideas, implementation, 
 * brilliance and passion of the Squeak/Pharo communities and the cryptography 
 * team, which are this software's foundation.
 * ******************************************************************************
 * porcini/whisper would not be possible without the ideas, implementation, 
 * brilliance and passion of the erights.org community, which is also this software's 
 * foundation.  In particular, I would like to thank the following individuals:
 *         Mark Miller
 *         Marc Stiegler
 *         Bill Franz
 *         Tyler Close 
 *         Kevin Reid
 *******************************************************************************/
package club.callistohouse.raven.presentation.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import com.google.zxing.common.reedsolomon.GaloisField;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

import club.callistohouse.raven.core.RavenServer;
import club.callistohouse.raven.core.RavenTerminal;
import club.callistohouse.raven.remote.MurmurInputStream;
import club.callistohouse.raven.remote.MurmurOutputStream;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.tables.SwissTable;
import club.callistohouse.session.SessionIdentity;

public class EncodingTests {

	@Before
	public void setup() { BasicConfigurator.configure(); }

	@Test
	@SuppressWarnings({ "unchecked", "resource" })
	public void testArrayList() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		List<String> list = new ArrayList<String>();
		list.add("pauwau");

		SwissTable table = new SwissTable();
		Scope scope = new Scope(new RavenTerminal(new RavenServer(new SessionIdentity("mine", 10042), table)), table);

		ByteArrayOutputStream baos1 = new ByteArrayOutputStream(); 
		new ObjectOutputStream(baos1).writeObject(list);
		List<String> poppedList1 = (List<String>)new ObjectInputStream(new ByteArrayInputStream(baos1.toByteArray())).readObject();

		ByteArrayOutputStream baos2 = new ByteArrayOutputStream(); 
		new MurmurOutputStream(baos2, scope).writeObject(list);
		List<String> poppedList2 = (List<String>)new MurmurInputStream(new ByteArrayInputStream(baos2.toByteArray()), scope).readObject();

		assertEquals(list, poppedList1);
		assertEquals(list, poppedList2);
	}

	@SuppressWarnings("resource")
	@Test
	public void testArray() throws IOException, ClassNotFoundException {
		String[] array = new String[] { "robert" };
		SwissTable table = new SwissTable();
		Scope scope = new Scope(new RavenTerminal(new RavenServer(new SessionIdentity("mine", 10042), table)), table);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		new MurmurOutputStream(baos, scope).writeObject(array);
		String[] poppedArray = (String[])new MurmurInputStream(new ByteArrayInputStream(baos.toByteArray()), scope).readObject();
		assertArrayEquals(array, poppedArray);
	}

	@SuppressWarnings("resource")
	@Test
	public void testString() throws IOException, ClassNotFoundException {
		String str = "Life can be so sweet, all it takes is faith and the right attitude";
		SwissTable table = new SwissTable();
		Scope scope = new Scope(new RavenTerminal(new RavenServer(new SessionIdentity("mine", 10042), table)), table);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		new MurmurOutputStream(baos, scope).writeObject(str);
		System.out.println(baos.toString());
		String poppedStr = (String)new MurmurInputStream(new ByteArrayInputStream(baos.toByteArray()), scope).readObject();
		assertEquals(str, poppedStr);
	}

	@Test
	public void testBaseParametersForRS_15_9() {
		GaloisField galoisField = new GaloisField(GenericGF.OW_CODE_FIELD_16, 15, 9, 4);
		assertEquals(15, galoisField.getNumberOfChunkCodeSymbols());
		assertEquals(9, galoisField.getNumberOfChunkDataSymbols());
		assertEquals(30, galoisField.getNumberOfBlockCodeBytes());
		assertEquals(18, galoisField.getNumberOfBlockDataBytes());
		assertEquals(60, galoisField.getNumberOfBlockCodeSymbols());
		assertEquals(36, galoisField.getNumberOfBlockDataSymbols());
		assertEquals(6, galoisField.getECSymbols());
		assertEquals(4, galoisField.getSymbolSizeInBits());
	}

	@Test
	public void testBaseParametersForRS_255_247() {
		GaloisField galoisField = new GaloisField(GenericGF.RS_256_A, 255, 247, 8);
		assertEquals(255, galoisField.getNumberOfChunkCodeSymbols());
		assertEquals(247, galoisField.getNumberOfChunkDataSymbols());
		assertEquals(1020, galoisField.getNumberOfBlockCodeBytes());
		assertEquals(988, galoisField.getNumberOfBlockDataBytes());
		assertEquals(1020, galoisField.getNumberOfBlockCodeSymbols());
		assertEquals(988, galoisField.getNumberOfBlockDataSymbols());
		assertEquals(8, galoisField.getECSymbols());
		assertEquals(8, galoisField.getSymbolSizeInBits());
	}

	@Test
	public void testBaseParametersForRS_255_223() {
		GaloisField galoisField = new GaloisField(GenericGF.RS_256_B, 255, 223, 8);
		assertEquals(255, galoisField.getNumberOfChunkCodeSymbols());
		assertEquals(223, galoisField.getNumberOfChunkDataSymbols());
		assertEquals(1020, galoisField.getNumberOfBlockCodeBytes());
		assertEquals(892, galoisField.getNumberOfBlockDataBytes());
		assertEquals(1020, galoisField.getNumberOfBlockCodeSymbols());
		assertEquals(892, galoisField.getNumberOfBlockDataSymbols());
		assertEquals(32, galoisField.getECSymbols());
		assertEquals(8, galoisField.getSymbolSizeInBits());
	}

	@Test
	public void testEncodeAndDecodeRS_15_9() throws ReedSolomonException {
		GaloisField galoisField = new GaloisField(GenericGF.OW_CODE_FIELD_16, 15, 9, 4);
		byte[] chunk = new byte[] { 
				(byte) 0x0F, 
				(byte) 0x0F, 
				(byte) 0x0A, 
				(byte) 0x0A, 
				(byte) 0x0B, 
				(byte) 0x0B, 
				(byte) 0x09, 
				(byte) 0x09, 
				(byte) 0x0D};
		byte[] encodedBytes = new ReedSolomonEncoder(galoisField).encodeBytes(chunk);
		byte[] decodedBytes = new ReedSolomonDecoder(galoisField).decodeBytes(encodedBytes);
		assertArrayEquals(chunk, decodedBytes);
	}
	@Test
	public void testEncodeAndDecodeRS_255_247() throws ReedSolomonException {
		GaloisField galoisField = new GaloisField(GenericGF.RS_256_A, 255, 247, 8);
		byte[] chunk = new byte[247];
		byte[] test = new byte[] { 
				(byte) 0x0F, 
				(byte) 0x0E, 
				(byte) 0x0D, 
				(byte) 0x0B, 
				(byte) 0x0A, 
				(byte) 0x09, 
				(byte) 0x08, 
				(byte) 0x07, 
				(byte) 0x06, 
				(byte) 0x05};
		for(int i = 0; i < 24; i++) {
			System.arraycopy(test, 0, chunk, i*10, test.length);
		}
		test = new byte[] { 
				(byte) 0x0F, 
				(byte) 0x0E, 
				(byte) 0x0D, 
				(byte) 0x0B, 
				(byte) 0x0A,
				(byte) 0x09,
				(byte) 0x08 };
		System.arraycopy(test, 0, chunk, 240, test.length);
		byte[] encodedBytes = new ReedSolomonEncoder(galoisField).encodeBytes(chunk);
		byte[] decodedBytes = new ReedSolomonDecoder(galoisField).decodeBytes(encodedBytes);
		assertArrayEquals(chunk, decodedBytes);
	}
	@Test
	public void testEncodeAndDecodeRS_255_223() throws ReedSolomonException {
		GaloisField galoisField = new GaloisField(GenericGF.RS_256_B, 255, 223, 8);
		byte[] chunk = new byte[223];
		byte[] test = new byte[] { 
				(byte) 0x0F, 
				(byte) 0x0E, 
				(byte) 0x0D, 
				(byte) 0x0B, 
				(byte) 0x0A, 
				(byte) 0x09, 
				(byte) 0x08, 
				(byte) 0x07, 
				(byte) 0x06, 
				(byte) 0x05};
		for(int i = 0; i < 22; i++) {
			System.arraycopy(test, 0, chunk, i*10, test.length);
		}
		test = new byte[] { 
				(byte) 0x0F, 
				(byte) 0x0E, 
				(byte) 0x0D };
		System.arraycopy(test, 0, chunk, 220, test.length);
		byte[] encodedBytes = new ReedSolomonEncoder(galoisField).encodeBytes(chunk);
		byte[] decodedBytes = new ReedSolomonDecoder(galoisField).decodeBytes(encodedBytes);
		assertArrayEquals(chunk, decodedBytes);
	}

/*	@Test
	public void testSmallDataEncoderDecoder() throws ReedSolomonException {
		byte[] testBytes = getSmallData();
		byte[] encodedBytes = new FECBlockEncoder().encode(testBytes);
		assertArrayEquals(testBytes, decodedBytes);
	}
	@Test
	public void testLargeDataEncoderDecoder() throws ReedSolomonException {
		byte[] testBytes = getLargeData();
		byte[] encodedBytes = new FECBlockEncoder().encode(testBytes);
		byte[] decodedBytes = new FECBlockDecoder().decode(encodedBytes);
		assertArrayEquals(testBytes, decodedBytes);
	}*/

	protected byte[] getSmallData() { return "hello world".getBytes(); }
	protected byte[] getLargeData() {
		byte[] patternBytes = "helloworld".getBytes();
		byte[] testBytes = new byte[3000000];
		for(int i = 0; i < 300000; i++) {
			System.arraycopy(patternBytes, 0, testBytes, i*10, 10);
		}
		return testBytes;
	}

/*	@Test
	public void testEncodingDecoding() throws IOException, ClassNotFoundException {
		List<byte[]> byteList = getTestObject();
		byte[] serializedBytes = serializeObject(byteList);
		byte[] encodedBytes = encodeTranny().encode(serializedBytes);
		byte[] decodedBytes = decodeTranny().decode(encodedBytes);
		assertArrayEquals(serializedBytes, decodedBytes);
	}

	@Test
	public void testByteArrays() throws IOException, ClassNotFoundException {
		byte[] bytes = "hello pauwau".getBytes();
		byte[] encodedBytes = encodeTranny().encode(bytes);
		byte[] decodedBytes = decodeTranny().decode(encodedBytes);
		assertArrayEquals(bytes, decodedBytes);
	}*/

	protected List<byte[]> getTestObject() {
		byte[] testBytes = new byte[1203];
		new SecureRandom().nextBytes(testBytes);
		List<byte[]> byteList = new ArrayList<byte[]>();
		byteList.add("hello pauwau".getBytes());
		byteList.add(testBytes);
		return byteList;
	}

	protected byte[] serializeObject(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(obj);
		return baos.toByteArray();
	}
	protected Object deserializeObject(byte[] decodedBytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes); 
		ObjectInputStream in = new ObjectInputStream(bais);
		return in.readObject();
	}

/*	@Test
	public void testEncoding() throws ReedSolomonException, IOException {
		String message = "Please don't serve a bad meal";
		System.out.println(new String(encodeTranny().encode(message.getBytes())));
//		System.out.println(encodeAndSerializeWithFECBlockEncoder(message));
	}
	@Test
	public void testDeoding() throws ReedSolomonException, IOException, ClassNotFoundException {
		String message = "laedntsreabdmale a  ve 'o seP";
		System.out.println(new String(decodeTranny().decode(message.getBytes())));
//		System.out.println(decodeAndDeserializeWithFECBlockEncoder(message));
	}

	ScrambleByteTransformer encodeTranny() { return new ScrambleByteTransformer(); }
	ScrambleByteTransformer decodeTranny() { return new ScrambleByteTransformer(); }*/
}
