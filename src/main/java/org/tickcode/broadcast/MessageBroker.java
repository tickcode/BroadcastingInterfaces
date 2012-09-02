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

public interface MessageBroker {

	public abstract boolean isAllowingBroadcastsToBroadcast();

	/**
	 * If you were running a multi-threaded application and you knew that there
	 * would be no infinite loops where broadcasts call broadcasts, you need to
	 * set this variable to true. Otherwise one thread could prevent the
	 * broadcast of the other thread because of shared code.
	 * 
	 * @param allowingInfiniteLoops
	 */
	public abstract void setAllowingBroadcastsToBroadcast(
			boolean allowingInfiniteLoops);

	public abstract void broadcast(Broadcast producer, String methodName,
			Object[] params);

	public abstract void remove(Broadcast consumer);

	public abstract void add(Broadcast consumer);

	public abstract void add(ErrorHandler handler);

	public abstract void remove(ErrorHandler handler);

	public abstract void clear();

}
