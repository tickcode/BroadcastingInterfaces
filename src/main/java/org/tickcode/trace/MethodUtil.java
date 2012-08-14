package org.tickcode.trace;

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
