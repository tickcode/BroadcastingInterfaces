package org.tickcode.broadcast;

public class MessageBrokerSignature {

	private String name;
	private String host;
	private Integer port;
	private Class _class;
	
	public MessageBrokerSignature(){
	}

	public MessageBrokerSignature(Class _class, String name, String host, Integer port){
		this._class = _class;
		this.name = name;
		this.host = host;
		this.port = port;
	}
	
	public MessageBrokerSignature(MessageBrokerSignature source){
		this.name = source.getName();
		this.host = source.getHost();
		this.port = source.getPort();
		this._class = source.getKlass();
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

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Class getKlass() {
		return _class;
	}

	public void setKlass(Class _class) {
		this._class = _class;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_class == null) ? 0 : _class.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((port == null) ? 0 : port.hashCode());
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
		if (_class == null) {
			if (other._class != null)
				return false;
		} else if (!_class.equals(other._class))
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
		if (port == null) {
			if (other.port != null)
				return false;
		} else if (!port.equals(other.port))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(_class.getSimpleName()).append(" ").append(name).append("@").append(host);
		if(port != null)
			buffer.append(":").append(port);
		return buffer.toString();
	}
	
}
