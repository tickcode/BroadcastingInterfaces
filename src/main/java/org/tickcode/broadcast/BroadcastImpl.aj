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
package org.tickcode.broadcast;

import org.apache.log4j.Logger;

/**
 * This aspect will provide behavior to any class implementing interfaces that
 * extend {@link Broadcast}. Any interface that extends {@link Broadcast} must
 * have methods that are unique across all the subclass interfaces.
 * 
 * @author Eyon Land
 */
public aspect BroadcastImpl {

	private static Logger log;
	public static boolean loggingOn;
	public static boolean isUsingAspectJ;

	static {
		log = Logger.getLogger(org.tickcode.broadcast.Broadcast.class);
		loggingOn = (log.getEffectiveLevel() != org.apache.log4j.Level.OFF);
		AbstractMessageBroker.setUsingAspectJ(true);
	}
	

	pointcut shouldLog() : !within(Logging) && if(loggingOn);

//	/**
//	 * When an instance of a Broadcast sub-interface is created, we will grab
//	 * the instance and add it to {@link AbstractMessageBroker}.
//	 */
//	pointcut createErrorHandler(ErrorHandler _this):
//		  execution (ErrorHandler+.new(..)) && this(_this) && if(AbstractMessageBroker.get().isUsingAspectJ());
//
//	after(ErrorHandler _this) returning: createErrorHandler(_this){
//		AbstractMessageBroker.get().add(_this);
//	}
//
//	/**
//	 * When an instance of a Broadcast sub-interface is created, we will grab
//	 * the instance and add it to {@link AbstractMessageBroker}.
//	 */
//	pointcut createBroadcast(Broadcast _this):
//		  execution (Broadcast+.new(..)) && this(_this) && if(AbstractMessageBroker.get().isUsingAspectJ());
//
//	after(Broadcast _this) returning: createBroadcast(_this){
//		AbstractMessageBroker.get().add(_this);
//	}

	pointcut broadcastPointcutWithArguments(Broadcast _this): 
		execution (@BroadcastProducer * *(*))
	    && this(_this) && if(AbstractMessageBroker.isUsingAspectJ()) && if(!executingAdvice);

	static volatile boolean executingAdvice = false;

	/**
	 * Use this advice for methods with arguments
	 */
	after(Broadcast _this) returning: broadcastPointcutWithArguments(_this){
		MessageBroker broker = _this.getMessageBroker();
		if(broker == null)
			throw new NoMessageBrokerException("Did you forget to add " + _this.getClass().getName() + " to a message broker?");

		executingAdvice = !broker.isAllowingBroadcastsToBroadcast();

		String methodName = thisJoinPointStaticPart.getSignature().getName();
		Object[] params = thisJoinPoint.getArgs();

		broker.broadcast(_this, methodName, params);

		executingAdvice = false;
	}

	pointcut broadcastPointcutWithNoArguments(Broadcast _this): 
		execution (@BroadcastProducer * *())
	    && this(_this)  && if(AbstractMessageBroker.isUsingAspectJ()) && if(!executingAdvice);

	after(Broadcast _this) returning: broadcastPointcutWithNoArguments(_this){
		MessageBroker broker = _this.getMessageBroker();
		if(broker == null)
			throw new NoMessageBrokerException("Did you forget to add " + _this.getClass().getName() + " to a message broker?");

		executingAdvice = !broker.isAllowingBroadcastsToBroadcast();
		
		String methodName = thisJoinPointStaticPart.getSignature().getName();
		Object[] params = thisJoinPoint.getArgs();

		broker.broadcast(_this, methodName, params);
		executingAdvice = false;
	}
	
	private MessageBroker Broadcast.messageBroker;
	public final MessageBroker Broadcast.getMessageBroker(){
		return messageBroker;
	}
	public final void Broadcast.setMessageBroker(MessageBroker broker){
		messageBroker = broker;
	}

}
