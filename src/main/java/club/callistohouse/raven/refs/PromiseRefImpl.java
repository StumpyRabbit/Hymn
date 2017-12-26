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
package club.callistohouse.raven.refs;

import java.util.List;

import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.resolvers.Resolver;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.utils.Pair;

public class PromiseRefImpl extends RefImpl implements PromiseInterface {
	private RefImpl ref;
	private List<MessageSend> buffer;
	private Vat vat;

	public PromiseRefImpl(List<MessageSend> buffer, Vat vat) {
		this.vat = vat;
		this.buffer = buffer;
	}

	public Vat getVat() { return vat; }
	public Object getReceiver() throws NotResolvedException { return getReceiver(1000); }
	public Object getReceiver(long timeout) throws NotResolvedException { return getRefImpl(timeout).getReceiver(); }
	synchronized RefImpl getRefImpl(long timeout) throws NotResolvedException {
		if(ref == null) {
			synchronized(this) {
				try {
					wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if(ref == null) {
			throw new NotResolvedException("promise not resolved");
		}
		return ref;
	}

	public void becomeContext(RefImpl newRef) throws NotResolvedException {
		super.becomeContext(newRef);
		ref = newRef;
		synchronized(this) {
			notifyAll();
		}
	}
	public Ref redirectMessage(MessageSend send) {
		if (send.isOneWay()) {
			buffer.add(send);
			return null;
		} else {
			Pair<PromiseRefImpl, Resolver> pair = promise();
			send.setResolver(pair.second());
			buffer.add(send);
			Ref chainedPromise = pair.first().getProxy();
			return chainedPromise;
		}
	}
	public void whenMoreResolved(ReactorInterface reactor) {
		buffer.add(new MessageSend(true, false, getVat(), "whenMoreResolved", reactor));
	}
	public void whenBroken(ReactorInterface reactor) {
		buffer.add(new MessageSend(true, false, getVat(), "whenBroken", reactor));
	}

	public String toString() {
		return getClass().getSimpleName() + "(hash: " + hashCode() + ", buffered sends: " + buffer.size() + ")";
	}
}
