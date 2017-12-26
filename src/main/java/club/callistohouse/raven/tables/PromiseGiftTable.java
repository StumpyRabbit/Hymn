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
package club.callistohouse.raven.tables;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.refs.PromiseRefImpl;
import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.raven.resolvers.Resolver;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.vat.Vine;
import club.callistohouse.raven.vat.WeakPtr;
import club.callistohouse.utils.Pair;

@SuppressWarnings("rawtypes")
public class PromiseGiftTable {

	private Ref remoteLocator; 
	private Scope scope;
	private Map<Pair<Object,BigInteger>, Pair<Object, Resolver>> gifts;

	public PromiseGiftTable(Ref remoteLocator, Scope scope) {
		this.remoteLocator = remoteLocator;
		this.scope = scope;
		gifts = new HashMap<Pair<Object,BigInteger>, Pair<Object, Resolver>>();
	}

	public int size() { return gifts.size(); }

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "<" + hashCode() + ">");
		for(Entry entry:gifts.entrySet()) {
			builder.append(",\n\t\t");
			builder.append("(");
			builder.append(entry.getKey() + ", " + entry.getValue());
			builder.append(")");
		}
		return builder.toString();
	}

	public Object provideFor(BigInteger receiverVatId, Object gift, BigInteger nonce) throws NotResolvedException {
		Pair<Object,BigInteger> keyPair = buildKeyPair(receiverVatId, nonce);
		Pair<Object, Resolver> valuePair = gifts.get(keyPair);
		Vine vine = new Vine(null);
        @SuppressWarnings("unused")
		WeakPtr weakPtr = new WeakPtr(this, scope.getVat(), "drop", keyPair);
		if(valuePair == null) {
			valuePair = new Pair<Object, Resolver>(gift, null); 
			gifts.put(keyPair, valuePair);
		} else {
			valuePair.second().resolve(gift);
			gifts.remove(keyPair);
		}
		return vine;
	}

	public Object provideFor(String receiverNickname, Object gift, BigInteger nonce) throws NotResolvedException {
		Pair<Object,BigInteger> keyPair = buildKeyPair(receiverNickname, nonce);
		Pair<Object, Resolver> valuePair = gifts.get(keyPair);
		Vine vine = new Vine(null);
        @SuppressWarnings("unused")
		WeakPtr weakPtr = new WeakPtr(this, scope.getVat(), "drop", keyPair);
		if(valuePair == null) {
			valuePair = new Pair<Object, Resolver>(gift, null); 
			gifts.put(keyPair, valuePair);
		} else {
			valuePair.second().resolve(gift);
			gifts.remove(keyPair);
		}
		return vine;
	}

	public Object acceptFor(String nickname, BigInteger nonce) throws NotResolvedException {
		if(nickname == null) {
			return null;
		}
		Pair<Object, BigInteger> keyPair = buildKeyPair(nickname, nonce);
		Pair<Object, Resolver> valuePair = gifts.get(keyPair);
		if(valuePair == null) {
			Pair<PromiseRefImpl, Resolver> promiseValuePair = RefUtil.promise(scope.getVat());
			valuePair = new Pair<Object, Resolver>(promiseValuePair.first(), promiseValuePair.second());
			gifts.put(keyPair, valuePair);
			Vine vine = new Vine(null);
            @SuppressWarnings("unused")
			WeakPtr weakPtr = new WeakPtr(this, scope.getVat(), "drop", keyPair);
			remoteLocator.redirectMessageOneWay("ignore", vine);
		} else {
			gifts.remove(keyPair);
		}
		return valuePair.first();
	}

	public Object acceptFor(BigInteger vatId, BigInteger nonce) throws NotResolvedException {
		if(vatId == null) {
			return null;
		}
		Pair<Object, BigInteger> keyPair = buildKeyPair(vatId, nonce);
		Pair<Object, Resolver> valuePair = gifts.get(keyPair);
		if(valuePair == null) {
			Pair<PromiseRefImpl, Resolver> promiseValuePair = RefUtil.promise(scope.getVat());
			valuePair = new Pair<Object, Resolver>(promiseValuePair.first(), promiseValuePair.second());
			gifts.put(keyPair, valuePair);
			Vine vine = new Vine(null);
            @SuppressWarnings("unused")
			WeakPtr weakPtr = new WeakPtr(this, scope.getVat(), "drop", keyPair);
			remoteLocator.redirectMessageOneWay("ignore", vine);
		} else {
			gifts.remove(keyPair);
		}
		return valuePair.first();
	}

	public void drop(Pair<BigInteger,BigInteger> keyPair) throws NotResolvedException {
		Pair<Object, Resolver> valuePair = gifts.get(keyPair);
		if(valuePair == null)
			return;
		Resolver resolver = valuePair.second();
		if(resolver != null)
			resolver.smash(new IOException("The vine was dropped: " + keyPair.second().toString()));
		gifts.remove(keyPair);
	}

	protected Pair<Object, BigInteger> buildKeyPair(Object vatId, BigInteger nonce) {
		return new Pair<Object,BigInteger>(vatId, nonce);
	}

	public void smash(Exception e) {
		for(Object value:gifts.values()) {
			if(value != null)
				smashObject(value, e);
		}
		this.gifts = null;
	}

	private void smashObject(Object obj, Exception e) {
		Method m = null;
		try {
			m = obj.getClass().getDeclaredMethod("smash", Exception.class);
		} catch (NoSuchMethodException e1) {
			return;
		} catch (SecurityException e1) {
			e1.printStackTrace();
		}
		try {
			m.invoke(obj, e);
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
		}
	}
}
