/*******************************************************************************
 * Copyright � 2012 tickcode.org All rights reserved. 
 *  
 * This file is part of the Tickcode collection of software
 *  
 * This file may be distributed under the terms of the tickcode.org 
 * license as defined by tickcode.org and appearing in the file 
 * license.txt included in the packaging of this file. 
 *  
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 *  
 *  See http://www.tickcode.org/LICENSE for licensing information. 
 *   
 *  Contact ask@tickcode.org if any conditions of this licensing 
 *  are not clear to you.
 ******************************************************************************/
package org.tickcode.broadcast;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
public class MessageBroker {

	private static Logger logger;
	public static boolean loggingOn;

	static {
		logger = Logger.getLogger(org.tickcode.broadcast.MessageBroker.class);
		loggingOn = (logger.getEffectiveLevel() != org.apache.log4j.Level.OFF);
	}

	private static MessageBroker singleton;

	public static MessageBroker getSingleton() {
		if (singleton == null)
			singleton = new MessageBroker();
		return singleton;
	}

	private MessageBroker() {
	}

	private boolean allowingBroadcastsToBroadcast = false;
	ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface> interfacesByMethodName = new ConcurrentHashMap<String, BroadcastConsumersForAGivenInterface>();
	ConcurrentLinkedQueue<WeakReference<ErrorHandler>> errorHandlers = new ConcurrentLinkedQueue<WeakReference<ErrorHandler>>();

	public boolean isAllowingBroadcastsToBroadcast() {
		return allowingBroadcastsToBroadcast;
	}

	/**
	 * If you were running a multi-threaded application and you knew that there
	 * would be no infinite loops where broadcasts call broadcasts, you need to
	 * set this variable to true. Otherwise one thread could prevent the
	 * broadcast of the other thread because of shared code.
	 * 
	 * @param allowingInfiniteLoops
	 */
	public void setAllowingBroadcastsToBroadcast(boolean allowingInfiniteLoops) {
		this.allowingBroadcastsToBroadcast = allowingInfiniteLoops;
	}

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
			BreadCrumbTrail trail = BreadCrumbTrail.getActiveTrail();
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
								errorHandler.get().error(consumer,
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

	public void broadcast(Broadcast _this, String methodName, Object[] params) {
		if (loggingOn) {
			logger.debug(methodName + "(" + MethodUtil.getArguments(params)
					+ ")");
		}
		interfacesByMethodName.get(methodName).broadcast(_this, params);
	}

	public void unregister(Broadcast consumer) {
		for (BroadcastConsumersForAGivenInterface imp : interfacesByMethodName
				.values()) {
			imp.remove(consumer);
		}
	}

	public void register(Broadcast _this) {
		HashMap<String, Class> methodsWithAnnotations = new HashMap<String, Class>();
		for (Method method : _this.getClass().getMethods()) {
			if (method.isAnnotationPresent(BroadcastConsumer.class)) {
				methodsWithAnnotations.put(method.getName(),
						BroadcastConsumer.class);
			}
			if (method.isAnnotationPresent(BroadcastProducer.class)) {
				methodsWithAnnotations.put(method.getName(),
						BroadcastConsumer.class);
			}
		}

		for (Class _interface : _this.getClass().getInterfaces()) {
			if (Broadcast.class.isAssignableFrom(_interface)
					&& Broadcast.class != _interface) {
				if (loggingOn) {
					logger.debug("Interface: " + _interface.getSimpleName()
							+ " added for " + _this.getClass().getSimpleName());
				}
				for (Method method : _interface.getMethods()) {
					if (loggingOn) {
						logger.debug("Broadcasting to "
								+ MethodUtil.getReadableMethodString(
										_interface, method));

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
						impl = new BroadcastConsumersForAGivenInterface(
								_interface, method);
						impl.addBroadcastReceiver(_this);
						interfacesByMethodName.put(method.getName(), impl);
						methodsWithAnnotations.remove(method.getName());
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
						impl.addBroadcastReceiver(_this);
						methodsWithAnnotations.remove(method.getName());
					}
				}
			}
		}
		for (String methodName : methodsWithAnnotations.keySet()) {
			String annotation = "@"
					+ methodsWithAnnotations.get(methodName).getSimpleName();
			throw new WrongUseOfAnnotationException("The method "
					+ _this.getClass().getName() + "." + methodName + "(...)"
					+ " has the annotation " + annotation
					+ " but does not implement an interface that extends "
					+ Broadcast.class.getName());
		}
	}

	public void register(ErrorHandler handler) {
		if (!hasErrorHandler(handler)) {
			this.errorHandlers.add(new WeakReference<ErrorHandler>(handler));
		}
	}

	public void unregister(ErrorHandler handler) {
		for (WeakReference<ErrorHandler> h : this.errorHandlers) {
			if (h.get() != null) {
				if (h.get() == handler)
					this.errorHandlers.remove(h);
			} else
				errorHandlers.remove(h);
		}
	}
	
	public void reset(){
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

}
