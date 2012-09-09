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

/**
 * Provides static methods used primarily by the aspect BroadcastImpl.aj
 * @author Eyon Land
 *
 */
public abstract class AbstractMessageBroker implements MessageBroker{
	private static boolean isUsingAspectJ = false;
	
	/**
	 * Used by aspects so they know whether they can be involved with building the desired mix-ins.
	 * @return
	 */
	public static boolean isUsingAspectJ() {
		return isUsingAspectJ;
	}

	/**
	 * Can be called explicitly to avoid using aspects.  
	 * Normally this would only be used in testing new ideas where AspectJ
	 * is substituted for javassist or asm/cglib technology.
	 * @param usingAspectJ
	 */
	public static void setUsingAspectJ(boolean usingAspectJ) {
		isUsingAspectJ = usingAspectJ;
	}
	
	/**
	 * For a java.lang.reflect.Proxy, if the InvocationHandler happens to implement GetProxyImplementation
	 * then this provides a way to get the underlying implementation.  This would be used for example
	 * if the {@link BroadcastProxy} were being used to provide the broadcast behavior to the implementation.
	 * @param broadcast
	 * @return
	 */
	public static Broadcast getBroadcastImplementation(Broadcast broadcast){
		if(broadcast instanceof java.lang.reflect.Proxy){
			InvocationHandler handler = ((java.lang.reflect.Proxy)broadcast).getInvocationHandler(broadcast);
			if(handler instanceof GetProxyImplementation){
				return ((GetProxyImplementation)handler).getBroadcastImplementation();
			}
		}
		return broadcast;
	}


}
