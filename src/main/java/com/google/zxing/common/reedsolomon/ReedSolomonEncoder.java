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

/**
 * <p>
 * Implements Reed-Solomon enbcoding, as the name implies.
 * </p>
 * 
 * @author Sean Owen
 * @author William Rucklidge
 */
public final class ReedSolomonEncoder {

    protected GaloisField galoisField;
    @SuppressWarnings("rawtypes")
	private final Vector cachedGenerators;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public ReedSolomonEncoder(GaloisField galoisField) {
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

    public byte[] encodeBytes(byte[] chunk) {
    	int[] toBeEncoded = GaloisField.bytesToInts(chunk);
    	int[] encoded = encode(toBeEncoded);
    	return Arrays.copyOf(GaloisField.intsToBytes(encoded), galoisField.getNumberOfChunkCodeSymbols());
    }

    /*
     * This method expects integers that are already separated into symbols.
     */
    public int[] encode(int[] toBeEncodedChunk) {
    	if(toBeEncodedChunk.length != galoisField.getNumberOfChunkDataSymbols()) {
    		throw new IllegalArgumentException("toBeEncodedChunk is wrong size.  expecting: " 
    				+ galoisField.getNumberOfChunkDataSymbols() + " received: " + toBeEncodedChunk.length);
    	}
    	int[] encodedChunk = new int[galoisField.getNumberOfChunkCodeSymbols()];
        System.arraycopy(toBeEncodedChunk, 0, encodedChunk, 0, galoisField.getNumberOfChunkDataSymbols());
        GenericGFPoly generator = buildGenerator(galoisField.getECSymbols());
       	int[] infoCoefficients = new int[galoisField.getNumberOfChunkDataSymbols()];
        System.arraycopy(toBeEncodedChunk, 0, infoCoefficients, 0, galoisField.getNumberOfChunkDataSymbols());
        GenericGFPoly info = new GenericGFPoly(getGFField(), infoCoefficients);
        info = info.multiplyByMonomial(galoisField.getECSymbols(), 1);
        GenericGFPoly remainder = info.divide(generator)[1];
        int[] coefficients = remainder.getCoefficients();
        int numZeroCoefficients = galoisField.getECSymbols() - coefficients.length;
        for (int i = 0; i < numZeroCoefficients; i++) {
        	encodedChunk[galoisField.getNumberOfChunkDataSymbols() + i] = 0;
        }
        System.arraycopy(coefficients, 0, encodedChunk, galoisField.getNumberOfChunkDataSymbols() + numZeroCoefficients, coefficients.length);
        return encodedChunk;
    }

    @SuppressWarnings("unchecked")
	private GenericGFPoly buildGenerator(int degree) {
        if (degree >= cachedGenerators.size()) {
            GenericGFPoly lastGenerator = (GenericGFPoly) cachedGenerators.elementAt(cachedGenerators.size() - 1);
            for (int d = cachedGenerators.size(); d <= degree; d++) {
                GenericGFPoly nextGenerator = lastGenerator.multiply(new GenericGFPoly(getGFField(), new int[] { 1, getGFField().exp(d - 1) }));
                cachedGenerators.addElement(nextGenerator);
                lastGenerator = nextGenerator;
            }
        }
        return (GenericGFPoly) cachedGenerators.elementAt(degree);
    }
}
