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
import org.tickcode.broadcast.Broadcast;
import org.tickcode.broadcast.BroadcastConsumer;
import org.tickcode.broadcast.MessageBroker;
import org.tickcode.broadcast.BroadcastProducer;
import org.tickcode.broadcast.ErrorHandler;

public class InterfaceThrowsExceptionTest {

	protected interface DoSomethingInterface extends Broadcast {
		public void doSomething();
	}

	protected class MyErrorHandler implements ErrorHandler{
		Throwable ex;
		public void error(Broadcast broadcast, Throwable ex){
			this.ex = ex;
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
		ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
		ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
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
	public void testErrorHandler() {		
		MyErrorHandler handler = new MyErrorHandler();
		MyErrorHandler handler2 = new MyErrorHandler();
		try{
			ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
			ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
			wellBehaved.doSomething();
			// we are also testing the number of times a method get's invoked as we expect here
			Assert.assertEquals(1, wellBehaved.getCount());
			Assert.assertEquals(0, badBehavior.getCount());
			
			Assert.assertTrue(badBehavior.lastErrorThrown == handler.ex);
			Assert.assertTrue(badBehavior.lastErrorThrown == handler2.ex);
		}finally{
			MessageBroker.getSingleton().unregister(handler);
			MessageBroker.getSingleton().unregister(handler2);
		}
	}

}
