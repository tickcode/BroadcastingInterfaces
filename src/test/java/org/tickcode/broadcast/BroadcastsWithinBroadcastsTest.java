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

		public MyFirstClass(String name) {
			this.name = name;
		}

		@BroadcastConsumer
		@BroadcastProducer
		public void method1() {
			System.out.println("We invoked method1 on " + name);
			method1++;
			method2();
		}

		@BroadcastProducer
		@BroadcastConsumer
		public void method2() {
			System.out.println("We invoked method2 on " + name);
			method2++;

			if (method2 > 20)
				throw new StackOverflowError();
		}
	}

	@Test
	public void testDefaultSettings() {
		VMMessageBroker broker = new VMMessageBroker();

		MyFirstClass first = new MyFirstClass("first");
		MyFirstClass second = new MyFirstClass("second");
		broker.add(first);
		broker.add(second);

		Assert.assertEquals(0, first.method1);
		Assert.assertEquals(0, first.method2);
		Assert.assertEquals(0, second.method1);
		Assert.assertEquals(0, second.method2);
		first.method1();
		// method1 gets invoked and then method2 on the first instance
		// then method2 gets broadcasted on the second instance because it has
		// the annotation @BroadcastConsumer
		// and because the advice is after (meaning at the top of the stack)
		// then the method1 gets broadcasted on the second instance because it
		// has the annotation @BroadcastConsumer
		// finally method2 on the second instance get's invoked within method1
		// but it is within the broadcasting
		// so we stop here

		Assert.assertEquals(1, first.method1);
		Assert.assertEquals(1, first.method2);
		Assert.assertEquals(1, second.method1);
		Assert.assertEquals(2, second.method2);

	}

	protected class MyErrorHandler implements ErrorHandler {
		int trailSize;
		String trailString;
		@Override
		public void error(MessageBroker broker, Broadcast broadcast, Throwable ex, BreadCrumbTrail trail) {
			this.trailString = trail.toString();
			this.trailSize = trail.size();
			throw new RuntimeException("Are we getting a stack overflow?", ex);
		}
	}

	@Test
	public void testAllowBroadcastingWithinBroadcasting() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.setAllowingBroadcastsToBroadcast(true);
		MyErrorHandler handler = new MyErrorHandler();
		broker.add(handler);
		MyFirstClass first = new MyFirstClass("first");
		MyFirstClass second = new MyFirstClass("second");
		broker.add(first);
		broker.add(second);

		Assert.assertEquals(0, first.method1);
		Assert.assertEquals(0, first.method2);
		Assert.assertEquals(0, second.method1);
		Assert.assertEquals(0, second.method2);

		try {
			first.method1();
			Assert.fail("We should have an exception here!");
		} catch (RuntimeException ex) {
			Assert.assertEquals("Are we getting a stack overflow?",
					ex.getMessage());
			Assert.assertTrue(20 < handler.trailSize);
			//System.out.println(handler.trail);
		}

	}

	@Test
	public void testAllowBroadcastingWithinBroadcastingUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.setAllowingBroadcastsToBroadcast(true);
		MyErrorHandler handler = new MyErrorHandler();
		MyFirstClass first = new MyFirstClass("first");
		MyFirstClass second = new MyFirstClass("second");

		Assert.assertEquals(0, first.method1);
		Assert.assertEquals(0, first.method2);
		Assert.assertEquals(0, second.method1);
		Assert.assertEquals(0, second.method2);

		try {
			broker.clear();
			broker.setUsingAspectJ(false);
			broker.add(first);
			broker.add(second);
			broker.add(handler);
			((InfiniteLoopInterface)BroadcastProxy.newInstance(broker, first)).method1();
			// proxy does not support broadcasts within broadcasts!
		} finally {
			broker.setAllowingBroadcastsToBroadcast(
					false);
			broker.remove(
					handler);
			broker.setUsingAspectJ(true);
		}

	}
	
}
