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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import club.callistohouse.raven.PauwauUrl;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.vat.Vat;

public class RefTests {

	@Before
	public void setup() {
		BasicConfigurator.configure();
	}

	@Test
	public void testOne() {
		Ref ref = RefUtil.wrap("a pauwau is the best", Vat.getLocalVat());
		Ref newRef = null;
		try {
			newRef = ref.redirectMessage("indexOf", "best");
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		Integer reply = null;
		try {
			reply = (Integer) newRef.getReceiver(300);
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		assertEquals(16, reply.intValue());
	}
	@Test
	public void testTwo() {
		Ref ref = RefUtil.wrap("a pauwau is the best", Vat.getLocalVat());
		try {
			assertEquals("a pauwau is the best", ref.getReceiver());
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		Ref newRef = null;
		try {
			newRef = ref.redirectMessage("length");
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		try {
			assertEquals(20, newRef.getReceiver());
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
	}
	@Test
	public void testPipelineSend() {
		Ref ref = RefUtil.wrap("a pauwau is the best", Vat.getLocalVat());
		Ref newRef1 = null;
		try {
			newRef1 = ref.redirectMessage("indexOf", "best");
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		Ref newRef2 = null;
		try {
			newRef2 = newRef1.redirectMessage("hashCode");
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		Integer reply = null;
		try {
			reply = (Integer) newRef2.getReceiver();
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		assertEquals(16, reply.intValue());
	}
	@Test
	public void testBounceSend() {
		Ref alice = RefUtil.wrap(new GalaxyObject(), new Vat("alice"));
		Ref bob = RefUtil.wrap(new GalaxyObject(), new Vat("bob"));
		Ref answerRef = null;
		try {
			answerRef = alice.redirectMessage("redirectForTheAnswer", bob);
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		Integer reply = null;
		try {
			reply = (Integer) answerRef.getReceiver(1000);
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		assertEquals(42, reply.intValue());
	}
	@Test
	public void testEventualArg() {
		Ref alice = RefUtil.wrap(new GalaxyObject(), Vat.getLocalVat());
		Ref bob = RefUtil.wrap(new GalaxyObject(), Vat.getLocalVat());
		Ref answerRef = null;
		try {
			answerRef = alice.redirectMessage("probeFarRef", bob);
		} catch (NotResolvedException e) {
			assertTrue(false);
			e.printStackTrace();
		}
		try {
			Integer reply = (Integer) answerRef.getReceiver(1000);
			assertEquals(42, reply.intValue());
		} catch (NotResolvedException e) {
			assertTrue(true);
			e.printStackTrace();
		}
	}

	@Test
	public void testMurmorUrl() {
		PauwauUrl id = new PauwauUrl("murmur://localhost:4200/nick/0F0A080410/0F0A080410");
		assertEquals("localhost", id.getSessionId().getSocketAddress().getHostName());
		assertEquals(4200, id.getSessionId().getSocketAddress().getPort());
		assertEquals("0F0A080410", id.getSessionId().getVatId());
		assertEquals("64592806928", id.getSwissNumber().toString());
	}
}
