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