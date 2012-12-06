package org.tickcode.broadcast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class CachedMessageBrokers {

	public static volatile ConcurrentHashMap<MessageBrokerSignature, MessageBroker> cache = new ConcurrentHashMap<MessageBrokerSignature, MessageBroker>();

	public static MessageBroker findOrCreate(MessageBrokerSignature signature)
			throws InvocationTargetException, NoSuchMethodException,
			IllegalAccessException, InstantiationException {

		MessageBroker broker = cache.get(signature);
		if (broker != null)
			return broker;

		String name = signature.getName();
		String host = signature.getHost();
		Integer port = signature.getPort();
		Class _class = signature.getKlass();
		if (host != null && name != null && port != null) {
			Class[] parameterTypes = new Class[3];
			parameterTypes[0] = String.class;
			parameterTypes[1] = String.class;
			parameterTypes[2] = Integer.class;
			try {
				Constructor<MessageBroker> constructor = _class
						.getConstructor(parameterTypes);
				Object[] params = new Object[3];
				params[0] = name;
				params[1] = host;
				params[2] = port;
				broker = constructor.newInstance(params);
				cache.put(new MessageBrokerSignature(signature), broker);
				return broker;
			} catch (NoSuchMethodException ex) {
				// let the other conditions get checked
			}
		}
		if (signature.getHost() != null && signature.getName() != null) {
			Class[] parameterTypes = new Class[2];
			parameterTypes[0] = String.class;
			parameterTypes[1] = String.class;
			Constructor<MessageBroker> constructor = _class
					.getConstructor(parameterTypes);
			Object[] params = new Object[2];
			params[0] = name;
			params[1] = host;
			broker = constructor.newInstance(params);
			cache.put(new MessageBrokerSignature(signature), broker);
			return broker;
		} else {
			Class[] parameterTypes = new Class[1];
			parameterTypes[0] = String.class;
			Constructor<MessageBroker> constructor = _class
					.getConstructor(parameterTypes);
			Object[] params = new Object[1];
			params[0] = name;
			broker = constructor.newInstance(params);
			cache.put(new MessageBrokerSignature(signature), broker);
			return broker;
		}
	}

}
