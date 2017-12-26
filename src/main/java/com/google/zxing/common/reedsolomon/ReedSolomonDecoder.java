/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.common.reedsolomon;

import java.util.Arrays;
import java.util.Vector;

public class ReedSolomonDecoder {

    protected GaloisField galoisField;
    @SuppressWarnings("rawtypes")
	private final Vector cachedGenerators;
    public int[] errorLocations;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public ReedSolomonDecoder(GaloisField galoisField) {
		this.galoisField = galoisField;
        if (!GenericGF.QR_CODE_FIELD_256.equals(getGFField()) 
        		&& !GenericGF.OW_CODE_FIELD_16.equals(getGFField())
        		&& !GenericGF.RS_256_A.equals(getGFField())
        		&& !GenericGF.RS_256_B.equals(getGFField())
        		&& !GenericGF.AZTEC_PARAM.equals(getGFField())
        		&& !GenericGF.AZTEC_DATA_6.equals(getGFField())
        		&& !GenericGF.AZTEC_DATA_8.equals(getGFField())
        		&& !GenericGF.AZTEC_DATA_10.equals(getGFField())
        		&& !GenericGF.AZTEC_DATA_12.equals(getGFField())) {
            throw new IllegalArgumentException("Illegal algorithm");
        }
		this.cachedGenerators = new Vector();
        cachedGenerators.addElement(new GenericGFPoly(getGFField(), new int[] { 1 }));
	}

    public GenericGF getGFField() { return galoisField.getFieldImpl(); }

	public byte[] decodeBytes(byte[] chunk) throws ReedSolomonException {
    	int[] toBeDecoded = GaloisField.bytesToInts(chunk);
    	int[] decoded = decode(toBeDecoded);
    	return Arrays.copyOf(GaloisField.intsToBytes(decoded), galoisField.getNumberOfChunkDataSymbols());
	}

	public int[] decode(int[] encodedChunk) throws ReedSolomonException {
		if (encodedChunk.length != galoisField.getNumberOfChunkCodeSymbols()) {
			throw new IllegalArgumentException(
					"encodedChuck is wrong size.  expecting: "
							+ galoisField.getNumberOfChunkCodeSymbols()
							+ " received: " + encodedChunk.length);
		}
		GenericGFPoly poly = new GenericGFPoly(getGFField(), encodedChunk);
		int[] syndromeCoefficients = new int[galoisField.getECSymbols()];
		boolean noError = true;
		for (int i = 0; i < galoisField.getECSymbols(); i++) {
			int eval = poly.evaluateAt(getGFField().exp(i));
			syndromeCoefficients[syndromeCoefficients.length - 1 - i] = eval;
			if (eval != 0) {
				noError = false;
			}
		}
		if (noError) {
			return encodedChunk;
		}
		GenericGFPoly syndrome = new GenericGFPoly(getGFField(), syndromeCoefficients);
		GenericGFPoly[] sigmaOmega = runEuclideanAlgorithm(
				getGFField().buildMonomial(galoisField.getECSymbols(), 1), 
				syndrome, 
				galoisField.getECSymbols());
		GenericGFPoly sigma = sigmaOmega[0];
		GenericGFPoly omega = sigmaOmega[1];
		errorLocations = findErrorLocations(sigma);
		int[] errorMagnitudes = findErrorMagnitudes(omega, errorLocations, false);
		for (int i = 0; i < errorLocations.length; i++) {
			int position = encodedChunk.length - 1 - getGFField().log(errorLocations[i]);
			if (position < 0) {
				throw new ReedSolomonException("Bad error location");
			}
			encodedChunk[position] = GenericGF.addOrSubtract(encodedChunk[position], errorMagnitudes[i]);
		}
		return encodedChunk;
	}

    protected GenericGFPoly[] runEuclideanAlgorithm(GenericGFPoly a, GenericGFPoly b, int R) throws ReedSolomonException {
        // Assume a's degree is >= b's
        if (a.getDegree() < b.getDegree()) {
            GenericGFPoly temp = a;
            a = b;
            b = temp;
        }

        GenericGFPoly rLast = a;
        GenericGFPoly r = b;
        GenericGFPoly sLast = getGFField().getOne();
        GenericGFPoly s = getGFField().getZero();
        GenericGFPoly tLast = getGFField().getZero();
        GenericGFPoly t = getGFField().getOne();

        // Run Euclidean algorithm until r's degree is less than R/2
        while (r.getDegree() >= R / 2) {
            GenericGFPoly rLastLast = rLast;
            GenericGFPoly sLastLast = sLast;
            GenericGFPoly tLastLast = tLast;
            rLast = r;
            sLast = s;
            tLast = t;

            // Divide rLastLast by rLast, with quotient in q and remainder in r
            if (rLast.isZero()) {
                // Oops, Euclidean algorithm already terminated?
                throw new ReedSolomonException("r_{i-1} was zero");
            }
            r = rLastLast;
            GenericGFPoly q = getGFField().getZero();
            int denominatorLeadingTerm = rLast.getCoefficient(rLast.getDegree());
            int dltInverse = getGFField().inverse(denominatorLeadingTerm);
            while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
                int degreeDiff = r.getDegree() - rLast.getDegree();
                int scale = getGFField().multiply(r.getCoefficient(r.getDegree()), dltInverse);
                q = q.addOrSubtract(getGFField().buildMonomial(degreeDiff, scale));
                r = r.addOrSubtract(rLast.multiplyByMonomial(degreeDiff, scale));
            }

            s = q.multiply(sLast).addOrSubtract(sLastLast);
            t = q.multiply(tLast).addOrSubtract(tLastLast);
        }

        int sigmaTildeAtZero = t.getCoefficient(0);
        if (sigmaTildeAtZero == 0) {
            throw new ReedSolomonException("sigmaTilde(0) was zero");
        }

        int inverse = getGFField().inverse(sigmaTildeAtZero);
        GenericGFPoly sigma = t.multiply(inverse);
        GenericGFPoly omega = r.multiply(inverse);
        return new GenericGFPoly[] { sigma, omega };
    }

    protected int[] findErrorLocations(GenericGFPoly errorLocator) throws ReedSolomonException {
        // This is a direct application of Chien's search
        int numErrors = errorLocator.getDegree();
        if (numErrors == 1) { // shortcut
            return new int[] { errorLocator.getCoefficient(1) };
        }
        int[] result = new int[numErrors];
        int e = 0;
        for (int i = 1; i < getGFField().getSize() && e < numErrors; i++) {
            if (errorLocator.evaluateAt(i) == 0) {
                result[e] = getGFField().inverse(i);
                e++;
            }
        }
        if (e != numErrors) {
//            throw new ReedSolomonException("Error locator degree does not match number of roots - Degree: " + numErrors + " Roots: " + e);
        }
        return result;
    }

    protected int[] findErrorMagnitudes(GenericGFPoly errorEvaluator, int[] errorLocations, boolean dataMatrix) {
        // This is directly applying Forney's Formula
        int s = errorLocations.length;
        int[] result = new int[s];
        for (int i = 0; i < s; i++) {
            int xiInverse = getGFField().inverse(errorLocations[i]);
            int denominator = 1;
            for (int j = 0; j < s; j++) {
                if (i != j) {
                    // denominator = field.multiply(denominator,
                    // GenericGF.addOrSubtract(1,
                    // field.multiply(errorLocations[j], xiInverse)));
                    // Above should work but fails on some Apple and Linux JDKs
                    // due to a Hotspot bug.
                    // Below is a funny-looking workaround from Steven Parkes
                    int term = getGFField().multiply(errorLocations[j], xiInverse);
                    int termPlus1 = ((term & 0x1) == 0) ? (term | 1) : (term & ~1);
                    denominator = getGFField().multiply(denominator, termPlus1);
                }
            }
            result[i] = getGFField().multiply(errorEvaluator.evaluateAt(xiInverse), getGFField().inverse(denominator));
            // Thanks to sanfordsquires for this fix:
            if (dataMatrix) {
                result[i] = getGFField().multiply(result[i], xiInverse);
            }
        }
        return result;
    }

}
