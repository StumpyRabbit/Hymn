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
package club.callistohouse.raven.descriptors;

import java.math.BigInteger;
import java.net.InetSocketAddress;

import club.callistohouse.raven.Ref;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.vat.Vine;
import club.callistohouse.session.parrotttalk.SessionIdentity;

public class Promise3Desc extends ObjectRefDesc {
	private static final long serialVersionUID = 6883656806105142634L;

	private String nickname;
	private InetSocketAddress isa;
	private String vatId;
	private BigInteger nonce;
	private Object vine;

	public Promise3Desc(String nickname, InetSocketAddress isa, String vatId, BigInteger nonce, Object vine) {
		this.nickname = nickname;
		this.isa = isa;
		this.vatId = vatId;
		this.nonce = nonce;
		this.vine = vine;
	}

	public String getVatId() { return vatId; }
	public BigInteger getNonce() { return nonce; }
	public Object getVine() { return vine; }
	public Object wrapNewVine() { return new Vine((Ref)getVine()); }

	public SessionIdentity asSessionIdentity() {
		return new SessionIdentity(nickname, isa, getVatId());
	}

	public String toString() {
		return getClass().getSimpleName() + "("
				+ asSessionIdentity() + ", "
				+ getNonce() + ")";
				
	}

	@Override
	public Object getEventualFromScope(Scope scope) throws NotResolvedException {
		return scope.rendezvousForPromise3Desc(this);
	}
}
