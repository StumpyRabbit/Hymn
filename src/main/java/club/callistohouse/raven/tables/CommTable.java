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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import club.callistohouse.utils.Pair;

public class CommTable<T> {

    protected String tableName;
    protected TreeMap<Integer, Pair<T,Integer>> map = new TreeMap<Integer, Pair<T,Integer>>();
    protected List<Integer> holes = new ArrayList<Integer>();
    protected boolean isNegative = false;

    public CommTable(String tableName, boolean isNegative) {
    	this.tableName = tableName;
		this.isNegative = isNegative;
    }

    public T get(int index) throws IOException {
    	int internalIndex = internalizeIndex(index);
    	if(map.containsKey(internalIndex)) {
    		T result = (T) map.get(internalIndex).first();
            return result;
    	} else
            throw new IOException("not available: " + index);
    }
    public void put(int index, T value) throws IOException {
    	int internalIndex = internalizeIndex(index);
    	if(!map.containsKey(internalIndex)) {
    		map.put(internalIndex, new Pair<T,Integer>(value,1));
    	}
    }
    public int bind(T value) {
    	Integer key = null;
    	if(holes.size() > 0) {
        	Collections.sort(holes);
        	key = holes.get(0);
    		holes.remove(key);
    		map.put(key, new Pair<T,Integer>(value,1));
    	} else {
    		if(map.size() > 0)
        		key = map.lastKey() + 1;
    		else {
    			key = 0;
    		}
    		map.put(key, new Pair<T,Integer>(value,1));
    	}
		return externalizeIndex(key);
    }

    public Integer indexOf(T value) {
    	for(Entry<Integer, Pair<T,Integer>> entry:map.entrySet()) {
    		if(entry.getValue().second().equals(value))
    			return externalizeIndex(entry.getKey());
    	}
    	return null;
    }
    public int size() { return map.size(); }
    public void increment(int index) {
    	int internalIndex = internalizeIndex(index);
    	Pair<T,Integer> pair = map.get(internalIndex);
    	Pair<T,Integer> newPair = new Pair<T,Integer>(pair.first(), pair.second() + 1);
    	map.put(internalIndex, newPair);
    }
    public boolean decrement(int index, int delta) {
    	int internalIndex = internalizeIndex(index);
    	Pair<T,Integer> pair = map.get(internalIndex);
    	Integer count = pair.second();
    	if(count <= 1) {
    		map.remove(internalIndex);
    		holes.add(internalIndex);
    		return true;
    	} else {
    		Pair<T,Integer> newPair = new Pair<T,Integer>(pair.first(), pair.second() - 1);
    		map.put(internalIndex, newPair);
    		return false;
    	}
    }
    public void smash(Throwable problem) {
        map = null;
        holes = null;
    }
    public String toString() {
    	StringBuilder builder = new StringBuilder();
    	builder.append("table(" + size() + ")");
    	if(size() == 0)
    		return builder.toString();
    	builder.append(" <");
		try {
			int max = size() < 5 ? size() : 4;
			for (int i = 0; i < max - 1; i++) {
				builder.append("map[" + i + "] = ");
				builder.append(get(externalizeIndex(i)));
				builder.append(", ");
			}
			builder.append("map[" + (max - 1) + "] = ");
			builder.append(get(externalizeIndex(max - 1)));
			builder.append(">");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
    }

    protected int internalizeIndex(int index) {
		if(isNegative) 
			return -index - 1;
		else
			return index - 1;
	}
	protected int externalizeIndex(int internalIndex) {
		if(isNegative) 
			return -(internalIndex + 1);
		else
			return internalIndex + 1;
	}
	protected void send(T obj, String action, Exception e) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = null;
		m = obj.getClass().getDeclaredMethod(action, Exception.class);
		m.invoke(obj, e);
	}
}
