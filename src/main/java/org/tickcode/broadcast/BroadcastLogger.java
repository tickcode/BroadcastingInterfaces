package org.tickcode.broadcast;


public interface BroadcastLogger extends Broadcast{
	public void logEvent(int level, String message, String exceptionMessage, StackTraceElement[] exceptionElements);
}
