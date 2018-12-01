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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;

import club.callistohouse.raven.ReactorInterface;
import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.handlers.RemoteHandler;
import club.callistohouse.raven.refs.BrokenRefImpl;
import club.callistohouse.raven.refs.FarRefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.refs.RemotePromiseRefImpl;

public class ResolverReactor implements Resolver, ReactorInterface {
	RemoteHandler handler;
	private WeakReference<Ref> ref;
	private BigInteger objectHash;

	public ResolverReactor(RemoteHandler remoteHandler, BigInteger swissHash) {
		this.handler = remoteHandler;
		this.objectHash = swissHash;
	}

	public Integer getWireId() { return handler.getWireId(); }
	public void value(Object obj) throws NotResolvedException { resolve(obj); }
	public void reactToLostClient(Exception e) throws NotResolvedException { smash(e); }
	public String toString() { return getClass().getSimpleName() + "(" + getWireId() + ")"; }

	public void resolve(final Object result) throws NotResolvedException {
		handler.scope.getVat().sendRunnable(new Runnable() {
			public void run() {
				if(ref == null)
					return;
				if(ref.get() == null)
					return;
				if(Exception.class.isAssignableFrom(result.getClass())) {
				}
				Ref newRef = RefUtil.wrap(result, handler.scope.getVat());
				try {
					if (newRef == null)
						ref.get().getRefImpl().becomeContext(null);
					else
						ref.get().getRefImpl().becomeContext(newRef.getRefImpl());
					if(RefUtil.isResolved(newRef.getRefImpl())) {
						handler = null;
						ref = null;
					}
				} catch (NotResolvedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}});
	}
	public void smash(Throwable e) throws NotResolvedException { resolve(new BrokenRefImpl(e).getProxy()); }

	public Ref getProxy() {
		if(ref != null)
			return ref.get();
		if(handler != null)
			handler.wireCount = handler.wireCount + 1;
		Ref localRef = null;
		if(objectHash == null) {
			localRef = new RemotePromiseRefImpl(handler, this).getProxy();
		} else {
			localRef = new FarRefImpl(handler, this).getProxy();
		}
		ref = new WeakReference<Ref>(localRef);
		return ref.get();
	}

	public void reactToGC() throws IOException, NotResolvedException {
		if(handler == null)
			return;
		if(ref != null)
			return;
		handler.reactToGC();
	}

	public boolean isFresh() {
		if(handler == null)
			return true;
		return handler.isFresh();
	}

	protected void finalize() throws Throwable {
	    reactToGC();
	    super.finalize();
	}
}
