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

public class BroadcastsWithinBroadcastsTest {

	protected interface InfiniteLoopInterface extends Broadcast {
		public void method1();

		public void method2();
	}

	protected class MyFirstClass implements InfiniteLoopInterface {

		String name;
		int method1;
		int method2;
		MessageBroker broker;
		InfiniteLoopInterface broadcast;

		public MyFirstClass(String name, MessageBroker broker) {
			this.name = name;
			this.broker = broker;
			broadcast = (InfiniteLoopInterface)broker.createProducer(InfiniteLoopInterface.class);
		}

		public void method1() {
			//System.out.println("We invoked method1 on " + name);
			method1++;
			broadcast.method2();
		}

		public void method2() {
			//System.out.println("We invoked method2 on " + name);
			method2++;

			if (method2 > 20)
				throw new StackOverflowError();
		}
	}

	protected class MyErrorHandler implements ErrorHandler {
		int trailSize;
		String trailString;

		@Override
		public void error(String broker, Broadcast broadcast,
				Throwable ex, BreadCrumbTrail trail) {
			this.trailString = trail.toString();
			this.trailSize = trail.size();
			throw new RuntimeException("Are we getting a stack overflow?", ex);
		}
	}

	@Test
	public void testAllowBroadcastingWithinBroadcasting() {
		VMMessageBroker broker = new VMMessageBroker();
		MyErrorHandler handler = new MyErrorHandler();
		broker.addErrorHandler(handler);
		MyFirstClass first = new MyFirstClass("first", broker);
		MyFirstClass second = new MyFirstClass("second", broker);
		broker.addConsumer(first);
		broker.addConsumer(second);

		Assert.assertEquals(0, first.method1);
		Assert.assertEquals(0, first.method2);
		Assert.assertEquals(0, second.method1);
		Assert.assertEquals(0, second.method2);

		try {
			broker.addConsumer(first);
			(broker.createProducer(InfiniteLoopInterface.class)).method1();
			/**
			 * first.method1() first.method1 == 1 first.method2() first.method2
			 * == 1 second.method2() (from broadcast) second.method2 == 1
			 * second.method1() (from broadcast) second.method1 == 1
			 * second.method2() second.method2 == 2 first.method2() (from
			 * broadcast) first.method2 == 2
			 * 
			 */
			 
//We invoked method1 on first
//We invoked method2 on first
//We invoked method2 on second
//We invoked method1 on second
//We invoked method2 on first
//We invoked method2 on second

			Assert.assertEquals(1, first.method1);
			Assert.assertEquals(2, first.method2);
			Assert.assertEquals(1, second.method1);
			Assert.assertEquals(2, second.method2);

		} catch (RuntimeException ex) {
			Assert.assertEquals("Are we getting a stack overflow?",
					ex.getMessage());
			Assert.assertTrue(20 < handler.trailSize);
			// System.out.println(handler.trail);
		}

	}

	@Test
	public void testAllowBroadcastingWithinBroadcastingUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		MyErrorHandler handler = new MyErrorHandler();
		MyFirstClass first = new MyFirstClass("first", broker);
		MyFirstClass second = new MyFirstClass("second", broker);

		Assert.assertEquals(0, first.method1);
		Assert.assertEquals(0, first.method2);
		Assert.assertEquals(0, second.method1);
		Assert.assertEquals(0, second.method2);

		try {
			broker.clear();
			broker.addConsumer(second);
			broker.addErrorHandler(handler);
			broker.addConsumer(first);
			(broker.createProducer(InfiniteLoopInterface.class)).method1();
		} finally {
			broker.removeErrorHandler(handler);
		}

	}

}
