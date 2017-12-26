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

import java.security.SecureRandom;

import club.callistohouse.utils.Pair;
import club.callistohouse.utils.PrimeGenerator;

public class Brand {

	public static Pair<Sealer, Unsealer> pair(String brandName) {
		Brand brand = new Brand(brandName);
		Sealer sealer = new Sealer(brand);
		Unsealer unsealer = new Unsealer(brand);
		return new Pair<Sealer, Unsealer>(sealer, unsealer);
	}
	public static Pair<Sealer, Unsealer> pairFromPrime(int numBits, int uncertainty) {
		PrimeGenerator gen = new PrimeGenerator(numBits, uncertainty, new SecureRandom());
		String brandName = null;
		try {
			brandName = gen.getStrongPrime().toString();
		} catch(NumberFormatException e) {
			brandName = gen.getSafePrime().toString();
		}
		Brand brand = new Brand(brandName);
		Sealer sealer = new Sealer(brand);
		Unsealer unsealer = new Unsealer(brand);
		return new Pair<Sealer, Unsealer>(sealer, unsealer);
	}

	private String brandName;

	private Brand(String brandName) {
		this.brandName = brandName;
	}

	@Override public int hashCode() { return 97 * 7 + brandName.hashCode(); }
	@Override public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		} else if(!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		} else {
			return brandName.equals(((Brand)obj).brandName);
		}
	}	
}
