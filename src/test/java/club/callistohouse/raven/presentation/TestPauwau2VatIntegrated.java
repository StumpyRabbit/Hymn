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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import club.callistohouse.raven.PassByCopy;
import club.callistohouse.raven.CapURL;
import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.presentation.utils.GalaxyObject;
import club.callistohouse.utils.events.EventEngine;
import club.callistohouse.utils.events.Listener;

public class TestPauwau2VatIntegrated extends AbstractPauwauTest {

	@Before
	public void setup() {
		setup2Introducers();
	}
	@After
	public void tearDown() {
		tearDown2Introducers();
	}

	@Test(timeout=50000)
	public void testTwoVats() throws Exception {
		try {
			CapURL url = registerObject(pauwau1, new GalaxyObject());
			Ref testObject = realizeRef(pauwau2, url);
			Ref answer1 = testObject.redirectMessage("getTheAnswer");
			log.info("The value is: " + answer1.getReceiver(20000));
			assertEquals(42, answer1.getReceiver(1000));
		} catch(Exception e) {
			log.debug(e);
			assertTrue(false);
		}
	}
	public boolean whenReactorTriggered = false;
	@Test(timeout=50000)
	public void testTwoVatsWithWhen() throws Exception {
		CapURL url = registerObject(pauwau1, new GalaxyObject());
		final Ref answer = realizeRef(pauwau2, url).redirectMessage("getTheAnswer").redirectMessage("hashCode");
		answer.whenResolved(buildWhenReactor());
		assertEquals(42, answer.getReceiver(20000));
		log.info("The value from the promise is: " + answer.getReceiver(1000));
		Thread.sleep(10);
		assertTrue(whenReactorTriggered);
	}
	protected ReactorInterface buildWhenReactor() {
		return new ReactorInterface() {
			public void value(Object obj) throws NotResolvedException {
				whenReactorTriggered = true;
				log.info("The value in the reactor is: " + obj);
			}
			public void reactToLostClient(Exception e) {
				assertFalse(false);
			}
		};
	}
	Object myTestEvent;
	@Test(timeout=50000)
	public void testTwoVatsEvents() throws Exception {
		try {
			log.info("starting Two Vat Event test");
			TestObjectProducer testProducer = new TestObjectProducer();
			CapURL url = pauwau1.makeOrReturnUrl(testProducer, true);
			Ref testObjectRef = pauwau2.sturdy(url).liveRef();

			testObjectRef.addListener( 
					new Listener<PauwauTestEvent>(PauwauTestEvent.class) { 
							protected void handle(PauwauTestEvent event) {
								log.info(event);
								myTestEvent = event;
							}});

			Thread.sleep(20000);
			testProducer.fireTestEvent();
			Thread.sleep(200);
			assertNotNull(myTestEvent);

			myTestEvent = null;

			testObjectRef.fire(new PauwauTestEvent());
			Thread.sleep(200);
			assertNotNull(myTestEvent);
		} catch(Exception e) {
			log.error("test blows up", e);
			assertTrue(false);
		}
	}
	@SuppressWarnings("serial")
	private static class PauwauTestEvent implements PassByCopy {}
	private static class TestObjectProducer extends EventEngine {
		void fireTestEvent() {
			fire(new PauwauTestEvent());
		}
	}
}
