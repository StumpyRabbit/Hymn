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

import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.RefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.utils.events.Listener;

public class Ref {
	private RefImpl refImpl;

	public Ref(RefImpl refImpl) { this.refImpl = refImpl; }

	public Object getReceiver() throws NotResolvedException { return getReceiver(5000); }
	public Object getReceiver(int timeout) throws NotResolvedException { return refImpl.getReceiver(timeout); }

	public Ref redirectMessage(String action, Object... args) throws NotResolvedException { return redirectMessage(new MessageSend(false, true, getVat(), action, args)); }
	public void redirectMessageOneWay(String action, Object... args) throws NotResolvedException { redirectMessage(new MessageSend(RefUtil.getRefImplActions().contains(action), false, getVat(), action, args)); }
	public Ref redirectMessage(MessageSend send) throws NotResolvedException { return refImpl.redirectMessage(send); }

	public void whenResolved(ReactorInterface reactor) { 
		try {
			refImpl.whenMoreResolved(reactor);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}
	public void whenBroken(ReactorInterface reactor) { try {
		refImpl.whenBroken(reactor);
	} catch (NotResolvedException e) {
		e.printStackTrace();
	} }

	public void whenResolved(Ref reactorRef) { 
		try {
			refImpl.whenMoreResolved(reactorRef);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}
	public void whenBroken(Ref reactorRef) { try {
		refImpl.whenBroken(reactorRef);
	} catch (NotResolvedException e) {
		e.printStackTrace();
	} }

	public void addListener(Listener<?> listener) {
		try {
			redirectMessageOneWay("addListener", listener);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}

	public void removeListener(Listener<?> listener) {
		try {
			redirectMessageOneWay("removeListener", listener);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}

	public void fire(Object event) {
		try {
			redirectMessageOneWay("fire", event);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}

	public Vat getVat() { return refImpl.getVat(); }
	public RefImpl getRefImpl() { return refImpl; }
	public void setRefImpl(RefImpl newRefImpl) { this.refImpl = newRefImpl; }
	public String toString() { return refImpl.toString(); }
}
