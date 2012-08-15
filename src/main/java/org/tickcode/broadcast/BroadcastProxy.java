package org.tickcode.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BroadcastProxy implements java.lang.reflect.InvocationHandler {

	private Broadcast realImplementation;

	public static Broadcast newInstance(Broadcast obj) {
		MessageBroker.get().register(obj);
		Broadcast proxy = (Broadcast)java.lang.reflect.Proxy.newProxyInstance(obj.getClass()
				.getClassLoader(), obj.getClass().getInterfaces(),
				new BroadcastProxy(obj));
		return proxy;
	}

	private BroadcastProxy(Broadcast obj) {
		this.realImplementation = obj;
	}

	public Object invoke(Object proxy, Method m, Object[] args)
			throws Throwable {
		Object result;
		try {
			result = m.invoke(realImplementation, args);
			MessageBroker.get().broadcast(realImplementation, m.getName(), args);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		} catch (Exception e) {
			throw new RuntimeException("unexpected invocation exception: "
					+ e.getMessage());
		} finally {
			System.out.println("TODO: broadcast here!");
			System.out.println("end method " + m.getName());
		}
		return result;
	}

}
