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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;

public class CallbackService<T> implements
		BroadcastServiceProxy.MessgeBrokerCallbackSignature{
	Logger log = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
	
	ConcurrentHashMap<String, Object> callbackProxiesByThumbprint = new ConcurrentHashMap<String, Object>();
	ConcurrentHashMap<String, MessageBroker> messageBrokersByThumbprint = new ConcurrentHashMap<String, MessageBroker>();
	Class<? extends T> callbackInterface;
	
	public CallbackService() {
		callbackInterface = findInterface();
		if(!callbackInterface.isInterface()){
			throw new IllegalArgumentException("You must provide an interface for the callback.");
		}
	}
	
	public Class findInterface(){
		Type type = this.getClass().getGenericSuperclass();
		Class _class = null;
		if (type instanceof ParameterizedType){
		      ParameterizedType paramType = (ParameterizedType) type;
		      Type[] arguments = paramType.getActualTypeArguments();
		      if (arguments[0] instanceof Class) {
		    	  _class = (Class)arguments[0];
					if(!_class.isInterface())
						_class = null;
		      }
		}
		if(_class == null)
				throw new IllegalArgumentException("CallbackService is a parameterized class that must be used with an interface.");
		return _class;
	}
	
	protected T getCallbackProxy() {
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		Object callbackProxy = callbackProxiesByThumbprint.get(thumbprint);
		if(callbackProxy==null){
			return (T)null;
		}
		else{
			return (T)callbackProxy;
		}
	}
	
	protected MessageBroker getCallbackMessageBroker(){
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		return messageBrokersByThumbprint.get(thumbprint);
	}


	@Override
	public void useThisCallbackSignature(
			MessageBrokerSignature callbackSignature) {
		
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		
		try {
			Object callbackProxy = callbackProxiesByThumbprint.get(thumbprint);
			if (callbackProxy == null) {
				MessageBroker callbackBroker = CachedMessageBrokers
						.findOrCreate(callbackSignature);
				messageBrokersByThumbprint.put(thumbprint, callbackBroker);
				callbackProxy = callbackBroker.createPublisher(callbackInterface);
				callbackProxiesByThumbprint.put(thumbprint, callbackProxy);
			}

		} catch (IllegalAccessException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (InstantiationException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (ClassNotFoundException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (InvocationTargetException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (NoSuchMethodException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		}
	}
}
