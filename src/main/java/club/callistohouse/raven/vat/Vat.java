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
package club.callistohouse.raven.vat;

import java.util.ArrayList;
import java.util.List;

import club.callistohouse.raven.refs.RefUtil;
import club.callistohouse.utils.queue.ThreadedQueue;


public class Vat {
	public static Vat getLocalVat() { return localVat; }
	private static Vat localVat = new Vat("local-vat");
	private static List<String> mirandaMethods = new ArrayList<String>();
	
	static {
		mirandaMethods.add("whenMoreResolved");
		mirandaMethods.add("whenResolved");
		mirandaMethods.add("whenBroken");
	}
 
	private String vatName;
	private ThreadedQueue queue;

	public Vat(String name) { this.vatName = name; this.queue = new ThreadedQueue(name); setup(); }

	private void setup() {
		queue.start();
		sendRunnable(new Runnable() {
			public void run() {
				RefUtil.initializeVat(Vat.this);
			}});
	}
	public void teardown() { 
		sendRunnable(new Runnable() {
			public void run() {
				RefUtil.deinitializeVat(Vat.this);
			}});
		queue.stop();
	}

	public void send(MessageSend send) { 
		send.setVat(this); 
		if(mirandaMethods.contains(send.action)) {
			sendRunnableFirst(send);
		} else {
			sendRunnable(send);
		}
	}
	public void sendRunnable(Runnable runnable) { 
		try {
			queue.put(runnable);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	public void sendRunnableFirst(Runnable runnable) { 
		try {
			queue.putFirst(runnable);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	public String getVatName() { return vatName; }
}
