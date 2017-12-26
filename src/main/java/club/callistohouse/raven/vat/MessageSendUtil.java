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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import club.callistohouse.raven.exceptions.MethodNotFoundException;
import club.callistohouse.raven.exceptions.ReceiverNotFoundException;

public class MessageSendUtil {

	protected static Method getMethod(Class<?> receiverClass, String selector, Object[] args) throws MethodNotFoundException, ReceiverNotFoundException {
		Method method = null;
		Class<?>[] argClasses = buildArgClasses(args);
		try {
			method = receiverClass.getMethod(selector, argClasses);
			if (method != null)
				return method;
		} catch (Exception e) {}
		return lookupMethodWithArgClasses(receiverClass, selector, argClasses);
	}

	protected static Class<?>[] buildArgClasses(Object[] args) {
		List<Class<?>> argClassesList = new ArrayList<Class<?>>();
		for (Object arg : args) {
			argClassesList.add(arg.getClass());
		}
		Class<?>[] argClasses = argClassesList.toArray(new Class[0]);
		return argClasses;
	}

	protected static Method lookupMethodWithArgClasses(Class<?> receiverClass, String selector, Class<?>[] argClasses) throws MethodNotFoundException {
		Method[] methods = receiverClass.getMethods();
		List<Method> filteredMethods = new ArrayList<Method>();
		for (Method m : methods) {
			if (m.getName().equals(selector)) {
				filteredMethods.add(m);
			}
		}
		for (Method m : filteredMethods) {
			Class<?>[] paramClasses = m.getParameterTypes();
			if (paramClasses.length != argClasses.length)
				continue;
			boolean argsMatch = true;
			for (int i = 0; i < paramClasses.length; i++) {
				if (!(paramClasses[i].isAssignableFrom(argClasses[i]))) {
					argsMatch = false;
					continue;
				}
			}
			if(argsMatch)
				return m;
		}
		throw new MethodNotFoundException("receiver: " + receiverClass + " selector: " + selector);
	}
}
