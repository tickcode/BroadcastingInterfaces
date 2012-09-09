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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * The point of this class is to show how you might avoid using Aspects, Javassist, asm/cglib.  In some sense
 * it exposes the limitation of java proxies in that you cannot simply use the proxy as though it were
 * the original class.  You are limited to using only the interfaces the proxy has been defined for.
 * @author Eyon Land
 *
 */
public class BroadcastProxy implements java.lang.reflect.InvocationHandler, GetProxyImplementation {

	private Broadcast implementation;
	private MessageBroker messageBroker;
	private HashSet<String> broadcastMethods = new HashSet<String>();

	public static Broadcast newInstance(MessageBroker broker, Broadcast implementation) {
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(implementation.getClass()
				.getClassLoader(), implementation.getClass().getInterfaces(),
				new BroadcastProxy(broker, implementation));
		broker.add(proxy);
		return proxy;
	}

	private BroadcastProxy(MessageBroker broker, Broadcast implementation) {
		this.implementation = implementation;
		this.messageBroker = broker;
		for (Method method : implementation.getClass().getMethods()) {
			if (method.isAnnotationPresent(BroadcastProducer.class)) {
				broadcastMethods.add(method.getName());
			}
		}
	}
	
	public Broadcast getBroadcastImplementation(){
		return implementation;
	}
	
	public static MessageBroker getMessageBroker(Broadcast proxy){
		return ((BroadcastProxy)((java.lang.reflect.Proxy)proxy).getInvocationHandler(proxy)).getBroker();
	}
	
	private MessageBroker getBroker(){
		return messageBroker;
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		Object result;
		result = m.invoke(implementation, args);
		if(broadcastMethods.contains(m.getName()))
			messageBroker.broadcast((Broadcast)proxy, m.getName(), args);
		return result;
	}

}
