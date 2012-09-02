package org.tickcode.broadcast;

public class Parameters {

	String channel;
	Object[] arguments;
	
	public Parameters(){
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Object[] getArguments() {
		return arguments;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}
}
