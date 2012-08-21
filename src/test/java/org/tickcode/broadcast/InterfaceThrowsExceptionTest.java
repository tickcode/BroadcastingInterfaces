/*******************************************************************************
 * Copyright © 2012 tickcode.org All rights reserved. 
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

import org.junit.Assert;
import org.junit.Test;
import org.tickcode.trace.BreadCrumbTrail;

public class InterfaceThrowsExceptionTest {

	protected interface DoSomethingInterface extends Broadcast {
		public void doSomething();
	}

	protected class MyErrorHandler implements ErrorHandler{
		Throwable ex;
		int trailSize;
		String trailString;
		public void error(MessageBroker broker, Broadcast broadcast, Throwable ex, BreadCrumbTrail trail){
			this.ex = ex;
			this.trailSize = trail.size();
			this.trailString = trail.toString();
		}
	}

	protected class ThisClassThrowsAnException implements DoSomethingInterface{
		RuntimeException lastErrorThrown = null;
		int count;
		@BroadcastConsumer
		@BroadcastProducer
		public void doSomething() {
			if(count >= 0){
				lastErrorThrown =  new RuntimeException("Ooops we have an exception!");
				throw lastErrorThrown;
			}
			count++;
		}
		int getCount(){
			return count;
		}
	}

	protected class ThisClassDoesNotThrowAnException implements DoSomethingInterface{
		int count;
		@BroadcastConsumer
		@BroadcastProducer
		public void doSomething() {
			count++;
		}
		int getCount(){
			return count;
		}
	}

	@Test
	public void testExceptionDoesNotHurtOtherConsumers() {
		VMMessageBroker broker = new VMMessageBroker();
		ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
		broker.add(wellBehaved);
		ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
		broker.add(badBehavior);
		wellBehaved.doSomething();
		// we are also testing the number of times a method get's invoked as expected here
		Assert.assertEquals(1, wellBehaved.getCount());
		Assert.assertEquals(0, badBehavior.getCount());
		
		try{
			badBehavior.doSomething();
			Assert.fail("We should be throwing an exception here because we are the producer and no consumers should be notified!");
		}catch(RuntimeException ex){
			// good
		}
		
		// Make sure that nothing has changed because the badBehavior was acting as the producer
		Assert.assertEquals(1, wellBehaved.getCount());
		Assert.assertEquals(0, badBehavior.getCount());
		
	}
	
	@Test
	public void testExceptionDoesNotHurtOtherConsumersUsingProxy(){
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
			ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
			ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
			
			broker.add(wellBehaved);
			broker.add(badBehavior);
			
			DoSomethingInterface wellBehavedProxy = (DoSomethingInterface)BroadcastProxy.newInstance(broker, wellBehaved);
			wellBehavedProxy.doSomething();
			// we are also testing the number of times a method get's invoked as expected here
			Assert.assertEquals(1, wellBehaved.getCount());
			Assert.assertEquals(0, badBehavior.getCount());
			
			try{
				DoSomethingInterface badBehaviorProxy = (DoSomethingInterface)BroadcastProxy.newInstance(broker, badBehavior);
				badBehaviorProxy.doSomething();
				Assert.fail("We should be throwing an exception here because we are the producer and no consumers should be notified!");
			}catch(RuntimeException ex){
				// good
			}finally{
				broker.clear();
			}
			
			// Make sure that nothing has changed because the badBehavior was acting as the producer
			Assert.assertEquals(1, wellBehaved.getCount());
			Assert.assertEquals(0, badBehavior.getCount());

		}finally{
			broker.setUsingAspectJ(true);
		}
	}

	@Test
	public void testErrorHandler() {		
		VMMessageBroker broker = new VMMessageBroker();
		MyErrorHandler handler = new MyErrorHandler();
		broker.add(handler);
		MyErrorHandler handler2 = new MyErrorHandler();
		broker.add(handler2);
		try{
			ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
			broker.add(wellBehaved);
			ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
			broker.add(badBehavior);
			wellBehaved.doSomething();
			// we are also testing the number of times a method get's invoked as we expect here
			Assert.assertEquals(1, wellBehaved.getCount());
			Assert.assertEquals(0, badBehavior.getCount());
			
			Assert.assertTrue(badBehavior.lastErrorThrown == handler.ex);
			Assert.assertTrue(badBehavior.lastErrorThrown == handler2.ex);
			Assert.assertEquals(handler.trailString, 1, handler.trailSize);
		}finally{
			broker.remove(handler);
			broker.remove(handler2);
			broker.clear();
		}
	}
	
	@Test
	public void testErrorHandlerUsingProxy(){
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
			MyErrorHandler handler = new MyErrorHandler();
			MyErrorHandler handler2 = new MyErrorHandler();
			try{
				ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
				ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
				broker.add(wellBehaved);
				broker.add(badBehavior);
				broker.add(handler);
				broker.add(handler2);

				((DoSomethingInterface)BroadcastProxy.newInstance(broker, wellBehaved)).doSomething();
				// we are also testing the number of times a method get's invoked as we expect here
				Assert.assertEquals(1, wellBehaved.getCount());
				Assert.assertEquals(0, badBehavior.getCount());
				
				Assert.assertTrue(badBehavior.lastErrorThrown == handler.ex);
				Assert.assertTrue(badBehavior.lastErrorThrown == handler2.ex);
				Assert.assertEquals(handler.trailString, 1, handler.trailSize);
			}finally{
				broker.remove(handler);
				broker.remove(handler2);
				broker.clear();
			}
		}finally{
			broker.setUsingAspectJ(true);
		}
	}

}
