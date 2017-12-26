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
package club.callistohouse.raven.tables;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.RefUtil;

public class SwissTable {

	private SecureRandom secureRandom;
	private Map<WeakReference<Object>, BigInteger> objectsToSwiss = new HashMap<WeakReference<Object>, BigInteger>();
	private Map<BigInteger, WeakReference<Object>> swissToReference = new HashMap<BigInteger,WeakReference<Object>>();

	public SwissTable() {
		byte seed = new Long(System.currentTimeMillis()).byteValue();
		byte[] baseSeed = new byte[1];
		baseSeed[0] = seed;
		byte[] seedBytes = new byte[20];
		for(int i = 0; i < 20; i++) {
			System.arraycopy(baseSeed, 0, seedBytes, i, 1);
		}
		new SecureRandom(seedBytes).nextBytes(seedBytes);
		secureRandom = new SecureRandom(seedBytes);
	}

	public int size() { return swissToReference.size(); }

	public BigInteger generateSwiss() {
		byte[] swissBytes = new byte[20];
		secureRandom.nextBytes(swissBytes);
		return new BigInteger(swissBytes);
	}

	public Object lookupSwiss(BigInteger swissNumber) {
		try {
			return lookupLocalSwiss(swissNumber);
		} catch(IllegalStateException e) {}
		return null;
	}

	protected Object lookupLocalSwiss(BigInteger swissNumber) throws IllegalArgumentException {
		WeakReference<Object> weak = swissToReference.get(swissNumber);
		if(weak == null) {
			throw new IllegalStateException("no reference found");
		}
		Object ref = weak.get();
		if(ref != null) {
			return ref;
		}
		throw new IllegalStateException("weak reference went away");
	}

	public BigInteger registerIdentitySwiss(Object object, byte[] swissBase) throws IOException, DigestException, NotResolvedException {
		Object realObject = RefUtil.unwrap(object);
		BigInteger swiss = objectsToSwiss.get(realObject);
		if(swiss == null) {
			swiss = generateSwiss();
			WeakReference<Object> weakObject = new WeakReference<Object>(realObject);
			swissToReference.put(swiss, weakObject);
			objectsToSwiss.put(weakObject, swiss);
		}
		return swiss;
	}

	public BigInteger getNewSwiss(Object object) throws IOException, DigestException, NotResolvedException {
		Object realObject = RefUtil.unwrap(object);
		if(realObject == null)
			return BigInteger.valueOf(0);
		return getIdentity(realObject);
	}

	public BigInteger registerNewReferenceSwiss(Object object, BigInteger swiss) throws DigestException, IOException, NotResolvedException {
		Object realObject = RefUtil.unwrap(object);
		WeakReference<Object> weak = new WeakReference<Object>(realObject);
		swissToReference.put(swiss, weak);
		objectsToSwiss.put(weak, swiss);
		return swiss;	
	}

	public BigInteger getIdentity(Object object) throws IOException, DigestException, NotResolvedException {
		Object realObject = RefUtil.unwrap(object);
		BigInteger swiss = objectsToSwiss.get(realObject);
		if(swiss == null) {
			swiss = generateSwiss();
			WeakReference<Object> weak = new WeakReference<Object>(realObject);
			swissToReference.put(swiss, weak);
			objectsToSwiss.put(weak, swiss);
		}
		return swiss;	
	}

	public boolean hasRegistration(Object target) throws IOException, DigestException, NotResolvedException {
		return getSwissForObject(target) != null;
	}

	public BigInteger getSwissForObject(Object target) {
		for(Entry<WeakReference<Object>,BigInteger> entry : objectsToSwiss.entrySet()) {
			WeakReference<Object> weak = (WeakReference<Object>)entry.getKey();
			Object eachObj = weak.get();
			if(eachObj != null) {
				if(target.equals(eachObj)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}
}




