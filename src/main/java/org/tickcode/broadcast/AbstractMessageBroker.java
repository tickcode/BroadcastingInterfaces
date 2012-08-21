package org.tickcode.broadcast;


public abstract class AbstractMessageBroker implements MessageBroker{
	private static boolean isUsingAspectJ = false;
	public static boolean isUsingAspectJ() {
		return isUsingAspectJ;
	}

	public static void setUsingAspectJ(boolean usingAspectJ) {
		isUsingAspectJ = usingAspectJ;
	}

}
