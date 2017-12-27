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
package club.callistohouse.raven.presentation;

import java.io.IOException;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import club.callistohouse.raven.Raven;
import club.callistohouse.raven.CapURL;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;

public class AbstractPauwauTest {
	static Logger log = Logger.getLogger(AbstractPauwauTest.class);

	protected static Ref realizeRef(Raven intro, CapURL url) throws NotResolvedException {
		return intro.liveRef(url);
	}

	protected static CapURL registerObject(Raven intro, Object obj) throws DigestException, NotResolvedException, NoSuchAlgorithmException, IOException {
		return intro.makeOrReturnUrl(obj, true);
	}

	Raven pauwau1 = null;
	Raven pauwau2 = null;
	Raven pauwau3 = null;

	public void setup2Introducers() {
		if(pauwau1 == null) {
			BasicConfigurator.configure();
			log.info("2 Introducers starting");
			try {
				Thread.sleep(1000);
				pauwau1 = new Raven("first", 10001).onTheAir();
				pauwau2 = new Raven("second", 10002).onTheAir();
			} catch (Exception e) { log.error("oops", e); }
			log.info("2 Introducers on the air");
		} else {
			log.info("2 Introducers already started");
		}
	}

	public void tearDown2Introducers() {
		log.info("2 Introducers stopping");
		pauwau1.offTheAir(); pauwau1 = null;
		pauwau2.offTheAir(); pauwau2 = null;
		log.info("2 Introducers off the air");
	}

	public void setup3Introducers() {
		if(pauwau3 == null) {
			BasicConfigurator.configure();
			log.info("3 Introducers starting");
			try {
				pauwau1 = new Raven("first", 10001).onTheAir();
				pauwau2 = new Raven("second", 10002).onTheAir();
				pauwau3 = new Raven("third", 10003).onTheAir();
			} catch (Exception e) { log.error("oops", e); }
			log.info("3 Introducers on the air");
		} else {
			log.info("3 Introducers already started");
		}
	}

	public void tearDown3Introducers() {
		log.info("3 Introducers stopping");
		pauwau1.offTheAir(); pauwau1 = null;
		pauwau2.offTheAir(); pauwau2 = null;
		pauwau3.offTheAir(); pauwau3 = null;
		log.info("3 Introducers off the air");
	}
}
