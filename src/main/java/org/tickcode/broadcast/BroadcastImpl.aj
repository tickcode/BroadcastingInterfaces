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

	pointcut broadcastPointcutWithArguments(Broadcast _this): 
		execution(@BroadcastProducer public * org.tickcode.broadcast.Broadcast+.*(..))		  	
	    && this(_this) && if(AbstractMessageBroker.isUsingAspectJ());

	/**
	 * Use this advice for methods with @BroadcastProducer annotations
	 */
	after(Broadcast _this) returning: broadcastPointcutWithArguments(_this){
		MessageBroker broker = _this.messageBroker;
		if(broker == null)
			throw new NoMessageBrokerException("Did you forget to add " + _this.getClass().getName() + " to a message broker?");

		String methodName = thisJoinPointStaticPart.getSignature().getName();
		Object[] params = thisJoinPoint.getArgs();

		broker.broadcast(_this, methodName, params);

	}

	
	pointcut messageBrokerAddPointcut(MessageBroker _this, Broadcast consumer): 
		execution(public void org.tickcode.broadcast.MessageBroker+.add(Broadcast))		  	
	    && this(_this) && args(consumer) && if(AbstractMessageBroker.isUsingAspectJ());
	/**
	 * This advice will be ensure that the field messageBroker this aspect provides get's set
	 * when the implementation is added to a {@link MessageBroker} implementation.
	 * @param _this
	 * @param consumer
	 */
	before(MessageBroker _this, Broadcast consumer) : messageBrokerAddPointcut(_this, consumer){
		if(consumer.messageBroker != null && consumer.messageBroker != _this)
			throw new OnlyOneMessageBrokerSupportedException("You can only have one MessageBroker for each implementation.");
		consumer.messageBroker = _this;
	}
	
	pointcut messageBrokerRemovePointcut(MessageBroker _this, Broadcast consumer): 
		execution(public void org.tickcode.broadcast.MessageBroker+.remove(Broadcast))		  	
	    && this(_this) && args(consumer) && if(AbstractMessageBroker.isUsingAspectJ());
	/**
	 * When the {@link MessageBroker} removes this implementation of Broadcast, we make sure
	 * our internal messageBroker field gets set to null.
	 * @param _this
	 * @param consumer
	 */
	after(MessageBroker _this, Broadcast consumer) returning : messageBrokerRemovePointcut(_this, consumer){
		consumer.messageBroker = null;
	}

	/**
	 * An internal field to hold the {@link MessageBroker}.  This field is used only by this aspect.
	 * @return
	 */
	private MessageBroker Broadcast.messageBroker;

}
