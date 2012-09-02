package org.tickcode.broadcast;

import java.lang.reflect.Method;

public class RedisBroadcastProxy implements java.lang.reflect.InvocationHandler {

	private RedisMessageBroker messageBroker;

	public static Broadcast newInstance(RedisMessageBroker broker, Class[] broadcastInterfaces) {
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(broker.getClass()
				.getClassLoader(), broadcastInterfaces,
				new RedisBroadcastProxy(broker));
		return proxy;
	}
	
	private RedisBroadcastProxy(RedisMessageBroker broker) {
		this.messageBroker = broker;
	}
	
//	public static MessageBroker getMessageBroker(Broadcast proxy){
//		return ((RedisBroadcastProxy)((java.lang.reflect.Proxy)proxy).getInvocationHandler(proxy)).getBroker();
//	}
	
	private MessageBroker getBroker(){
		return messageBroker;
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		messageBroker.broadcast((Broadcast)proxy, m.getName(), args);
		return null;
	}

}
