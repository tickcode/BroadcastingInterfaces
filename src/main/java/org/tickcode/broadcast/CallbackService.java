package org.tickcode.broadcast;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;

public class CallbackService<R, C> implements
		BroadcastServiceProxy.MessgeBrokerCallbackSignature {
	Logger log = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
	
	ConcurrentHashMap<String, C> cachedCallbackProxies = new ConcurrentHashMap<String, C>();
	Class<? extends C> callbackInteface;

	public CallbackService(MessageBroker serviceMessageBroker,
			Class<? extends C> callbackInteface, R request) {
		this.callbackInteface = callbackInteface;
		serviceMessageBroker.addConsumer(request);
	}

	@Override
	public void useThisCallbackSignature(
			MessageBrokerSignature callbackSignature) {
		
		BreadCrumbTrail trail = BreadCrumbTrail.get();
		String thumbprint = trail.getThumbprint();
		
		try {
			MessageBroker callbackBroker = CachedMessageBrokers
					.findOrCreate(callbackSignature);
			C callbackProxy = cachedCallbackProxies.get(thumbprint);
			if (callbackProxy == null) {
				callbackProxy = callbackBroker.createProducer(callbackInteface);
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
