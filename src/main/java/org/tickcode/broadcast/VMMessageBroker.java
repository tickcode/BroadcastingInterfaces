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
 * of all messaging between decoupled {@link Broadcast} implementations.
 * 
 * @author eyon
 * 
 */
public class VMMessageBroker implements MessageBroker {

	private static Logger logger;
	private static boolean loggingOn;

	private static VMMessageBroker singleton;

	public static VMMessageBroker get() {
		if (singleton == null)
			singleton = new VMMessageBroker();
		return singleton;
	}

	static {
		logger = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
		loggingOn = (logger.getEffectiveLevel() != org.apache.log4j.Level.OFF);
	}

	public VMMessageBroker() {
	}
	
	public String getHost(){
		return host;
	}
	public void setHost(String host){
		this.host = host;
	}

	public <T extends Broadcast> T createProducer(Class<? extends T> _class) {
		ArrayList<Class<? extends T>> broadcastInterfaces = new ArrayList<Class<? extends T>>();
		for (Class<? extends T> _interface : (Class<? extends T>[]) _class
				.getInterfaces()) {
			if (Broadcast.class.isAssignableFrom(_class)) {
				broadcastInterfaces.add(_class);
				addInterface(_class, null);
			}
		}
		if (broadcastInterfaces.size() == 0)
			throw new RuntimeException(
					"You cannot create a producer for an interface that does not extend Broadcast.");
		return (T) BroadcastProducerProxy.newInstance(this, broadcastInterfaces
				.toArray(new Class[broadcastInterfaces.size()]));
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

	protected ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface> interfacesByMethodName = new ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface>();
	protected ConcurrentLinkedQueue<WeakReference<ErrorHandler>> errorHandlers = new ConcurrentLinkedQueue<WeakReference<ErrorHandler>>();
	private ConcurrentHashMap<Broadcast, Broadcast> watchForDuplicatesOfUnderlyingImplementationFromProxies = new ConcurrentHashMap<Broadcast, Broadcast>();
	protected String host = "localhost";
	
	protected class BroadcastConsumersForAGivenInterface {
		Class broadcastInterface;
		Method method;
		CopyOnWriteArrayList<WeakReference<Broadcast>> consumers = new CopyOnWriteArrayList<WeakReference<Broadcast>>();

		BroadcastConsumersForAGivenInterface(Class _interface, Method method) {
			broadcastInterface = _interface;
			this.method = method;
		}

		public void addBroadcastReceiver(Broadcast consumer) {
			if (!weHave(consumer)) {
				consumers.add(new WeakReference<Broadcast>(consumer));
				if (loggingOn) {
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
		protected void setWeakReferencesToNull(Broadcast consumer) {
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Broadcast> c = consumers.get(i);
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

		protected void remove(Broadcast consumer) {
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Broadcast> c = consumers.get(i);
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

		protected boolean weHave(Broadcast consumer) {
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Broadcast> c = consumers.get(i);
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

		protected Set<Broadcast> getConsumers() {
			HashSet<Broadcast> returnConsumers = new HashSet<Broadcast>();
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < consumers.size(); i++) {
				WeakReference<Broadcast> c = consumers.get(i);
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
			for (WeakReference<Broadcast> ref : consumers) {
				if (ref.get() == null)
					consumers.remove(ref);
			}
		}

		public void broadcast(Broadcast producer, Object[] params) {
			BreadCrumbTrail trail = BreadCrumbTrail.get();
			boolean cleanOutWeakReferences = false;
			for (int i = 0; i < this.consumers.size(); i++) {
				WeakReference<Broadcast> ref = this.consumers.get(i);
				Broadcast consumer = ref.get();
				if (consumer != null) {
					try {
						if (consumer != producer) {
							if (loggingOn) {
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
	public void broadcast(Broadcast producer, String methodName, Object[] params) {
		if (loggingOn) {
			logger.debug(methodName + "(" + MethodUtil.getArguments(params)
					+ ")");
		}
		beginBroadcasting(producer, methodName, params);
		BroadcastConsumersForAGivenInterface b = interfacesByMethodName
				.get(methodName);
		if (b != null)
			b.broadcast(producer, params);
		finishedBroadcasting(producer, methodName, params);
	}

	protected void beginBroadcasting(Broadcast producer, String methodName,
			Object[] params) {
		// available for subclasses
	}

	protected void finishedBroadcasting(Broadcast producer, String methodName,
			Object[] params) {
		// available for subclasses
	}

	@Override
	public int size() {
		HashSet<Broadcast> consumer = new HashSet<Broadcast>();
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethodName
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
	public void removeConsumer(Broadcast consumer) {
		// consumer = getBroadcastImplementation(consumer);
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethodName
				.values()) {
			imp.remove(consumer);
		}
	}

	protected void addInterface(Class _interface, Broadcast consumer) {
		if (Broadcast.class.isAssignableFrom(_interface)
				&& Broadcast.class != _interface) { // you cannot just implement
													// {@link Broadcast}.
			if (loggingOn) {
				if (consumer != null)
					logger.debug("Interface: " + _interface.getSimpleName()
							+ " added for "
							+ consumer.getClass().getSimpleName());
			}
			for (Method method : _interface.getMethods()) {
				if (loggingOn) {
					logger.debug("Broadcasting to "
							+ MethodUtil.getReadableMethodString(_interface,
									method));

				}

				if (!Void.TYPE.equals(method.getReturnType())) {
					throw new NonVoidBroadcastMethodException(
							"You tried to implement a non-void broadcast method.  See "
									+ MethodUtil.getReadableMethodString(
											_interface, method));
				}

				BroadcastConsumersForAGivenInterface impl = interfacesByMethodName
						.get(method.getName());
				if (impl == null) {
					impl = new BroadcastConsumersForAGivenInterface(_interface,
							method);
					if (consumer != null)
						impl.addBroadcastReceiver(consumer);
					interfacesByMethodName.put(method.getName(), impl);
				} else if (impl.broadcastInterface != _interface) {
					logger.error("We cannot have two methods with the same name! Please look at "
							+ MethodUtil.getReadableMethodString(
									impl.broadcastInterface, impl.method)
							+ " and "
							+ MethodUtil.getReadableMethodString(_interface,
									method));
					throw new DuplicateMethodException(
							"We cannot have two methods from a Broadcast interface with the same name! Please look at "
									+ MethodUtil.getReadableMethodString(
											impl.broadcastInterface,
											impl.method)
									+ " and "
									+ MethodUtil.getReadableMethodString(
											_interface, method));
				} else {
					if (consumer != null)
						impl.addBroadcastReceiver(consumer);
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
	public void addConsumer(Broadcast consumer) {
		if (!watchForDuplicatesOfUnderlyingImplementationFromProxies
				.containsKey(consumer)) {
			watchForDuplicatesOfUnderlyingImplementationFromProxies.put(
					consumer, consumer);
		} else {
			Broadcast implementation = consumer;
			Broadcast previousProxy = watchForDuplicatesOfUnderlyingImplementationFromProxies
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
		interfacesByMethodName.clear();
		errorHandlers.clear();
	}

	/**
	 * Used for testing to simulate when the garbage collector runs and causes a
	 * WeakReferences to be null for a Broadcast implementation
	 */
	protected void setWeakReferencesToNull(Broadcast consumer) {
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethodName
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
		return "VMMessageBroker";
	}

}
