package org.tickcode.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;

public class CallbackService<T> implements
		BroadcastServiceProxy.MessgeBrokerCallbackSignature{
	Logger log = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
	
	ConcurrentHashMap<String, Object> cachedCallbackProxies = new ConcurrentHashMap<String, Object>();
	Class<? extends T> callbackInterface;
	
	public CallbackService() {
		callbackInterface = findInterface();
		if(!callbackInterface.isInterface()){
			throw new IllegalArgumentException("You must provide an interface for the callback.");
		}
	}
	
	public Class findInterface(){
		Type type = this.getClass().getGenericSuperclass();
		Class _class = null;
		if (type instanceof ParameterizedType){
		      ParameterizedType paramType = (ParameterizedType) type;
		      Type[] arguments = paramType.getActualTypeArguments();
		      if (arguments[0] instanceof Class) {
		    	  _class = (Class)arguments[0];
					if(!_class.isInterface())
						_class = null;
		      }
		}
		if(_class == null)
				throw new IllegalArgumentException("CallbackService is a parameterized class that must be used with an interface.");
		return _class;
	}
	
	protected T getCallbackProxy() {
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		Object callbackProxy = cachedCallbackProxies.get(thumbprint);
		if(callbackProxy==null){
			return (T)null;
		}
		else{
			return (T)callbackProxy;
		}
	}


	@Override
	public void useThisCallbackSignature(
			MessageBrokerSignature callbackSignature) {
		
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		
		try {
			MessageBroker callbackBroker = CachedMessageBrokers
					.findOrCreate(callbackSignature);
			Object callbackProxy = cachedCallbackProxies.get(thumbprint);
			if (callbackProxy == null) {
				callbackProxy = callbackBroker.createProducer(callbackInterface);
				cachedCallbackProxies.put(thumbprint, callbackProxy);
			}

		} catch (IllegalAccessException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (InstantiationException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (ClassNotFoundException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (InvocationTargetException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		} catch (NoSuchMethodException ex) {
			log.error("Unable to recover the message broker " + callbackSignature, ex);
		}
	}
}
