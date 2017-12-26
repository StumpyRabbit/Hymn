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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.remote.MurmurInputStream;
import club.callistohouse.raven.remote.MurmurMessage;
import club.callistohouse.raven.remote.MurmurOutputStream;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.session.Session;
import club.callistohouse.session.Session.DataReceived;

public class MurmurTerminal {
	private static Logger log = Logger.getLogger(MurmurTerminal.class);

	private MurmurServer server;
	private Session sessionTerminal;
	private Scope scope;

	public MurmurTerminal(MurmurServer server) {
		this.server = server;
	}

	public MurmurServer getServer() { return server; }
	public Scope getScope() { return scope; }
	public void setScope(Scope scope) {
		if(this.scope == null)
			this.scope = scope;
	}
	public Session getSessionTerminal() { return sessionTerminal; }
	public void setSessionTerminal(Session term) {
		if(sessionTerminal == null) {
			this.sessionTerminal = term;
			sessionTerminal.addListener(
					new Listener<DataReceived>(DataReceived.class) {
						public void handle(final DataReceived msg) { 
							getScope().getVat().sendRunnable(new Runnable() {
								public void run() {
									receiveSessionData(msg);
								}}); 
						} });
		}
	}

	public String toString() {
		return getClass().getSimpleName() + "<hashcode: " + hashCode() + ">";
	}

	@SuppressWarnings({ "resource" })
	public void sendMsg(MurmurMessage msg) throws IOException {
		log.debug("sending message: " + msg);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new MurmurOutputStream(baos, getScope()).writeObject(msg);
		getSessionTerminal().send(baos.toByteArray());
	}
	@SuppressWarnings({ "resource" })
	public void receiveSessionData(DataReceived sessionData) {
		ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) sessionData.data);
		try {
			MurmurMessage msg = (MurmurMessage)new MurmurInputStream(bais, getScope()).readObject();
			log.debug("receiving message: " + msg);
			msg.receiveMessageOnScope(getScope());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NotResolvedException e) {
			e.printStackTrace();
		}
	}
}
