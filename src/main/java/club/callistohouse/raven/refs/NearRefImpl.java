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

import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.resolvers.Resolver;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.utils.Pair;

public class NearRefImpl extends RefImpl {

	private Vat vat;
	private Object receiver;
	private Class<?> receiverClass;

	public NearRefImpl(Object receiver, Vat vat) {
		this.receiver = receiver;
		if(receiver != null)
			this.receiverClass = (Class<?>) receiver.getClass();
		this.vat = vat;
	}

	public Object getReceiver() { return receiver; }
	public Object getReceiver(long timeout) { return getReceiver(); }
	public Class<?> getReceiverClass() { return receiverClass; }

	public Ref redirectMessage(MessageSend send) {
		try {
			if (send.isOneWay()) {
				send.setRef(getProxy());
				getVat().send(send);
				return null;
			} else {
				Ref ref = null;
				send.setRef(getProxy());
				if(send.getResolver() == null) {
					Pair<PromiseRefImpl, Resolver> pair = promise();
					send.setResolver(pair.second());
					ref = pair.first().getProxy();
				}
				getVat().send(send);
				return ref;
			}
		} catch(Exception e) {
			return RefUtil.wrap(e, getVat());
		}
	}

	public void whenBroken(ReactorInterface reactor) {}

	public Vat getVat() { return vat; }

	public String toString() {
		return getClass().getSimpleName() + "(hash: " + hashCode() + ", receiver: " + getReceiver() + ")";
	}
}
