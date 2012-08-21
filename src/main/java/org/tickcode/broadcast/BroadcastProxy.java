package org.tickcode.broadcast;

import java.lang.reflect.Method;

public class BroadcastProxy implements java.lang.reflect.InvocationHandler {

	private Broadcast implementation;
	private MessageBroker messageBroker;

	public static Broadcast newInstance(MessageBroker broker, Broadcast implementation) {
		broker.add(implementation);
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(implementation.getClass()
				.getClassLoader(), implementation.getClass().getInterfaces(),
				new BroadcastProxy(broker, implementation));
		return proxy;
	}

	private BroadcastProxy(MessageBroker broker, Broadcast implementation) {
		this.implementation = implementation;
		this.messageBroker = broker;
	}
	
	public static Broadcast getImplementation(Broadcast proxy){
		if(proxy instanceof java.lang.reflect.Proxy){
			return ((BroadcastProxy)((java.lang.reflect.Proxy)proxy).getInvocationHandler(proxy)).getImp();
		}
		else
			return proxy;
	}
	
	private Broadcast getImp(){
		return implementation;
	}
	public static MessageBroker getMessageBroker(Broadcast proxy){
		return ((BroadcastProxy)((java.lang.reflect.Proxy)proxy).getInvocationHandler(proxy)).getBroker();
	}
	
	private MessageBroker getBroker(){
		return messageBroker;
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		Object result;
		result = m.invoke(implementation, args);
		getMessageBroker((Broadcast)proxy).broadcast(implementation, m.getName(), args);
		return result;
	}

}
