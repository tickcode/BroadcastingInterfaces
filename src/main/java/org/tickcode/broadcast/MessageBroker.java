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
 * Provides the expected interface for any MessageBroker. The interface will be
 * most useful to classes that are trying to automatically provide the mix-in
 * for {@link Object} behavior. For example {@link BroadcastProxy} and the
 * aspect BroadcastImpl.aj.
 * 
 * @author Eyon Land
 * 
 */
public interface MessageBroker {

	public abstract void broadcast(Object producer, Method m, Object[] params, String thumbprint) throws NoSuchMethodException;

	public abstract void removeSubscriber(Object consumer);
	
	public abstract void removeAllSubscribers();

	public abstract void addSubscriber(Object consumer);

	public abstract <T extends Object> T createPublisher(
			Class<? extends T> broadcastInterfaces);

	public abstract <T extends Object> T createCallbackPublisher(MessageBroker callbackBroker,
			Class<? extends T> broadcastInterfaces);
	
	public abstract void addErrorHandler(ErrorHandler handler);

	public abstract void removeErrorHandler(ErrorHandler handler);

	public abstract void clear();

	public abstract int size();
	
	public MessageBrokerSignature getSignature();
	
	public String getThumbprint();

}
