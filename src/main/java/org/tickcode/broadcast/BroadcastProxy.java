package org.tickcode.broadcast;

import java.lang.reflect.Method;

public class BroadcastProxy implements java.lang.reflect.InvocationHandler {

	private Broadcast implementation;

	public static Broadcast newInstance(Broadcast implementation) {
		MessageBroker.get().register(implementation);
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(implementation.getClass()
				.getClassLoader(), implementation.getClass().getInterfaces(),
				new BroadcastProxy(implementation));
		return proxy;
	}

	private BroadcastProxy(Broadcast implementation) {
		this.implementation = implementation;
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

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		Object result;
		result = m.invoke(implementation, args);
		MessageBroker.get().broadcast(implementation, m.getName(), args);
		return result;
	}

}
