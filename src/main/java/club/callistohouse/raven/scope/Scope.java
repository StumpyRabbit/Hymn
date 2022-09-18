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
package club.callistohouse.raven.scope;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.DigestException;

import club.callistohouse.raven.Ref;
import club.callistohouse.raven.core.RavenServer;
import club.callistohouse.raven.core.RavenTerminal;
import club.callistohouse.raven.descriptors.FarDesc;
import club.callistohouse.raven.descriptors.ImportDesc;
import club.callistohouse.raven.descriptors.IncomingDesc;
import club.callistohouse.raven.descriptors.ObjectRefDesc;
import club.callistohouse.raven.descriptors.Promise3Desc;
import club.callistohouse.raven.descriptors.RedirectorDesc;
import club.callistohouse.raven.descriptors.RemotePromiseDesc;
import club.callistohouse.raven.descriptors.ResolverDesc;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.exceptions.SealedException;
import club.callistohouse.raven.handlers.FarHandler;
import club.callistohouse.raven.handlers.LookupHandler;
import club.callistohouse.raven.handlers.RemoteHandler;
import club.callistohouse.raven.handlers.RemotePromiseHandler;
import club.callistohouse.raven.refs.ProxyRefImpl;
import club.callistohouse.raven.refs.RefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.remote.RavenMessage;
import club.callistohouse.raven.resolvers.ResolverReactor;
import club.callistohouse.raven.tables.ClientTable;
import club.callistohouse.raven.tables.ProviderTable;
import club.callistohouse.raven.tables.SwissTable;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.raven.vat.Vine;
import club.callistohouse.session.SessionIdentity;
import club.callistohouse.utils.Base64Encoder;

public class Scope {
//	static Logger log = Logger.getLogger(Scope.class);

	private ClientTable imports = new ClientTable("imports table", false);
	private ProviderTable exports = new ProviderTable("exports table", false);
	private ClientTable questions = new ClientTable("questions table", true);
	private ProviderTable answers = new ProviderTable("answers table", true);

	private SwissTable table;
	private NonceLocator localLocator;
	private Ref remoteLocator;
	private RavenTerminal terminal;

	public Scope(RavenTerminal term, SwissTable table) {
		this.table = table;
		this.terminal = term;
		this.localLocator = new NonceLocator(this, table);
		this.remoteLocator = new LookupHandler(this).resolver.getProxy();
	}

	public String toString() {
		return getClass().getSimpleName() + "(hashcode: " + hashCode() + " server: " + getServer() + ")"; 
	}

	public NonceLocator getLocalLocator() { return localLocator; }
	public Ref getRemoteLocator() { return remoteLocator; }
	public Sealer getSealer() { return getServer().getSealer(); }
	public Unsealer getUnsealer() { return getServer().getUnsealer(); }
	public SwissTable getSwissTable() { return table; }
	public RavenTerminal getTerminal() { return terminal; }
	public RavenServer getServer() { return terminal.getServer(); }
	public Vat getVat() {
		if(getServer() == null)
			return null;
		return getServer().getVat(); 
	}
	public SessionIdentity getLocalIdentity() { return terminal.getSessionTerminal().getNearKey(); }
	public SessionIdentity getRemoteIdentity() { return terminal.getSessionTerminal().getFarKey(); }
	public String getRemoteVatId() { return getRemoteIdentity().getVatId(); }

	public Object externalize(Object obj) throws DigestException, NotResolvedException, IOException, SealedException {
		Object realObject = RefUtil.unwrap(obj, getVat());
		if (RefUtil.isEventual(realObject)) {
			return makeEventualDescriptor((ProxyRefImpl) realObject);
		} else  if (RefUtil.isRedirector(realObject)) {
			return newRedirectorDescriptor(RefUtil.wrap(realObject, getVat()).getRefImpl());
		} else  if (RefUtil.isResolver(realObject)) {
			return newResolverDescriptor(RefUtil.wrap(realObject, getVat()).getRefImpl());
		} else if (RefUtil.isEventualDescriptor(realObject)) {
			return realObject;
		} else if (RefUtil.isPassByCopy(realObject)) {
			return realObject;
		} else {
			return makeImportDescriptor(realObject);
		}
	}

	public Object internalize(Object obj) {
		if(obj == null)
			return null;
		if (RefUtil.isEventualDescriptor(obj)) {
			try { 
				return RefUtil.wrap(((ObjectRefDesc) obj).getEventualFromScope(this), getVat());
			} catch (Exception e) {
				return RefUtil.wrap(e, getVat());
			}
		}
		return obj;
	}

	public RavenTerminal getTerminal(SessionIdentity remoteId) { return getServer().getTerminal(remoteId); }

	private ObjectRefDesc makeEventualDescriptor(ProxyRefImpl proxy) throws IOException, SealedException, NotResolvedException {
		RemoteHandler handler = handlerFrom(proxy);
		// RemoteHandler.
		if (handler == null)
			return newRemotePromiseDescriptor(proxy);
		if (handler.scope.equals(this)) {
			return new IncomingDesc(handler.wireId);
		} else {
			return newPromise3DescriptorHandler(proxy, handler);
		}
	}

	private RemoteHandler handlerFrom(RefImpl proxy) throws SealedException, IOException {
		Unsealer unsealer = getUnsealer();
		SealedBox box = proxy.sealedDispatch(unsealer.getBrand());
		RemoteHandler handler = unsealer.unseal(box); // unseal: box type:
		return handler;
	}

	private ObjectRefDesc makeImportDescriptor(Object obj) throws DigestException, NotResolvedException, IOException {
		RefImpl refImpl = RefUtil.wrap(obj, getVat()).getRefImpl();
		Integer wirePosition = indexOfExport(refImpl.getProxy());
		if (wirePosition == null)
			return newFarDescriptor(refImpl);
		incrementExportWirePosition(wirePosition);
		return new ImportDesc(wirePosition);
	}

	private ObjectRefDesc newFarDescriptor(RefImpl promise) throws DigestException, NotResolvedException, IOException {
		BigInteger swiss = getSwissTable().getIdentity(promise);
		BigInteger hash = Base64Encoder.toBase64(swiss);
		Integer wirePosition = registerExport(promise.getProxy());
		return new FarDesc(wirePosition, hash);
	}

	private ObjectRefDesc newResolverDescriptor(RefImpl promise) throws DigestException, NotResolvedException, IOException {
		try {
			RemoteHandler handler = handlerFrom(promise);
			if ((handler == null) || handler.scope.equals(this)) {
				BigInteger swiss = getSwissTable().getIdentity(promise);
				BigInteger hash = Base64Encoder.toBase64(swiss);
				Integer wirePosition = registerExport(promise.getProxy());
				return new ResolverDesc(wirePosition, hash);
			} else {
				return newPromise3DescriptorHandler(promise, handler);
			}
		} catch (SealedException e) {
			BigInteger swiss = getSwissTable().getIdentity(promise);
			BigInteger hash = Base64Encoder.toBase64(swiss);
			Integer wirePosition = registerExport(promise.getProxy());
			return new ResolverDesc(wirePosition, hash);
		}
	}

	private ObjectRefDesc newRedirectorDescriptor(RefImpl promise) throws DigestException, NotResolvedException, IOException {
		try {
			RemoteHandler handler = handlerFrom(promise);
			if ((handler == null) || handler.scope.equals(this)) {
				BigInteger swiss = getSwissTable().getIdentity(promise);
				BigInteger hash = Base64Encoder.toBase64(swiss);
				Integer wirePosition = registerExport(promise.getProxy());
				return new RedirectorDesc(wirePosition, hash);
			} else {
				return newPromise3DescriptorHandler(promise, handler);
			}
		} catch (SealedException e) {
			BigInteger swiss = getSwissTable().getIdentity(promise);
			BigInteger hash = Base64Encoder.toBase64(swiss);
			Integer wirePosition = registerExport(promise.getProxy());
			return new RedirectorDesc(wirePosition, hash);
		}
	}

	private ObjectRefDesc newRemotePromiseDescriptor(RefImpl promise) throws IOException, NotResolvedException {
		Integer wirePosition = registerExport(promise.getProxy());
		BigInteger base = getSwissTable().generateSwiss();
		BigInteger num = Base64Encoder.toBase64(base);
		BigInteger hash = Base64Encoder.toBase64(num);
		FarHandler handler = new FarHandler(this, wirePosition, hash);
		ResolverReactor resolver = handler.resolver;
		Integer redirectorPosition = registerQuestion(resolver);
		handler.wireId = redirectorPosition;
		promise.whenMoreResolved(resolver);
		RemotePromiseDesc desc = new RemotePromiseDesc(wirePosition);
		desc.setRedirectorPosition(redirectorPosition);
		desc.setRedirectorBase(base);
		return desc;
	}

	// object A in Vat A sending a msg to object B in Vat B, passing object C in Vat C as a Promise3Desc
	private ObjectRefDesc newPromise3DescriptorHandler(Object promise, RemoteHandler remoteHostHandler) throws IOException, NotResolvedException {
		Scope remoteHostScope = remoteHostHandler.scope;
		BigInteger nonce = getSwissTable().generateSwiss();
		String recipientNickname = getRemoteIdentity().getDomain();
		String recipientVatId = getRemoteIdentity().getVatId();
		String providerVatId = remoteHostScope.getRemoteIdentity().getVatId();
		InetSocketAddress providerIsa = remoteHostScope.getRemoteIdentity().getSocketAddress();
		MessageSend send = new MessageSend(false, true, getVat(),
				"provideFor", 
				recipientVatId, 
				promise, 
				nonce);
		Ref result = remoteHostScope.getRemoteLocator().redirectMessage(send);
		return new Promise3Desc(
				recipientNickname,
				providerIsa,
				providerVatId,
				nonce, 
				new Vine(result));
	}

	// object B in Vat B receiving Vat A's call, passing object C in Vat C as a Promise3Desc
	public Ref rendezvousForPromise3Desc(Promise3Desc promise3Desc) throws NotResolvedException {
		if (promise3Desc.getVine() == null) return null;
		// scope for C
		Scope scope = getTerminal(promise3Desc.asSessionIdentity()).getScope();
		MessageSend send = new MessageSend(false, true, getVat(), 
				"acceptFrom", 
				getRemoteIdentity(),
				promise3Desc.getNonce(), 
				promise3Desc.wrapNewVine());
		return scope.getRemoteLocator().redirectMessage(send);
	}

	public RemotePromiseHandler makeQuestion() throws IOException { 
		RemotePromiseHandler handler = new RemotePromiseHandler(this);
		ResolverReactor resolver = handler.resolver;
		Integer id = registerQuestion(resolver);
		handler.wireId = id;
		return handler;
	}

	public void send(RavenMessage murmurMessage) throws IOException {
		terminal.sendMsg(murmurMessage);
	}


//	public static void dumpStack() { try { throw new RuntimeException("stack dump"); } catch (RuntimeException e) { log.debug(e); e.printStackTrace(); } }

	public Integer indexOfExport(Ref ref) { return exports.indexOf(ref); }
	public Ref exportAt(Integer wireId) throws IOException { return (Ref) exports.get(wireId); }
	public void incrementExportWirePosition(int wirePosition) { exports.increment(wirePosition); }
	public void decrementExportWirePositionCount(int wirePosition, int count) { if (exports.size() > 0) exports.decrement(wirePosition, count); }
	public int registerExport(Ref object) throws IOException { return exports.bind(object); }

	public Ref answerAt(Integer wireId) throws IOException { return (Ref) answers.get(wireId); }
	public void deregisterAnswerAtWirePosition(int wirePosition) { if (answers.size() > 0) answers.decrement(wirePosition, 1); }
	public void registerAnswerAtWirePosition(Ref object, Integer wirePosition) throws IOException { answers.put(wirePosition, object); }

	public void deregisterQuestionWirePosition(int wirePosition) { if (questions.size() > 0) questions.decrement(wirePosition, 1); }
	public int registerQuestion(ResolverReactor resolver) throws IOException { return questions.bind(resolver); }

	public ResolverReactor importAt(Integer wireId) throws IOException { return (ResolverReactor) imports.get(wireId); }
	public void registerImportAtWirePosition(ResolverReactor resolver, Integer wirePosition) throws IOException { imports.put(wirePosition, resolver); }

	public void smash() throws RuntimeException {
		try {
			questions.smash(null);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			answers.smash(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			imports.smash(null);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			exports.smash(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		localLocator.smash(null);
	}
}
