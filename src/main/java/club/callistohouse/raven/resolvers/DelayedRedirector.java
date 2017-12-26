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

import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.exceptions.SealedException;
import club.callistohouse.raven.refs.PromiseRefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.utils.Pair;

public class DelayedRedirector extends Redirector {
	private static final long serialVersionUID = -1158537689908300949L;

	public DelayedRedirector(ProxyResolver resolver) {
		super(resolver);
	}

	public void value(Object obj) throws NotResolvedException {
		if(resolver == null)
			return;

		if(resolver.isFresh() || RefUtil.isLocal(obj)) {
			resolver.resolve(obj);
			resolver = null;
			return;
		}

		Ref ref = (Ref)obj;
		try {
			if(RefUtil.isLocalToScope(resolver.handler.scope, ref)) {
				resolver.resolve(obj);
				resolver = null;
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			resolver = null;
			return;
		} catch (SealedException e) {
			e.printStackTrace();
			resolver = null;
			return;
		}

		Pair<PromiseRefImpl, Resolver> pair = RefUtil.promise(ref.getVat());
		Redirector redirector = new Redirector((ProxyResolver) pair.second());
		resolver.getProxy().whenResolved(redirector);
		resolver.resolve(pair.first());
		resolver = null;
	}
}
