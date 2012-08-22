/*******************************************************************************
 * Copyright © 2012 tickcode.org All rights reserved. 
 *  
 * This file is part of the Tickcode collection of software
 *  
 * This file may be distributed under the terms of the tickcode.org 
 * license as defined by tickcode.org and appearing in the file 
 * license.txt included in the packaging of this file. 
 *  
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 *  
 *  See http://www.tickcode.org/LICENSE for licensing information. 
 *   
 *  Contact ask@tickcode.org if any conditions of this licensing 
 *  are not clear to you.
 ******************************************************************************/package org.tickcode.trace;

import java.lang.reflect.Method;

public class MethodUtil {
	public static String getReadableMethodString(Class _interface, Method method){
		return _interface.getName() + "." + method.getName() +  "(" + getParamTypes(method.getParameterTypes()) + ")";
	}
	public static String getReadableMethodString(Class _interface, Method method, Object[] args){
		return _interface.getName() + "." + method.getName() + "(" + getArguments(args) + ")";
	}
	public static String getParamTypes(Class[] argClasses){
		StringBuffer buffer = new StringBuffer();
		if (argClasses == null)
			return "";
		for (int i = 0; i < argClasses.length; i++) {
			if (i > 0)
				buffer.append(",");
			buffer.append(argClasses[i].getSimpleName());
		}
		return buffer.toString();
	}
	public static String getArguments(Object[] args) {
		StringBuffer buffer = new StringBuffer();
		if (args == null)
			return "";
		for (int i = 0; i < args.length; i++) {
			if (i > 0)
				buffer.append(",");
			buffer.append(args[i]);
		}
		return buffer.toString();
	}

}
