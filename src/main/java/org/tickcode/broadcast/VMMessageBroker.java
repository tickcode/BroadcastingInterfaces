/*******************************************************************************
 * Copyright (c) 2012, tickcode.org
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of tickcode, nor tickcode.org, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.tickcode.broadcast;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;
import org.tickcode.trace.DefaultBreadCrumb;
import org.tickcode.trace.MethodUtil;

/**
 * Inspired by http://www.eaipatterns.com/MessageBroker.html, this is the heart
 * of all messaging between decoupled {@link Object} implementations.
 * 
 * @author eyon
 * 
 */
public class VMMessageBroker implements MessageBroker {

	private static Logger logger;
	private static boolean loggingOn;

	private static VMMessageBroker singleton;
    private MessageBrokerSignature signature;
    
	public static VMMessageBroker get() {
		if (singleton == null)
			singleton = new VMMessageBroker();
		return singleton;
	}

	static {
		logger = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
		loggingOn = (logger.getEffectiveLevel() != org.apache.log4j.Level.OFF);
	}
	
	public VMMessageBroker(){
		this(new MessageBrokerSignature(VMMessageBroker.class, "VMMessageBroker", "localhost", null));
	}

	public VMMessageBroker(MessageBrokerSignature signature) {
		this.signature = signature;
	}
	
	public <T extends Object> T createProducer(Class<? extends T> _class) {
		if(_class.isInterface()){
			return (T) BroadcastProducerProxy.newInstance(this, new Class[]{_class});
		}
		Class[] interfaces = _class.getInterfaces();
		if(interfaces != null && interfaces.length > 0){
			return (T) BroadcastProducerProxy.newInstance(this, interfaces);
		}
		else{
			throw new UnsupportedOperationException("You may only create a producer from an interface.");
		}
	}

	/**
	 * By using ThreadLocal, every set of method names will be unique per
	 * thread. So while we allow another thread to broadcast on the same method,
	 * for a given thread there can only be method invocations on a specific
	 * method name once per broadcast.
	 */
	protected ThreadLocal<HashSet<String>> methodsOnTheThread = new ThreadLocal<HashSet<String>>() {
		protected java.util.HashSet<String> initialValue() {
			return new HashSet<String>();
		};
	};

	protected ConcurrentHashMap<Method, BroadcastConsumersForAGivenInterface> interfacesByMethod = new ConcurrentHashMap<Method, BroadcastConsumersForAGivenInterface>();
	protected ConcurrentLinkedQueue<WeakReference<ErrorHandler>> errorHandlers = new ConcurrentLinkedQueue<WeakReference<ErrorHandler>>();
	private ConcurrentHashMap<Object, Object> watchForDuplicatesOfUnderlyingImplementationFromProxies = new ConcurrentHashMap<Object, Object>();
	
	protected class BroadcastConsumersForAGivenInterface {
		Class broadcastInterface;
		Method method;
		CopyOnWriteArrayList<WeakReference<Object>> consumers = new CopyOnWriteArrayList<WeakReference<Object>>();

		BroadcastConsumersForAGivenInterface(Class _interface, Method method) {
			broadcastInterface = _interface;
			this.method = method;
		}

		public void addBroadcastReceiver(Object consumer) {
			if (!weHave(consumer)) {
				consumers.add(new WeakReference<Object>(consumer));
				if (loggingOn && logger.isDebugEnabled()) {
					logger.debug(consumer.getClass().getName()
							+ " has implemented "
							+ broadcastInterface.getName()
							+ " and should consume these broadcasts.");
				}
			}
		}

		/**
		 * Used only for testing to simulate when the garbage collector runs and
		 * sets WeakReference values to null
		 * 
		 * @param consumer
		 */
		protected void setWeakReferencesToNull(Object consumer) {
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Object> c = consumers.get(i);
				if (c.get() != null) {
					if (c.get() == consumer) {
						c.clear();
						if (c.get() != null)
							throw new RuntimeException(
									"Expected this to be null!");
					}
				}
			}
		}

		protected void remove(Object consumer) {
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Object> c = consumers.get(i);
				if (c.get() != null) {
					if (c.get() == consumer)
						consumers.remove(c);
				} else
					cleanOutWeakReferences = true;
			}
			if (cleanOutWeakReferences) {
				cleanOutWeakReferences();
			}
		}

		protected boolean weHave(Object consumer) {
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Object> c = consumers.get(i);
				if (c.get() != null) {
					if (c.get() == consumer)
						return true;
				} else
					cleanOutWeakReferences = true;
			}
			if (cleanOutWeakReferences) {
				cleanOutWeakReferences();
			}
			return false;
		}

		protected Set<Object> getConsumers() {
			HashSet<Object> returnConsumers = new HashSet<Object>();
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Object> c = consumers.get(i);
				if (c.get() != null) {
					returnConsumers.add(c.get());
				} else
					cleanOutWeakReferences = true;
			}
			if (cleanOutWeakReferences) {
				cleanOutWeakReferences();
			}
			return returnConsumers;
		}

		protected void cleanOutWeakReferences() {
			for (WeakReference<Object> ref : consumers) {
				if (ref.get() == null)
					consumers.remove(ref);
			}
		}

		public void broadcast(Object producer, Object[] params) {
			BreadCrumbTrail trail = BreadCrumbTrail.get();
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < this.consumers.size(); i++) {
				WeakReference<Object> ref = this.consumers.get(i);
				Object consumer = ref.get();
				if (consumer != null) {
					try {
						if (consumer != producer) {
							if (loggingOn && logger.isDebugEnabled()) {
								logger.debug("We are sending a broadcast to "
										+ consumer.getClass().getName()
										+ " on interface "
										+ MethodUtil.getReadableMethodString(
												broadcastInterface, method,
												params));
							}
							trail.add(new DefaultBreadCrumb(
									MethodUtil
											.getReadableMethodString(
													producer.getClass(),
													method, params),
									MethodUtil.getReadableMethodString(
											consumer.getClass(), method)));
							method.invoke(consumer, params);
						}
					} catch (IllegalAccessException ex) {
						if (loggingOn) {
							logger.error(
									"The consumer "
											+ consumer.getClass().getName()
											+ " on interface "
											+ MethodUtil
													.getReadableMethodString(
															broadcastInterface,
															method, params)
											+ " has an IllegalAccessException!",
									ex);
						}
					} catch (InvocationTargetException ex) {
						if (loggingOn) {
							logger.error(
									"The consumer "
											+ consumer.getClass().getName()
											+ " on interface "
											+ MethodUtil
													.getReadableMethodString(
															broadcastInterface,
															method, params)
											+ " has thrown an exception!", ex
											.getCause());
						}
						for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
							if (errorHandler.get() != null)
								errorHandler.get().error(VMMessageBroker.this.toString(),
										consumer, ex.getCause(), trail);
							else {
								errorHandlers.remove(errorHandler);
							}
						}
					}
				} else {
					cleanOutWeakReferences = true;
				}

			}
			if (cleanOutWeakReferences) {
				cleanOutWeakReferences();
			}
			trail.reset();

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.tickcode.broadcast.MessageBroker#broadcast(org.tickcode.broadcast
	 * .Broadcast, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void broadcast(Object producer, Method m, Object[] params) throws NoSuchMethodException{
		if (loggingOn && logger.isDebugEnabled()) {
			logger.debug(m.getName() + "(" + MethodUtil.getArguments(params)
					+ ")");
		}
		beginBroadcasting(producer, m, params);
		BroadcastConsumersForAGivenInterface b = interfacesByMethod
				.get(m);
		if (b != null)
			b.broadcast(producer, params);
		finishedBroadcasting(producer, m, params);
	}

	protected void beginBroadcasting(Object producer, Method method, Object[] params) {
		// available for subclasses
	}

	protected void finishedBroadcasting(Object producer, Method method, Object[] params) {
		// available for subclasses
	}

	@Override
	public int size() {
		HashSet<Object> consumer = new HashSet<Object>();
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethod
				.values()) {
			consumer.addAll(imp.getConsumers());
		}
		return consumer.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.tickcode.broadcast.MessageBroker#remove(org.tickcode.broadcast.Broadcast
	 * )
	 */
	@Override
	public void removeConsumer(Object consumer) {
		// consumer = getBroadcastImplementation(consumer);
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethod
				.values()) {
			imp.remove(consumer);
		}
	}

	protected void addInterface(Class _interface, Object consumer) {
		if (Object.class.isAssignableFrom(_interface)
				&& Object.class != _interface) { // you cannot just implement
													// {@link Broadcast}.
			if (consumer != null && loggingOn && logger.isDebugEnabled()){
					logger.debug("Interface: " + _interface.getSimpleName()
							+ " added for "
							+ consumer.getClass().getSimpleName());
			}
			for (Method method : _interface.getMethods()) {
				if (loggingOn && logger.isDebugEnabled()) {
					logger.debug("Broadcasting to "
							+ MethodUtil.getReadableMethodString(_interface,
									method));

				}

				if (Void.TYPE.equals(method.getReturnType())) {

					BroadcastConsumersForAGivenInterface impl = interfacesByMethod
							.get(method);
					if (impl == null) {
						impl = new BroadcastConsumersForAGivenInterface(_interface,
								method);
						if (consumer != null)
							impl.addBroadcastReceiver(consumer);
						interfacesByMethod.put(method, impl);
//				} else if (impl.broadcastInterface != _interface) {
//					logger.error("We cannot have two methods with the same name! Please look at "
//							+ MethodUtil.getReadableMethodString(
//									impl.broadcastInterface, impl.method)
//							+ " and "
//							+ MethodUtil.getReadableMethodString(_interface,
//									method));
//					throw new DuplicateMethodException(
//							"We cannot have two methods from a Broadcast interface with the same name! Please look at "
//									+ MethodUtil.getReadableMethodString(
//											impl.broadcastInterface,
//											impl.method)
//									+ " and "
//									+ MethodUtil.getReadableMethodString(
//											_interface, method));
					} else {
						if (consumer != null)
							impl.addBroadcastReceiver(consumer);
					}
				}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.tickcode.broadcast.MessageBroker#add(org.tickcode.broadcast.Broadcast
	 * )
	 */
	@Override
	public void addConsumer(Object consumer) {
		if (!watchForDuplicatesOfUnderlyingImplementationFromProxies
				.containsKey(consumer)) {
			watchForDuplicatesOfUnderlyingImplementationFromProxies.put(
					consumer, consumer);
		} else {
			Object implementation = consumer;
			Object previousProxy = watchForDuplicatesOfUnderlyingImplementationFromProxies
					.get(implementation);
			if (consumer instanceof java.lang.reflect.Proxy
					|| previousProxy instanceof java.lang.reflect.Proxy) {
				throw new ProxyImplementationException(
						"You tried to add a proxy with an implementation that was previously added.");
			}
		}

		for (Class _interface : consumer.getClass().getInterfaces()) {
			addInterface(_interface, consumer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.tickcode.broadcast.MessageBroker#add(org.tickcode.broadcast.ErrorHandler
	 * )
	 */
	@Override
	public void addErrorHandler(ErrorHandler handler) {
		if (!hasErrorHandler(handler)) {
			this.errorHandlers.add(new WeakReference<ErrorHandler>(handler));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tickcode.broadcast.MessageBroker#remove(org.tickcode.broadcast.
	 * ErrorHandler)
	 */
	@Override
	public void removeErrorHandler(ErrorHandler handler) {
		for (WeakReference<ErrorHandler> h : this.errorHandlers) {
			if (h.get() != null) {
				if (h.get() == handler)
					this.errorHandlers.remove(h);
			} else
				errorHandlers.remove(h);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tickcode.broadcast.MessageBroker#clear()
	 */
	@Override
	public void clear() {
		interfacesByMethod.clear();
		errorHandlers.clear();
	}

	/**
	 * Used for testing to simulate when the garbage collector runs and causes a
	 * WeakReferences to be null for a Broadcast implementation
	 */
	protected void setWeakReferencesToNull(Object consumer) {
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethod
				.values()) {
			imp.setWeakReferencesToNull(consumer);
		}
	}

	private boolean hasErrorHandler(ErrorHandler handler) {
		for (WeakReference<ErrorHandler> h : this.errorHandlers) {
			if (h.get() != null) {
				if (h.get() == handler)
					return true;
			} else
				errorHandlers.remove(h);
		}
		return false;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	
	@Override
	public String toString() {
		return signature.toString();
	}
	
	@Override
	public MessageBrokerSignature getSignature() {
		return signature;
	}


}
