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

import java.lang.reflect.Method;

/**
 * Used for creating a producer proxy for sending messages to all consumers using
 * the given MesssageBroker.
 * @author Eyon Land
 *
 */
public class BroadcastProducerProxy implements java.lang.reflect.InvocationHandler {

	private MessageBroker messageBroker;

	public static Broadcast newInstance(MessageBroker broker, Class[] broadcastInterfaces) {
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(broker.getClass()
				.getClassLoader(), broadcastInterfaces,
				new BroadcastProducerProxy(broker));
		return proxy;
	}
	
	private BroadcastProducerProxy(MessageBroker broker) {
		this.messageBroker = broker;
	}
	
	private MessageBroker getBroker(){
		return messageBroker;
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		messageBroker.broadcast((Broadcast)proxy, m.getDeclaringClass(), m.getName(), m.getParameterTypes(), args);
		return null;
	}

}
