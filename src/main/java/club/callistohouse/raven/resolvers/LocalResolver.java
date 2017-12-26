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
package club.callistohouse.raven.resolvers;

import java.util.List;

import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.MurmurException;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.utils.ListUtil;

public class LocalResolver implements Resolver {
	private Ref promise;
	private List<MessageSend> buffer;

	public LocalResolver(Ref promise, List<MessageSend> buffer) {
		this.promise = promise;
		this.buffer = buffer;
	}

	public void resolve(Object result) throws NotResolvedException {
		if(result == null)
			return;
		if(promise == null) {
			try {
				throw new MurmurException("already resolved");
			} catch (MurmurException e) {
				e.printStackTrace();
			}
		}
		Ref newRef = RefUtil.wrap(result, promise.getVat());
		promise.getRefImpl().becomeContext(newRef.getRefImpl());
		sendMessages();
		if(RefUtil.isResolved(newRef)) {
			promise = null;
		}
	}

	public void smash(Throwable e) throws NotResolvedException {
		resolve(e);
	}

	private void sendMessages() throws NotResolvedException {
		if(buffer == null)
			return;
		List<MessageSend> pendingMessages = ListUtil.reverseList(buffer);
		buffer = null;
		for(MessageSend send:pendingMessages) {
			send.setRef(promise);
			promise.redirectMessage(send);
		}
	}
}
