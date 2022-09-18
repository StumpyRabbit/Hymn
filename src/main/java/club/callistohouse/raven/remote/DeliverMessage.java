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
package club.callistohouse.raven.remote;

import java.io.IOException;

import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.handlers.RemotePromiseHandler;
import club.callistohouse.raven.scope.Scope;



public class DeliverMessage extends AbstractDeliverMessage {
	private static final long serialVersionUID = 771579998323045838L;

	private int answerId;
	private Object redirector;

	public DeliverMessage(int wireId, String selector, Object...args) {
		super(wireId, selector, args);
	}

	public int getAnswerId() { return answerId; }
	public void setAnswerId(int answerId) { this.answerId = answerId; }
	public Object getRedirector() { return redirector; }
	public void setRedirector(Object  redirector) { this.redirector = redirector; }
	public Integer getResolverId() { 
		if(redirector == null) return null;
		/*if(RefUtil.isRef(redirector)) return (Ref)(RefUtil.unwrap(redirector);
		return ((Redirector)redirector).getWireId();*/
		return 0;
	}

	public MessageEnum getType() { return MessageEnum.DELIVER; }

	public Ref sendMessageOnScope(Scope scope) throws IOException, NotResolvedException {
		RemotePromiseHandler promiseHandler = scope.makeQuestion();
		setAnswerId(promiseHandler.wireId);
		setRedirector(promiseHandler.makeDelayedRedirector());
		try {
			super.sendMessageOnScope(scope);
		} catch(Exception e) {
			promiseHandler.resolver.smash(e);
		}
		return promiseHandler.resolver.getProxy();
	}
	public void receiveMessageOnScope(Scope scope) throws IOException, NotResolvedException {
		Ref receiver = (Ref)scope.internalize(getReceiverDesc());
		Ref answer = receiver.redirectMessage(asMessageSend(scope.getVat()));
		scope.registerAnswerAtWirePosition(answer, getAnswerId());
		answer.whenResolved((ReactorInterface) ((Ref)getRedirector()).getReceiver());
	}

	public String toString() {
		return getClass().getSimpleName() 
				+ "(receiverId: " + getReceiverId() 
				+ " answerId: " + getAnswerId()
				+ " resolverId: " + getResolverId()
				+ " action: " + getSelector() 
				+ " args: " + argumentsToString(getArguments())
				+ ")";
	}
}
