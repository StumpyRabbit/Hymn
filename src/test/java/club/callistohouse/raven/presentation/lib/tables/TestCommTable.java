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
package club.callistohouse.raven.presentation.lib.tables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import club.callistohouse.raven.tables.CommTable;

public class TestCommTable {

	Object obj0 = new Object();
	Object obj1 = new Object();
	Object obj2 = new Object();

	@Test
	public void testCommTableGetPut() throws Exception {
		CommTable<Object> table = new CommTable<Object>("test-table", false);
		table.put(0, obj0);
		table.put(1, obj1);
		table.put(2, obj2);
		assertEquals(obj0, table.get(0));
		assertEquals(obj1, table.get(1));
		assertEquals(obj2, table.get(2));
	}
	@Test
	public void testCommTableIncDecWithZeroCount() throws Exception {
		CommTable<Object> table = new CommTable<Object>("test-table", false);
		int wire = table.bind(obj0);
		table.increment(wire);
		table.decrement(wire, 0);
		table.decrement(wire, 0);
		try {
			table.decrement(wire, 0);
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	@Test
	public void testCommTableIncDecWithOneCount() throws Exception {
		CommTable<Object> table = new CommTable<Object>("test-table", false);
		int wire = table.bind(obj0);
		table.increment(wire);
		table.decrement(wire, 1);
		table.decrement(wire, 1);
		try {
			table.decrement(wire, 1);
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	@Test
	public void testCommTableMultipleObjectIncDecWithOneCount() throws Exception {
		CommTable<Object> table = new CommTable<Object>("test-table", false);
		int wire0 = table.bind(obj0);
		table.increment(wire0);
		int wire1 = table.bind(obj1);
		assertEquals(2, wire1);
		table.decrement(wire0, 1);
		table.decrement(wire0, 1);
		int wire2 = table.bind(obj2);
		assertEquals(1, wire2);
		table.decrement(wire2, 1);
		try {
			table.decrement(wire2, 1);
		} catch (Exception e) {
			assertTrue(true);
		}
	}
}
