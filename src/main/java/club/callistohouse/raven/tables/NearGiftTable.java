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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import club.callistohouse.raven.vat.Vine;
import club.callistohouse.raven.vat.WeakPtr;


public class NearGiftTable {

	Map<Object[],Object> gifts = new HashMap<Object[],Object>();

    public void smash(Throwable problem) { gifts = null; }

    @SuppressWarnings("unused")
	public Vine provideFor( Object gift, BigInteger recipID, BigInteger nonce, BigInteger swissHash) {
        Object[] keyTriple = {recipID, nonce, swissHash};
        Vine result = new Vine(null);
        gifts.put(keyTriple, gift);
        WeakPtr weakPtr = new WeakPtr(result, "drop", keyTriple);
        return result;
    }

    public Object acceptFor(
    		BigInteger bigInteger,
    		BigInteger nonce,
    		BigInteger swissHash) {
        Object[] keyTriple = {bigInteger, nonce, swissHash};
        Object result = gifts.get(keyTriple);
        gifts.remove(keyTriple);
        return result;
    }

    public void drop(
    		String recipID, 
    		BigInteger nonce, 
    		BigInteger swissHash) {
        Object[] keyTriple = {recipID, nonce, swissHash};
        gifts.remove(keyTriple);
    }

	public int size() { return gifts.size(); }
}
