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
package club.callistohouse.raven.handlers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.ProxyRefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.remote.DeliverMessage;
import club.callistohouse.raven.remote.DeliverOnlyMessage;
import club.callistohouse.raven.remote.GCAnswerMessage;
import club.callistohouse.raven.remote.GCExportMessage;
import club.callistohouse.raven.resolvers.ProxyResolver;
import club.callistohouse.raven.scope.Brand;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.scope.SealedBox;
import club.callistohouse.raven.scope.Sealer;
import club.callistohouse.raven.scope.Unsealer;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;

public class RemoteHandler {
	private static Logger log = Logger.getLogger(RemoteHandler.class);

	public Scope scope;
	public ProxyResolver resolver;
	public Integer wireId = 0;
	public Integer wireCount = 0;
	private List<MessageSend> breakageSends = new ArrayList<MessageSend>();

	public RemoteHandler(Scope scope, Integer wireId, BigInteger hash) {
		this.scope = scope;
		this.wireId = wireId;
		this.resolver = new ProxyResolver(this, hash);
	}

	public Scope getScope() { return scope; }
	public Vat getVat() { return getScope().getVat(); }
	public Integer getWireId() { return wireId; }
	public boolean isFresh() { return true; }
	public boolean isImport() { return wireId != null && wireId > 0; }
	public boolean isQuestion() { return wireId != null && wireId < 0; }

	public GCAnswerMessage gcAnswerMsg() { 
		return new GCAnswerMessage(wireId); 
	}
	public GCExportMessage gcExportMsg() { return new GCExportMessage(wireId, wireCount); }
	public void gcImportToExport() throws IOException, NotResolvedException {
		if(scope != null)
			gcExportMsg().sendMessageOnScope(scope);
		wireCount = 0;
	}
	public void gcQuestionToAnswer() throws IOException {
		if(scope != null)
			gcAnswerMsg().sendMessageOnScope(scope);
	}

	public Ref handleSend(MessageSend send) throws NotResolvedException {
		send.setVat(getScope().getServer().getVat());
		DeliverMessage msg = new DeliverMessage(wireId, send.getAction(), send.getArgs());
		if(scope != null) {
			try {
				return msg.sendMessageOnScope(scope);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public void handleOneWaySend(MessageSend send) throws NotResolvedException {
		DeliverOnlyMessage msg = new DeliverOnlyMessage(wireId, send.getAction(), send.getArgs());
		if(scope != null) {
			try {
				msg.sendMessageOnScope(scope);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void handleWhenMoreResolved(ReactorInterface reactor) throws NotResolvedException {
		if(scope == null) {
			reactor.reactToLostClient(new IOException("Invalid scope"));
		}
		if (RefUtil.isEventual(reactor)) {
			Unsealer unsealer = scope.getUnsealer();
			SealedBox box = null;
			RemoteHandler handler = null;
			try {
				box = ((ProxyRefImpl)reactor).sealedDispatch(unsealer.getBrand());
				handler = unsealer.unseal(box);
			} catch (Exception e) {
				reactor.reactToLostClient(e);
			} 
			if(handler != null && !handler.scope.equals(scope)) {
				reactor.value(resolver.getProxy());
				return;
			}
		}
		log.debug("sending #whenResolved (handler: " + this + ")");
		MessageSend send = new MessageSend(false, false, getVat(), "whenMoreResolved", reactor);
		handleOneWaySend(send);
	}
	public void handleWhenBroken(ReactorInterface reactor) {
		breakageSends.add(new MessageSend(true, false, getVat(), "whenBroken", reactor));
	}
	public void handleWhenMoreResolved(Ref reactor) throws NotResolvedException {
		if(scope == null) {
			reactor.redirectMessage("reactToLostClient", new IOException("Invalid scope"));
		}
		if (RefUtil.isEventual(reactor)) {
			Unsealer unsealer = scope.getUnsealer();
			SealedBox box = null;
			RemoteHandler handler = null;
			try {
				box = ((ProxyRefImpl)reactor.getRefImpl()).sealedDispatch(unsealer.getBrand());
				handler = unsealer.unseal(box);
			} catch (Exception e) {
				reactor.redirectMessage("reactToLostClient", e);
			} 
			if(handler != null && !handler.scope.equals(scope)) {
				reactor.redirectMessage("value", resolver.getProxy());
				return;
			}
		}
		log.debug("sending #whenResolved (handler: " + this + ")");
		MessageSend send = new MessageSend(false, false, getVat(), "whenMoreResolved", reactor);
		handleOneWaySend(send);
	}
	public void handleWhenBroken(Ref reactor) {
		breakageSends.add(new MessageSend(true, false, getVat(), "whenBroken", reactor));
	}

	public void handleResolution(Object value) throws NotResolvedException {
		if(Exception.class.isAssignableFrom(value.getClass())) {
			for(MessageSend send:breakageSends) {
				ReactorInterface reactor = (ReactorInterface) send.getArgs()[0];
				reactor.value(RefUtil.wrap(value, scope.getVat()));
			}
		}
		reactToGC();
	}
	public SealedBox handleSealedDispatch(Brand brand) {
		Sealer sealer = scope.getSealer();
		if(!brand.equals(sealer.getBrand()))
			return null;
		return sealer.seal(this);
	}

	public void reactToGC() throws NotResolvedException {
		if(isImport()) {
			try {
				log.debug("GCExport");
				gcImportToExport();
			} catch (IOException e) {}
		}
		if(isQuestion()) {
			try {
				log.debug("GCAnswer");
				gcQuestionToAnswer();
			} catch (IOException e) {}
		}
		scope = null;
	}

	public String toString() { return getClass().getSimpleName() + "(" + wireId + ")"; }
}
