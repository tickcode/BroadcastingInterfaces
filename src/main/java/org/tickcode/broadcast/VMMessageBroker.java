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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;
import org.tickcode.trace.DefaultBreadCrumb;
import org.tickcode.trace.MethodUtil;

/**
 * Inspired by http://www.eaipatterns.com/MessageBroker.html
 * 
 * @author eyon
 * 
 */
public class VMMessageBroker extends AbstractMessageBroker {

	private static Logger logger;
	private static boolean loggingOn;
	private static boolean settingVMMessageBrokerForAll;

	static {
		logger = Logger.getLogger(org.tickcode.broadcast.VMMessageBroker.class);
		loggingOn = (logger.getEffectiveLevel() != org.apache.log4j.Level.OFF);
	}

	public VMMessageBroker() {
	}

	protected ThreadLocal<HashSet<String>> methodsOnTheThread = new ThreadLocal<HashSet<String>>(){
		protected java.util.HashSet<String> initialValue() {
			return new HashSet<String>();
		};
	};
	protected ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface> interfacesByMethodName = new ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface>();
	protected ConcurrentLinkedQueue<WeakReference<ErrorHandler>> errorHandlers = new ConcurrentLinkedQueue<WeakReference<ErrorHandler>>();

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
						if (getBroadcastImplementation(consumer) != getBroadcastImplementation(producer)) {
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
								errorHandler.get().error(VMMessageBroker.this, consumer,
										ex.getCause(), trail);
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

	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#broadcast(org.tickcode.broadcast.Broadcast, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void broadcast(Broadcast producer, String methodName, Object[] params) {
		HashSet<String> methodsOnTheCurrentThreadStack = methodsOnTheThread.get();
		if(!methodsOnTheCurrentThreadStack.contains(methodName)){
			if (loggingOn) {
				logger.debug(methodName + "(" + MethodUtil.getArguments(params)
						+ ")");
			}
			methodsOnTheCurrentThreadStack.add(methodName);
			beginBroadcasting(producer, methodName, params);
			interfacesByMethodName.get(methodName).broadcast(producer, params);	
			finishedBroadcasting(producer, methodName,params);
			methodsOnTheCurrentThreadStack.remove(methodName);
		}
	}
	protected void beginBroadcasting(Broadcast producer, String methodName, Object[] params) {
		// available for subclasses
	}
	protected void finishedBroadcasting(Broadcast producer, String methodName, Object[] params) {
		// available for subclasses
	}

	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#remove(org.tickcode.broadcast.Broadcast)
	 */
	@Override
	public void remove(Broadcast consumer) {
		consumer = getBroadcastImplementation(consumer);
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethodName
				.values()) {
			imp.remove(consumer);
		}
		consumer.setMessageBroker(null);
	}

	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#add(org.tickcode.broadcast.Broadcast)
	 */
	@Override
	public void add(Broadcast consumer) {
		consumer = getBroadcastImplementation(consumer);
		consumer.setMessageBroker(this);
		HashSet<String> broadcastConsumerMethods = new HashSet<String>();
		HashMap<String, Class> methodsWithAnnotations = new HashMap<String, Class>();
		for (Method method : consumer.getClass().getMethods()) {
			if (method.isAnnotationPresent(BroadcastConsumer.class)) {
				methodsWithAnnotations.put(method.getName(),
						BroadcastConsumer.class);
				broadcastConsumerMethods.add(method.getName());
			}
			if (method.isAnnotationPresent(BroadcastProducer.class)) {
				methodsWithAnnotations.put(method.getName(),
						BroadcastProducer.class);
			}
		}

		for (Class _interface : consumer.getClass().getInterfaces()) {
			if (Broadcast.class.isAssignableFrom(_interface)
					&& Broadcast.class != _interface) {
				if (loggingOn) {
					logger.debug("Interface: " + _interface.getSimpleName()
							+ " added for " + consumer.getClass().getSimpleName());
				}
				for (Method method : _interface.getMethods()) {
					if (loggingOn) {
						logger.debug("Broadcasting to "
								+ MethodUtil.getReadableMethodString(
										_interface, method));

					}

					if (!"getMessageBroker".equals(method.getName()) && !method.getName().endsWith("$messageBroker") && !Void.TYPE.equals(method.getReturnType())) {
						throw new NonVoidBroadcastMethodException(
								"You tried to implement a non-void broadcast method.  See "
										+ MethodUtil.getReadableMethodString(
												_interface, method));
					}

					BroadcastConsumersForAGivenInterface impl = interfacesByMethodName
							.get(method.getName());
					if (impl == null) {
						impl = new BroadcastConsumersForAGivenInterface(
								_interface, method);
						if(broadcastConsumerMethods.contains(method.getName()))
							impl.addBroadcastReceiver(consumer);
						interfacesByMethodName.put(method.getName(), impl);
						methodsWithAnnotations.remove(method.getName());
					} else if(
								"getMessageBroker".equals(method.getName()) 
								|| "setMessageBroker".equals(method.getName())
								|| method.getName().endsWith("$messageBroker")
								){
						// ignore
					} else if (impl.broadcastInterface != _interface) {
						logger.error("We cannot have two methods with the same name! Please look at "
								+ MethodUtil.getReadableMethodString(
										impl.broadcastInterface, impl.method)
								+ " and "
								+ MethodUtil.getReadableMethodString(
										_interface, method));
						throw new DuplicateMethodException(
								"We cannot have two methods from a Broadcast interface with the same name! Please look at "
										+ MethodUtil.getReadableMethodString(
												impl.broadcastInterface,
												impl.method)
										+ " and "
										+ MethodUtil.getReadableMethodString(
												_interface, method));
					} else {
						if(broadcastConsumerMethods.contains(method.getName()))
							impl.addBroadcastReceiver(consumer);
						methodsWithAnnotations.remove(method.getName());
					}
				}
			}
		}
		for (String methodName : methodsWithAnnotations.keySet()) {
			String annotation = "@"
					+ methodsWithAnnotations.get(methodName).getSimpleName();
			throw new WrongUseOfAnnotationException("The method "
					+ consumer.getClass().getName() + "." + methodName + "(...)"
					+ " has the annotation " + annotation
					+ " but does not implement an interface that extends "
					+ Broadcast.class.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#add(org.tickcode.broadcast.ErrorHandler)
	 */
	@Override
	public void add(ErrorHandler handler) {
		if (!hasErrorHandler(handler)) {
			this.errorHandlers.add(new WeakReference<ErrorHandler>(handler));
		}
	}

	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#remove(org.tickcode.broadcast.ErrorHandler)
	 */
	@Override
	public void remove(ErrorHandler handler) {
		for (WeakReference<ErrorHandler> h : this.errorHandlers) {
			if (h.get() != null) {
				if (h.get() == handler)
					this.errorHandlers.remove(h);
			} else
				errorHandlers.remove(h);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tickcode.broadcast.MessageBroker#clear()
	 */
	@Override
	public void clear(){
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

	public static boolean isSettingVMMessageBrokerForAll() {
		return settingVMMessageBrokerForAll;
	}

	public static void setSettingVMMessageBrokerForAll(
			boolean settingVMMessageBrokerForAll) {
		VMMessageBroker.settingVMMessageBrokerForAll = settingVMMessageBrokerForAll;
	}
	
	public static Broadcast getBroadcastImplementation(Broadcast broadcast){
		if(broadcast instanceof java.lang.reflect.Proxy){
			InvocationHandler handler = ((java.lang.reflect.Proxy)broadcast).getInvocationHandler(broadcast);
			if(handler instanceof GetProxyImplementation){
				return ((GetProxyImplementation)handler).getBroadcastImplementation();
			}
		}
		return broadcast;
	}


}
