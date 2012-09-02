/*******************************************************************************
 * Copyright (c) 2012, tickcode.org
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of tickcode, nor tickcode.org, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
