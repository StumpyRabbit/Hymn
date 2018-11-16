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

import club.callistohouse.raven.core.RavenTerminal;
import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.tables.NearGiftTable;
import club.callistohouse.raven.tables.PromiseGiftTable;
import club.callistohouse.raven.tables.SwissTable;
import club.callistohouse.session.SessionIdentity;

public class NonceLocator {

	private Scope scope;
	private SwissTable swissTable;
	private PromiseGiftTable promiseGifts; 
	private NearGiftTable nearGifts = new NearGiftTable();

	public NonceLocator(Scope scope, SwissTable swissTable) {
		this.scope = scope;
		this.swissTable = swissTable;
		this.promiseGifts = new PromiseGiftTable(scope.getRemoteLocator(), scope);
	}

	public Object lookupSwiss(BigInteger swissNumber) { return swissTable.lookupSwiss(swissNumber); 	}
	public void ignore(Object vine) {}

	public Object provideFor(BigInteger giftRecipientVatId, Object gift, BigInteger nonce) throws NotResolvedException {
		return promiseGifts.provideFor(giftRecipientVatId, gift, nonce);
	}

	public Object acceptFrom(SessionIdentity remoteId, BigInteger nonce, Object vine) throws IOException, NotResolvedException {
		RavenTerminal term = scope.getTerminal(remoteId);
		PromiseGiftTable donorTable = term.getScope().getLocalLocator().promiseGifts;
		return donorTable.acceptFor(scope.getRemoteIdentity().getVatId(), nonce);
	}

	public void smash(Exception e) { promiseGifts.smash(e); promiseGifts = null; }

	public String toString() {
		return getClass().getSimpleName() + "(swissTable: " + swissTable.size() + " objects" 
				+ ", promiseGifts: " + promiseGifts.size() + " gifts"
				+ ", nearGifts: " + nearGifts.size() + " gifts)";
	}
}
