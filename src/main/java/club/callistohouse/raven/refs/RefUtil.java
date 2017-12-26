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

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import club.callistohouse.raven.PassByCopy;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.descriptors.ObjectRefDesc;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.exceptions.SealedException;
import club.callistohouse.raven.handlers.RemoteHandler;
import club.callistohouse.raven.resolvers.LocalResolver;
import club.callistohouse.raven.resolvers.Redirector;
import club.callistohouse.raven.resolvers.Resolver;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.scope.SealedBox;
import club.callistohouse.raven.scope.Unsealer;
import club.callistohouse.raven.vat.MessageSend;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.utils.Pair;


/**
 * Base -> A.obj1.foo(B.obj2)
 * Base -> A: DeliverMessage(receiverDesc(obj:!B), action, args(obj:B), answerPromise, redirector)
 * 		Base:scope.newPromise3DescriptorHandler()
 * 			Base -> A: Promise3Desc(B.obj.nonce, B's remoteTcpId + remoteVatId)
 * 			Base -> B.locator.provideFor(A's remoteVatId)
 * A:scope.rendezvousForPromise3Desc(Promise3Desc(B.obj.nonce, B's remoteTcpId + remoteVatId))
 * 		A -> B: B.locator.acceptFrom(remoteVatId)
 * 			A.connectTerminal(B)
 *
 */
public class RefUtil {
	private static final ThreadLocal<Vat> context = new ThreadLocal<Vat>();
//	private static Logger log = Logger.getLogger(RefUtil.class);

	public static void initializeVat(Vat vat) {
		context.set(vat);
	}
	public static void deinitializeVat(Vat vat) {
		if(getVat().equals(vat)) {
			context.remove();
		}
	}
	public static Vat getVat() { 
		if(context.get() == null) {
			initializeVat(Vat.getLocalVat());
		}
		return context.get();
	}
	public static Ref wrap(Object obj) {
		return wrap(obj, getVat());
	}
	
	public static Ref wrap(Object obj, Vat vat) {
		if(obj == null)
			return null;
		if(Ref.class.isAssignableFrom(obj.getClass())) {
			return (Ref)obj;
		} else if(RefImpl.class.isAssignableFrom(obj.getClass())) {
			return ((RefImpl)obj).getProxy();
		} else if(Exception.class.isAssignableFrom(obj.getClass())) {
			return new BrokenRefImpl((Exception)obj).getProxy();
		} else {
			return new NearRefImpl(obj, vat).getProxy();
		}
	}
	public static Object wrapRemoteUnwrapLocal(Object obj, Vat vat) {
		if(obj == null)
			return null;
		if(Ref.class.isAssignableFrom(obj.getClass())) {
			return (Ref)obj;
		} else if(RefImpl.class.isAssignableFrom(obj.getClass())) {
			if(isLocal(obj)) {
				return unwrap(obj);
			} else {
				return ((RefImpl)obj).getProxy();
			}
		} else if(Exception.class.isAssignableFrom(obj.getClass())) {
			return new BrokenRefImpl((Exception)obj).getProxy();
		} else {
			return obj;
		}
	}
	public static Object unwrap(Object obj) {
		return unwrap(obj, getVat());
	}
	public static Object unwrap(Object obj, Vat unwrapVat) {
		if(obj == null)
			return null;
		RefImpl impl = null;
		if(Ref.class.isAssignableFrom(obj.getClass())) {
			impl = ((Ref)obj).getRefImpl();
		} else if(RefImpl.class.isAssignableFrom(obj.getClass())) {
			impl = (RefImpl) obj;
		} else {
			return obj;
		}
		if(isNear(impl, unwrapVat)) {
			try {
				return ((RefImpl)impl).getReceiver();
			} catch (NotResolvedException e) {
				throw new RuntimeException("near ref unresolved");
			}
		} else {
			return impl;
		}
	}
	public static Pair<PromiseRefImpl, Resolver> promise(Vat vat) {
		List<MessageSend> buffer = new ArrayList<MessageSend>();
		PromiseRefImpl promise = new PromiseRefImpl(buffer, vat);
		LocalResolver resolver = new LocalResolver(promise.getProxy(), buffer);
		return new Pair<PromiseRefImpl,Resolver>(promise, (Resolver) resolver);
	}

	public static List<String> getRefImplActions() {
		List<String> list = new ArrayList<String>();
		list.add("whenResolved");
		list.add("whenMoreResolved");
		list.add("whenBroken");
		return list;
	}

	public static boolean isPassByReference(Object obj) { return !isPassByCopy(obj); }

	public static boolean isPassByCopy(Object obj) {
		if(isRef(obj) && isNear(obj)) {
			return isPassByCopy((NearRefImpl)((Ref)obj).getRefImpl());
		}
		return Serializable.class.isAssignableFrom(obj.getClass()) 
				|| Externalizable.class.isAssignableFrom(obj.getClass())
				|| PassByCopy.class.isAssignableFrom(obj.getClass());
	}

	public static boolean isRef(Object obj) { return Ref.class.isAssignableFrom(obj.getClass()); }
	public static boolean isRefImpl(Object obj) { return isAssignableFrom(obj, RefImpl.class); }
	public static boolean isException(Object obj) { return isAssignableFrom(obj, Exception.class); }
	public static boolean isEventual(Object obj) { return isAssignableFrom(obj, ProxyRefImpl.class, PromiseRefImpl.class); }
	public static boolean isLocal(Object obj) { return !isRemote(obj); }
	public static boolean isRemote(Object obj) {
		if(isRef(obj))
			return isAssignableFrom(((Ref)obj).getRefImpl(), ProxyRefImpl.class);
		return isAssignableFrom(obj, ProxyRefImpl.class);
	}
	private static boolean isNear(Object obj) { return isAssignableFrom(obj, NearRefImpl.class); }
	public static boolean isNear(Object obj, Vat nearVat) {
		if(!isNear(obj)) {
			return false;
		}
		if(isRef(obj)) {
			return ((NearRefImpl)((Ref)obj).getRefImpl()).getVat().equals(nearVat); 
		} else if(isRefImpl(obj)) {
			return ((NearRefImpl)obj).getVat().equals(nearVat); 
		} else {
			return false;
		}
	}
	public static boolean isResolved(Object obj) { return !isPromise(obj); }
	public static boolean isPromise(Object obj) { return isAssignableFrom(obj, PromiseRefImpl.class, RemotePromiseRefImpl.class); }
	public static boolean isBroken(Object obj) { return isAssignableFrom(obj, BrokenRefImpl.class, UnconnectedRefImpl.class); }
	public static boolean isEventualDescriptor(Object obj) { return isAssignableFrom(obj, ObjectRefDesc.class); }
	public static boolean isResolver(Object obj) { return isAssignableFrom(obj, Resolver.class); }
	public static boolean isRedirector(Object obj) { return isAssignableFrom(obj, Redirector.class); }
	public static boolean isLocalToScope(Scope scope, Ref ref) throws IOException, SealedException {
		Unsealer unsealer = scope.getUnsealer();
		SealedBox box = ref.getRefImpl().sealedDispatch(unsealer.getBrand());
		RemoteHandler targetHandler = unsealer.unseal(box);
		return targetHandler != null && scope.equals(targetHandler.getScope());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean isAssignableFrom(Object obj, Class...classes) {
		Object realObject = obj;
		if(realObject == null)
			return false;
		if(isRef(obj))
			realObject = ((Ref)obj).getRefImpl();
		for(Class cls:classes) {
			if(cls.isAssignableFrom(realObject.getClass()))
				return true;
		}
		return false;
	}
}
