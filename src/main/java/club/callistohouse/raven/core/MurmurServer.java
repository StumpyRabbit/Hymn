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
package club.callistohouse.raven.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import club.callistohouse.raven.scope.Brand;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.raven.scope.Sealer;
import club.callistohouse.raven.scope.Unsealer;
import club.callistohouse.raven.tables.SwissTable;
import club.callistohouse.raven.vat.Vat;
import club.callistohouse.session.Session;
import club.callistohouse.session.Session.Connected;
import club.callistohouse.session.Session.Disconnected;
import club.callistohouse.session.Session.Identified;
import club.callistohouse.session.SessionAgent;
import club.callistohouse.session.SessionAgent.Started;
import club.callistohouse.session.SessionAgent.Stopped;
import club.callistohouse.session.SessionIdentity;
import club.callistohouse.utils.MapUtil;
import club.callistohouse.utils.Pair;

public class MurmurServer {

	private Vat vat;
	private SwissTable swissTable;
	private Sealer sealer;
	private Unsealer unsealer;
	private SessionAgent sessionServer;
	private Map<String,MurmurTerminal> terminalsByVatId = new HashMap<String, MurmurTerminal>();

	public MurmurServer(SessionIdentity id, SwissTable swissTable) throws FileNotFoundException, ClassNotFoundException, IOException {
		this.swissTable = swissTable;
		this.sessionServer = new SessionAgent(id);
		Pair<Sealer,Unsealer> pair = Brand.pair(id.getVatId().toString());
		this.sealer = pair.first();
		this.unsealer = pair.second();
		this.vat = new Vat(id.getDomain() + "-vat");
		setupListenersOnSessionServer();
	}

	public SwissTable getSwissTable() { return swissTable; }
	public Sealer getSealer() { return sealer; }
	public Unsealer getUnsealer() { return unsealer; }
	public Vat getVat() { return vat; }

	public void start() { sessionServer.start(); }
	public void stop() { sessionServer.stop(); }

	private void setupListenersOnSessionServer() {
		sessionServer.addListener(new Listener<Started>(Started.class) {
			protected void handle(Started event) {
			}});
		sessionServer.addListener(new Listener<Stopped>(Stopped.class) {
			protected void handle(Stopped event) {
			}});
		sessionServer.addListener(new Listener<Connected>(Connected.class) {
			protected void handle(Connected event) {
				MurmurTerminal tempMurmurTerm = MurmurServer.this.getTerminalByRemoteVatId(event.terminal.getFarKey().getVatId());
				if(tempMurmurTerm == null) {
					tempMurmurTerm = new MurmurTerminal(MurmurServer.this);
				}
				final MurmurTerminal murmurTerm = tempMurmurTerm;
				murmurTerm.setScope(new Scope(murmurTerm, getSwissTable()));
				murmurTerm.setSessionTerminal(event.terminal);
				event.terminal.addListener(new Listener<Identified>(Identified.class) {
					public void handle(Identified event) {
						terminalsByVatId.put(murmurTerm.getScope().getRemoteVatId(), murmurTerm);
					}
				});
			}});
		sessionServer.addListener(new Listener<Disconnected>(Disconnected.class) {
			protected void handle(Disconnected event) {
				MurmurTerminal pauwauTerm = getTerminalByRemoteVatId(event.terminal.getFarKey().getVatId());
				if(pauwauTerm != null) {
					pauwauTerm.getScope().smash();
					terminalsByVatId.remove(event.terminal.getFarKey().getVatId());
				}
			}});
	}

	public String toString() {
		return MapUtil.printMaps(new StringBuilder(), 2, getClass().getSimpleName() 
				+ "<hashCode: " + hashCode() 
				+ ", vatId: " + sessionServer.getNearKey().getVatId() 
				+ ">:  ", terminalsByVatId);
	}

	public Object lookupSwiss(BigInteger swissNumber) { return swissTable.lookupSwiss(swissNumber); }

	public MurmurTerminal getTerminal(SessionIdentity id) {
		MurmurTerminal murmurTerm = getTerminalByRemoteVatId(id.getVatId());
		if(murmurTerm != null) {
			return murmurTerm;
		}
		murmurTerm = new MurmurTerminal(this);
		if(id.getVatId() != null) {
			terminalsByVatId.put(id.getVatId(), murmurTerm);
		} else {
			throw new IllegalArgumentException("bad id");
		}
		Session sess = sessionServer.connect(id);
		murmurTerm.setSessionTerminal(sess);
		murmurTerm.setScope(new Scope(murmurTerm, getSwissTable()));
		return murmurTerm;
	}

	public MurmurTerminal getTerminalByRemoteVatId(String vatId) {
		if(vatId == null) {
			return null;
		}
		return terminalsByVatId.get(vatId);
	}
}
