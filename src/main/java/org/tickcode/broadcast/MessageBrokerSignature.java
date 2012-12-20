package org.tickcode.broadcast;

public class MessageBrokerSignature {

	private String name;
	private String host;
	private int port;
	private String className;
	
	public MessageBrokerSignature(){
	}

	public MessageBrokerSignature(String className, String name, String host, int port){
		this.className = className;
		this.name = name;
		this.host = host;
		this.port = port;
	}
	
	
	public MessageBrokerSignature(MessageBrokerSignature source){
		this.name = source.getName();
		this.host = source.getHost();
		this.port = source.getPort();
		this.className = source.getClassName();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (port);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageBrokerSignature other = (MessageBrokerSignature) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (port != other.port)
				return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(className).append(":").append(name).append("@").append(host);
		if(port != 0)
			buffer.append(":").append(port);
		return buffer.toString();
	}
	
}
