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