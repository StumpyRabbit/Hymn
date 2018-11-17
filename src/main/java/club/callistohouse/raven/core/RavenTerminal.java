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

import java.io.IOException;

import org.apache.log4j.Logger;

import club.callistohouse.raven.exceptions.NotResolvedException;
import club.callistohouse.raven.remote.RavenMessage;
import club.callistohouse.raven.scope.Scope;
import club.callistohouse.session.Session;
import club.callistohouse.session.Session.DataReceived;
import club.callistohouse.session.payload.Frame;
import club.callistohouse.session.payload.RawData;
import club.callistohouse.session.protocol.ThunkLayer;
import club.callistohouse.session.protocol.ThunkStack;
import club.callistohouse.utils.events.Listener;

public class RavenTerminal extends ThunkLayer {
	private static Logger log = Logger.getLogger(RavenTerminal.class);

	private RavenServer server;
	private Session sessionTerminal;
	private Scope scope;
	private ThunkStack stack;

	public RavenTerminal(RavenServer server) {
		this.server = server;
	}

	public void setStack(ThunkStack aStack) { stack = aStack;}
	public RavenServer getServer() { return server; }
	public Scope getScope() { return scope; }
	public void setScope(Scope scope) {
		if(this.scope == null)
			this.scope = scope;
	}
	public Session getSessionTerminal() { return sessionTerminal; }
	public void setSessionTerminal(Session term) {
		if(sessionTerminal == null) {
			this.sessionTerminal = term;
/*			sessionTerminal.addListener(
					new Listener<DataReceived>(DataReceived.class) {
						public void handle(final DataReceived msg) { 
							getScope().getVat().sendRunnable(new Runnable() {
								public void run() {
									receiveSessionData(msg);
								}}); 
						} });*/
		}
	}

	public String toString() {
		return getClass().getSimpleName() + "<hashcode: " + hashCode() + ">";
	}

	public void sendMsg(RavenMessage msg) throws IOException {
		log.debug("sending message: " + msg);
		Frame frame = new Frame(new RawData());
		frame.setPayload(msg);
		stack.downcall(frame, this);
	}

	public Object receiveSessionData(RavenMessage ravenMsg) {
		try {
			ravenMsg.receiveMessageOnScope(getScope());
			log.debug("received message: " + ravenMsg);
		} catch (NotResolvedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ravenMsg;
	}

	protected Object downThunk(Frame frame) { return frame.getPayload(); }
	protected Object upThunk(Frame frame) { return receiveSessionData((RavenMessage) frame.getPayload()); }
}
