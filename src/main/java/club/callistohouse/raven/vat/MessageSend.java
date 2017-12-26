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
package club.callistohouse.raven.vat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import club.callistohouse.raven.PassByCopy;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.MethodNotFoundException;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.exceptions.ReceiverNotFoundException;
import club.callistohouse.raven.refs.BrokenRefImpl;
import club.callistohouse.raven.refs.NearRefImpl;
import club.callistohouse.raven.refs.RefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.remote.MurmurMessage;
import club.callistohouse.raven.resolvers.Resolver;

@SuppressWarnings("rawtypes")
public class MessageSend implements PassByCopy,Runnable {
	private static final long serialVersionUID = 6977357360392603518L;
	private static final Logger logger = Logger.getLogger("Message Send");

	private static List<Sender> senderList = new ArrayList<Sender>();

	protected Object receiver;
	protected String action;
	protected Object[] args;
	protected Resolver resolver;
	protected Vat myVat;
	protected boolean returnsValue = true;
	protected boolean isRefImplReceiver = false;

	public MessageSend(boolean isRefImplReceiver, boolean returnsValue, String action, Object...args) {
		this.action = action;
		this.returnsValue = returnsValue;
		this.isRefImplReceiver = isRefImplReceiver;
		this.args = args;
	}
	public MessageSend(boolean isRefImplReceiver, boolean returnsValue, Vat vat, String action, Object...args) {
		this.action = action;
		this.returnsValue = returnsValue;
		this.isRefImplReceiver = isRefImplReceiver;
		this.args = args;
		wrapRemoteArgsAndUnwrapLocalArgs(vat, args);
	}

	public String getAction() { return action; }
	public Object[] getArgs() { return args; }
	public Resolver getResolver() { return resolver; }
	public Vat getVat() { return myVat; }
	public void setRef(Object promise) { this.receiver = promise; }
	public void setResolver(Resolver resolver) { this.resolver = resolver; }
	public void setVat(Vat vat) { 
		this.myVat = vat; 
		wrapRemoteArgsAndUnwrapLocalArgs(vat, args);
	}

	public boolean isOneWay() { return !returnsValue; }
	public boolean isRefImplReceiver() { return isRefImplReceiver; }

	public String toString() {
		try {
			return getClass().getSimpleName() + "(oneWay: " + isOneWay() + " action: " + action + ", " + getReceiver() + ", " + MurmurMessage.argumentsToString(args) + ")";
		} catch (ReceiverNotFoundException e) {
			e.printStackTrace();
		}
		return getClass().getSimpleName() + "(oneWay: " + isOneWay() + " action: " + action + ")";
	}

	public void run() {
		try {
			sendIt();
		} catch (ReceiverNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void wrapRemoteArgsAndUnwrapLocalArgs(Vat vat, Object... localArgs) {
		for(int i = 0; i < localArgs.length; i++) {
			localArgs[i] = RefUtil.wrapRemoteUnwrapLocal(localArgs[i], vat);
		}
	}

	protected Sender senderForMe() throws ReceiverNotFoundException {
		for(Sender sender:senderList) {
			if(sender.supportsReceiver(this)) {
				return sender;
			}
		}
		throw new ReceiverNotFoundException("receiver not found");
	}

	protected void sendIt() throws ReceiverNotFoundException {
		senderForMe().sendIt(this);
	}
	protected Object getReceiver() throws ReceiverNotFoundException {
		return senderForMe().receiver(this);
	}
	protected Class getReceiverClass() throws ReceiverNotFoundException {
		return getReceiver().getClass();
	}

	/* The object in the receiverRef slot can be 
	 *		null,
	 *		normal object,
	 *		EventualRefImpl,
	 *		NearRefImpl,
	 *		BrokenRefImpl,
	 *		Ref
	 */

	static {
		senderList.add(new Sender("sender to null") {
			public boolean supportsReceiver(MessageSend send) {
				return send.receiver == null;
			}
			public Object receiver(MessageSend send) {
				return send.receiver;
			}
		});
		senderList.add(new Sender("sender to normal object") {
			public boolean supportsReceiver(MessageSend send) {
				return !(RefUtil.isRef(send.receiver) || RefUtil.isRefImpl(send.receiver));
			}
			public Object receiver(MessageSend send) {
				return send.receiver;
			}
		});
		senderList.add(new Sender("sender to promise") {
			public boolean supportsReceiver(MessageSend send) {
				return RefUtil.isEventual(send.receiver);
			}
			public void sendIt(MessageSend send) {
				privateSendRedirectMessage(send);
			}
			public Object receiver(MessageSend send) {
				return send.receiver;
			}
		});
		senderList.add(new Sender("sender to near object") {
			public boolean supportsReceiver(MessageSend send) {
				return RefUtil.isNear(send.receiver, send.getVat()) && !(RefUtil.getRefImplActions().contains(send.getAction()));
			}

			public Object receiver(MessageSend send) {
				Object refImpl = send.receiver;
				if(RefUtil.isRef(refImpl))
					refImpl = ((Ref)refImpl).getRefImpl();
				if(send.isRefImplReceiver)
					return refImpl;
				return ((NearRefImpl)refImpl).getReceiver();
			}
		});
		senderList.add(new Sender("sender to broken") {
			public boolean supportsReceiver(MessageSend send) {
				return RefUtil.isBroken(send.receiver);
			}
			public void sendIt(MessageSend send) {
				((BrokenRefImpl)receiver(send)).getReceiver().printStackTrace();
			}
			public Object receiver(MessageSend send) {
				return send.receiver;
			}
		});
		senderList.add(new Sender("sender to ref") {
			public boolean supportsReceiver(MessageSend send) {
				return RefUtil.isRef(send.receiver);
			}
			public void sendIt(MessageSend send) {
				privateSendRedirectMessage(send);
			}
			public Object receiver(MessageSend send) {
				if(send.isRefImplReceiver)
					return ((Ref)send.receiver).getRefImpl();
				try {
					return ((Ref)send.receiver).getReceiver();
				} catch (NotResolvedException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	private static abstract class Sender {
		private String senderName;
		public Sender(String senderName) { this.senderName = senderName; }
		public String toString() { return senderName; }
		public abstract boolean supportsReceiver(MessageSend send);
		public abstract Object receiver(MessageSend send);
		public void sendIt(MessageSend send) {
			try {
				privateSendIt(send);
			} catch (Exception e) {
				e.printStackTrace();
				if(!send.isOneWay()) {
					try {
						send.resolver.smash(e);
					} catch (NotResolvedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		protected void privateSendIt(MessageSend send) {
			Object result = null;
			try {
				Object rcvr = receiver(send);
				Method myMethod = MessageSendUtil.getMethod(rcvr.getClass(), send.getAction(), send.getArgs());
				if (myMethod == null)
					throw new MethodNotFoundException("failed to find method: " + send.action);
				try {
					myMethod.setAccessible(true);
					result = myMethod.invoke(rcvr, send.getArgs());
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					if(!send.isOneWay())
						send.resolver.smash(e.getTargetException());
					return;
				}
				if(!send.isOneWay())
					send.resolver.resolve(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		protected void privateSendRedirectMessage(MessageSend send) {
			Object result = null;
			try {
				RefImpl rcvr = (RefImpl)receiver(send);
				Method myMethod = MessageSendUtil.getMethod(rcvr.getClass(), send.getAction(), send.getArgs());
				if (myMethod == null)
					throw new MethodNotFoundException("failed to find method: " + send.action);
				try {
					myMethod.setAccessible(true);
					result = myMethod.invoke(rcvr, send.getArgs());
				} catch (Exception e) {
					logger.debug("method retrieval failed: " + myMethod);
					e.printStackTrace();
					if(!send.isOneWay())
						send.resolver.resolve(e);
					return;
				}
				if(!send.isOneWay())
					send.resolver.resolve(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
