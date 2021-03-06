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

import org.tickcode.trace.MethodUtil;

/**
 * Used for creating a service proxy for sending messages to
 * {@link CallbackService} such that the callback will be on a different
 * callback {@link MessageBroker}.
 * 
 * @author Eyon Land
 * 
 */
public class CallbackServiceProxy implements
		java.lang.reflect.InvocationHandler {

	public static interface MessageBrokerCallbackSignature {
		public void useThisCallbackSignature(
				MessageBrokerSignature callbackSignature);
	}

	private MessageBroker messageBroker;
	private MessageBroker callbackBroker;
	private MessageBrokerCallbackSignature registerCallback;
	private String interfaceName;

	public static Object newInstance(MessageBroker broker,
			MessageBroker callbackBroker, Class broadcastInterface) {
		if (broadcastInterface.isInterface()) {

			Object proxy = (Object) java.lang.reflect.Proxy.newProxyInstance(
					broker.getClass().getClassLoader(),
					new Class[] { broadcastInterface },
					new CallbackServiceProxy(broker, callbackBroker,
							broadcastInterface.getName()));
			return proxy;
		} else {
			throw new UnsupportedOperationException(
					"You may only create a producer from an interface.");
		}
	}

	protected CallbackServiceProxy(MessageBroker broker,
			MessageBroker callbackBroker, String interfaceName) {
		this.messageBroker = broker;
		this.callbackBroker = callbackBroker;
		this.interfaceName = interfaceName;
		registerCallback = messageBroker
				.createPublisher(MessageBrokerCallbackSignature.class);
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		if (Void.TYPE.equals(m.getReturnType())) {
			registerCallback.useThisCallbackSignature(callbackBroker
					.getSignature());
			messageBroker.broadcast((Object) proxy, m, args,
					messageBroker.getThumbprint());
			return null;
		} else {
			if (m.getName().equals("toString")) {
				return this.toString();
			} else if (m.getName().equals("hashCode")) {
				return this.hashCode();
			} else if (m.getName().equals("equals")) {
				return this.equals(args[0]);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return interfaceName + "->" + messageBroker.toString() + "->"
				+ callbackBroker.toString();
	}

}
