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

import org.junit.Assert;
import org.junit.Test;
import org.tickcode.trace.BreadCrumbTrail;

public class InterfaceThrowsExceptionTest {

	protected interface DoSomethingInterface{
		public void doSomething();
	}

	protected class MyErrorHandler implements ErrorHandler {
		Throwable ex;
		int trailSize;
		String trailString;

		public void error(String broker, Object broadcast,
				Throwable ex, BreadCrumbTrail trail) {
			this.ex = ex;
			this.trailSize = trail.size();
			this.trailString = trail.toString();
		}
	}

	protected class ThisClassThrowsAnException implements DoSomethingInterface {
		RuntimeException lastErrorThrown = null;
		int count;

		public void doSomething() {
			if (count >= 0) {
				lastErrorThrown = new RuntimeException(
						"Ooops we have an exception!");
				throw lastErrorThrown;
			}
			count++;
		}

		int getCount() {
			return count;
		}
	}

	protected class ThisClassDoesNotThrowAnException implements
			DoSomethingInterface {
		int count;

		public void doSomething() {
			count++;
		}

		int getCount() {
			return count;
		}
	}

	@Test
	public void testExceptionDoesNotHurtOtherConsumers() {
		VMMessageBroker broker = new VMMessageBroker();
		MyErrorHandler handler = new MyErrorHandler();
		broker.addErrorHandler(handler);
		ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
		broker.addSubscriber(wellBehaved);
		ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
		broker.addSubscriber(badBehavior);
		wellBehaved.doSomething();
		// we are also testing the number of times a method get's invoked as
		// expected here
		Assert.assertEquals(1, wellBehaved.getCount());
		Assert.assertEquals(0, badBehavior.getCount());
		Assert.assertTrue(handler.ex == null);

		try {
			// verify that we actually do throw an exception as expected...
			badBehavior.doSomething();
			Assert.fail("We should be throwing an exception here because we are the producer and no consumers should be notified!");
		} catch (RuntimeException ex) {
			// good
		}

		Assert.assertTrue(handler.ex == null);
		// Make sure that nothing has changed because the badBehavior was acting
		// as the producer
		Assert.assertEquals(1, wellBehaved.getCount());
		Assert.assertEquals(0, badBehavior.getCount());

		
		try {
			// now use the proxy and confirm the error handler knows about he exception
			DoSomethingInterface badBehaviorProxy = broker.createPublisher(DoSomethingInterface.class);
			badBehaviorProxy.doSomething();
			Assert.assertTrue(handler.ex instanceof RuntimeException);
		} finally {
			broker.clear();
		}

		// Make sure that nothing has changed because the badBehavior was acting
		// as the producer
		Assert.assertEquals(2, wellBehaved.getCount());
		Assert.assertEquals(0, badBehavior.getCount());
		
	}

	@Test
	public void testErrorHandlerUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		MyErrorHandler handler = new MyErrorHandler();
		MyErrorHandler handler2 = new MyErrorHandler();
		try {
			ThisClassDoesNotThrowAnException wellBehaved = new ThisClassDoesNotThrowAnException();
			ThisClassThrowsAnException badBehavior = new ThisClassThrowsAnException();
			broker.addSubscriber(badBehavior);
			broker.addErrorHandler(handler);
			broker.addErrorHandler(handler2);

			DoSomethingInterface badBehaviorProxy = broker.createPublisher(DoSomethingInterface.class);
			broker.addSubscriber(badBehavior);
			broker.addSubscriber(wellBehaved);

			(broker.createPublisher(DoSomethingInterface.class)).doSomething();
			// we are also testing the number of times a method get's invoked as
			// we expect here
			Assert.assertEquals(1, wellBehaved.getCount());
			Assert.assertEquals(0, badBehavior.getCount());

			Assert.assertTrue(badBehavior.lastErrorThrown == handler.ex);
			Assert.assertTrue(badBehavior.lastErrorThrown == handler2.ex);
			Assert.assertEquals(handler.trailString, 1, handler.trailSize);
		} finally {
			broker.removeErrorHandler(handler);
			broker.removeErrorHandler(handler2);
			broker.clear();
		}
	}

}
