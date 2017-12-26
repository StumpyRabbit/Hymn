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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.Test;

import club.callistohouse.raven.PauwauUrl;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.presentation.utils.GalaxyObject;

public class TestPauwau3Vat extends AbstractPauwauTest {
	private static Logger log = Logger.getLogger(TestPauwau3Vat.class);

	@Test
	public void testThreeVats() throws Exception {
		try {
			setup3Introducers();
			PauwauUrl aliceUrl = registerObject(pauwau1, new GalaxyObject());
			PauwauUrl bobUrl = registerObject(pauwau2, new GalaxyObject());

			final Ref alice = (Ref) realizeRef(pauwau3, aliceUrl);
			final Ref bob = (Ref) realizeRef(pauwau3, bobUrl);

			Ref answer = alice.redirectMessage("redirectForTheAnswer", bob);
			log.info("The value is: " + answer.getReceiver(5000));
			assertEquals(42, answer.getReceiver(100));
		} catch(Exception e) {
			log.debug(e);
			assertTrue(false);
		} finally {
			tearDown3Introducers();
		}
	}
}
