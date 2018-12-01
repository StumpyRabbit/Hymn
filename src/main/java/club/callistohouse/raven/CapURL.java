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
package club.callistohouse.raven;

import java.math.BigInteger;
import java.net.InetSocketAddress;

import club.callistohouse.session.parrotttalk.SessionIdentity;

public class CapURL {

	private static String getSchema() { return "murmur"; }

	public static void validateHeader(String urlString) {
		if(!getSchema().equals(urlString.substring(0, 6)))
			throw new IllegalArgumentException("bad schema");
		if(!":".equals(urlString.substring(6, 7)))
			throw new IllegalArgumentException("bad formation");
		if(!"//".equals(urlString.substring(7, 9)))
			throw new IllegalArgumentException("bad formation");
	}
	public static String bigIntegerToHexString(BigInteger big) {
		return big.toString(16);
	}
	public static BigInteger hexStringToBigInteger(String hexString) {
		return new BigInteger(hexString, 16);
	}

	private SessionIdentity whisperId;
	private BigInteger swissNumber;

	public CapURL(SessionIdentity whisperId, BigInteger swissNumber) {
		this.whisperId = whisperId;
		this.swissNumber = swissNumber;
	}

	public CapURL(String urlString) {
		validateHeader(urlString);
		String content = urlString.substring(9, urlString.length());
		String[] fields = content.split("/");
		if(fields.length != 4)
			throw new IllegalArgumentException("bad url");
		String[] ipPort = fields[0].split(":");
		if(ipPort.length == 1) {
			ipPort = new String[] { ipPort[0], "11111" };
		}
		InetSocketAddress isa = new InetSocketAddress(ipPort[0], Integer.valueOf(ipPort[1]));
		String nickname = fields[1];
		try {
			setWhisperId(new SessionIdentity(nickname, isa, fields[2]));
		} catch (Exception e) {
			setWhisperId(new SessionIdentity(nickname, isa));
		}
		BigInteger swissNumber = hexStringToBigInteger(fields[3]);
		setSwissNumber(swissNumber);
	}

	public SessionIdentity getSessionId() { return whisperId; }
	public void setWhisperId(SessionIdentity whisperId) { this.whisperId = whisperId; }
	public BigInteger getSwissNumber() { return swissNumber; }
	public void setSwissNumber(BigInteger swissNumber) { this.swissNumber = swissNumber; }

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getSchema() + "://"); 
		builder.append(getSessionId().getDomain() + "/");
		builder.append(getSessionId().getSocketAddress().toString() + "/");
		builder.append(getSessionId().getVatId() + "/"); 
		builder.append(bigIntegerToHexString(getSwissNumber()));
		return builder.toString();
	}
}
