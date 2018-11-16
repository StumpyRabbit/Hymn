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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import club.callistohouse.raven.core.RavenServer;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.scope.Locator;
import club.callistohouse.raven.tables.SwissTable;
import club.callistohouse.session.SessionIdentity;

public class Introducer {

	protected SessionIdentity localId;
	protected SwissTable swissTable;
	protected Locator locator;
	protected File persistentObjectsFile;
	protected Map<String,Object> persistentObjects = new HashMap<String, Object>();

	public Introducer(String nickname, int port) throws UnknownHostException {
		this(new SessionIdentity(nickname, port));
	}
	public Introducer(SessionIdentity id) {
		this.localId = id;
		this.swissTable = new SwissTable();
	}

	public Introducer onTheAir() throws FileNotFoundException, ClassNotFoundException, IOException {
		if (locator == null) {
			locator = new Locator(new RavenServer(localId, swissTable));
			locator.start();
		}
		return this;
	}
	public void offTheAir() {
		if (locator != null) {
			locator.stop();
			locator = null;
		}
	}

	public CapURL makeOrReturnUrl(Object obj, boolean persist) throws DigestException, NotResolvedException, NoSuchAlgorithmException, IOException {
		BigInteger swiss = swissTable.getSwissForObject(obj);
		if(swiss == null) {
			return makeSturdy(obj, persist).asUrl();
		} else {
			return new CapURL(localId.asPublicCopy(), swiss);
		}
	}

	public Ref liveRef(CapURL url) throws NotResolvedException { return sturdy(url).liveRef(); }
	public Ref liveRef(String urlString) throws NotResolvedException { return liveRef(new CapURL(urlString)); }
	public SturdyRef sturdy(CapURL url) { return new SturdyRef(url, locator); }
	public SturdyRef sturdy(String urlString) { return sturdy(new CapURL(urlString)); }
	public Object sturdy(BigInteger swiss) { return sturdy(new CapURL(localId.asPublicCopy(), swiss)); }
	public boolean hasSturdy(BigInteger swiss) {
		try {
			swissTable.lookupSwiss(swiss);
		} catch (IllegalStateException e) {
			return false;
		}
		return true;
	}

	public SturdyRef makeSturdy(Object object, boolean persist) throws NoSuchAlgorithmException, IOException, DigestException, NotResolvedException {
		return makeSturdyWithSwiss(object, swissTable.getIdentity(object), persist);
	}
	public SturdyRef makeSturdy(Object object, BigInteger swissNumber, boolean persist) throws NoSuchAlgorithmException, IOException, DigestException, NotResolvedException {
		return makeSturdyWithSwiss(object, swissTable.registerNewReferenceSwiss(object, swissNumber), persist);
	}
	protected SturdyRef makeSturdyWithSwiss(Object object, BigInteger swiss, boolean persist) throws FileNotFoundException, IOException {
		CapURL url = new CapURL(localId.asPublicCopy(), swiss);
		if(persist) {
			persistentObjects.put(url.toString(), object);
		}
		return new SturdyRef(url, locator);
	}
}
