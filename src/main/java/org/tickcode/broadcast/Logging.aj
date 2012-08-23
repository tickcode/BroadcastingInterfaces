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
 ******************************************************************************/
/**
 * 
 */
package org.tickcode.broadcast;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
//import org.aspectj.lang.JoinPoint;


/**
 * Based on logging shown in
 * <u>Eclipse AspectJ</u> by Adrian Colyer, Andy Clement, George Harley and Matthew Webster.
 * Also includes changes posted on the user group aspectj-users@eclipse.org from Roland Kofler.
 */
public aspect Logging {

	private static Logger logger;
	public static boolean loggingOn;
	
	static{
		logger =
			Logger.getLogger(org.tickcode.broadcast.Broadcast.class);
		loggingOn = (logger.getEffectiveLevel()!=org.apache.log4j.Level.OFF);
	}
	
	pointcut shouldLog() : !within(Logging) && if(loggingOn);
	
	public static String lastMessageInBeforeAdvice;
	public static String lastMessageInAfterAdvice;
	
	/** Here we look for the public methods used by the Notice interfaces */
	pointcut publicReturnMethods(Broadcast _this) :
		shouldLog() 
		    && 
		  	execution(public * org.tickcode.broadcast.Broadcast+.*(..))		  	
		  	&& 
		  	this(_this);
	before(Broadcast _this) : publicReturnMethods(_this) {
		lastMessageInBeforeAdvice = logParamValues(logger,_this, thisJoinPointStaticPart, thisJoinPoint.getArgs());
		logger.info(lastMessageInBeforeAdvice);
    }
	
	after(Broadcast _this) returning(Object o): publicReturnMethods(_this){		
        // Object returnValue= thisJoinPoint.getArgs()[0];		
		if (logger.isEnabledFor(Level.INFO)){
			lastMessageInAfterAdvice = _this.getClass().getName() + "." + thisJoinPointStaticPart.getSignature().getName() + " returns"; 
			logger.info(lastMessageInAfterAdvice);
		     //Note: If the notice methods had a return value you would want to append the 'o'
		     //return value here.  
		}
	}
	
	private String logParamValues(
			Logger logger,
			Broadcast notice,
			JoinPoint.StaticPart joinPointStaticPart,
			Object[] paramValues) {
		if (logger.isEnabledFor(Level.INFO)) { // you can even gain perfomance by using the (otherwise unsexy) fast if clause
			StringBuffer buffer = new StringBuffer();
			buffer.append(notice.getClass().getName());
			buffer.append(".");
			buffer.append(joinPointStaticPart.getSignature().getName());
			buffer.append("(");
			for (int i=0; i<paramValues.length; i++) {
			     buffer.append(paramValues[i]);
			     if(i+1!=paramValues.length)
			       buffer.append(", ");
			}
			buffer.append(")");
		    return buffer.toString();
		 }
		return null;
    }
	
	/**
	 * Useful if you want to get the specific logger for the class
	 * that whose information is being logged.
	 * Logger log = 
			Logger.getLogger(assembleFullName(thisJoinPointStaticPart));
	 */
    private String assembleFullName(JoinPoint.StaticPart joinPointStaticPart) {
		Signature sig = joinPointStaticPart.getSignature();
		String sign= sig.getDeclaringType().getName()+"."+sig.getName();
		return sign;
	}	
}
