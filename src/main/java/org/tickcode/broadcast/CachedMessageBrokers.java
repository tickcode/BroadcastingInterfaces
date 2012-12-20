package org.tickcode.broadcast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class CachedMessageBrokers {

	public static volatile ConcurrentHashMap<MessageBrokerSignature, MessageBroker> cache = new ConcurrentHashMap<MessageBrokerSignature, MessageBroker>();

	public static MessageBroker findOrCreate(MessageBrokerSignature signature)
			throws InvocationTargetException, NoSuchMethodException,
			IllegalAccessException, InstantiationException,
			ClassNotFoundException {

		MessageBroker broker = cache.get(signature);
		if (broker != null)
			return broker;

		Class _class = Class.forName(signature.getClassName());
		Class[] parameterTypes = new Class[1];
		parameterTypes[0] = MessageBrokerSignature.class;
		Constructor<MessageBroker> constructor = _class
				.getConstructor(parameterTypes);
		Object[] params = new Object[1];
		params[0] = signature;
		broker = constructor.newInstance(params);
		cache.put(new MessageBrokerSignature(signature), broker);
		return broker;
	}
	
	public static void cache(MessageBroker broker){
		cache.put(broker.getSignature(), broker);
	}

}
