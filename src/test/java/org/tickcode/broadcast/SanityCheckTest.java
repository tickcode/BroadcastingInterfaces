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

public class SanityCheckTest {

	protected interface ArbitraryMethodsNoBroadcast {
		public void sanityCheckMethod1();

		public void shouldNotBroadcast();
	}

	protected interface ArbitraryMethods extends Broadcast {
		public void sanityCheckMethod1();

		public void sanityCheckMethod2(String message);

		public void sanityCheckMethod3(ArbitraryMethods myself);
	}

	protected class MyFirstClass implements ArbitraryMethods,
			ArbitraryMethodsNoBroadcast {
		int countMethod1;
		int countMethod2;
		int countMethod3;
		int countShouldNotBroadcast;

		String message;
		ArbitraryMethods payload;

		@BroadcastConsumer
		@BroadcastProducer
		public void sanityCheckMethod1() {
			countMethod1++;
		}

		@BroadcastProducer
		@BroadcastConsumer
		public void sanityCheckMethod2(String message) {
			countMethod2++;
			this.message = message;
		}

		@BroadcastConsumer
		@BroadcastProducer
		public void sanityCheckMethod3(ArbitraryMethods payload) {
			countMethod3++;
			this.payload = payload;
		}

		@Override
		public void shouldNotBroadcast() {
			countShouldNotBroadcast++;
		}
	}

	protected class MySecondClass implements ArbitraryMethods,
			ArbitraryMethodsNoBroadcast {
		int countMethod1;
		int countMethod2;
		int countMethod3;
		int countShouldNotBroadcast;

		String message;
		ArbitraryMethods payload;

		@BroadcastConsumer
		@BroadcastProducer
		public void sanityCheckMethod1() {
			countMethod1++;
		}

		@BroadcastConsumer
		@BroadcastProducer
		public void sanityCheckMethod2(String message) {
			countMethod2++;
			this.message = message;
		}

		@BroadcastConsumer
		@BroadcastProducer
		public void sanityCheckMethod3(ArbitraryMethods payload) {
			countMethod3++;
			this.payload = payload;
		}

		@Override
		public void shouldNotBroadcast() {
			countShouldNotBroadcast++;
		}

	}

	@Test
	public void testSanityCheck() {
		VMMessageBroker.setSettingVMMessageBrokerForAll(true);
		VMMessageBroker broker = new VMMessageBroker();
		MyFirstClass first = new MyFirstClass();
		Assert.assertEquals(1, broker.size());

		MySecondClass second = new MySecondClass();
		Assert.assertEquals(2, broker.size());
		// this is not necessary because we have
		// VMMessageBroker.setSettingVMMessageBrokerForAll(true);
		// broker.add(first);
		// broker.add(second);

		Assert.assertTrue(broker.isUsingAspectJ());
		first.sanityCheckMethod1();
		Assert.assertEquals(1, first.countMethod1);
		Assert.assertEquals(1, second.countMethod1);
		Assert.assertEquals(0, first.countMethod2);
		Assert.assertEquals(0, second.countMethod2);
		Assert.assertEquals(0, first.countMethod3);
		Assert.assertEquals(0, second.countMethod3);
		Assert.assertEquals(0, first.countShouldNotBroadcast);
		Assert.assertEquals(0, second.countShouldNotBroadcast);
		Assert.assertNull(first.message);
		Assert.assertNull(second.message);
		Assert.assertNull(first.payload);
		Assert.assertNull(second.payload);

		first.sanityCheckMethod2("my message");
		Assert.assertEquals(1, first.countMethod1);
		Assert.assertEquals(1, second.countMethod1);
		Assert.assertEquals(1, first.countMethod2);
		Assert.assertEquals(1, second.countMethod2);
		Assert.assertEquals(0, first.countMethod3);
		Assert.assertEquals(0, second.countMethod3);
		Assert.assertEquals(0, first.countShouldNotBroadcast);
		Assert.assertEquals(0, second.countShouldNotBroadcast);
		Assert.assertEquals("my message", first.message);
		Assert.assertEquals("my message", second.message);
		Assert.assertNull(first.payload);
		Assert.assertNull(second.payload);

		first.sanityCheckMethod3(first);
		Assert.assertEquals(1, first.countMethod1);
		Assert.assertEquals(1, second.countMethod1);
		Assert.assertEquals(1, first.countMethod2);
		Assert.assertEquals(1, second.countMethod2);
		Assert.assertEquals(1, first.countMethod3);
		Assert.assertEquals(1, second.countMethod3);
		Assert.assertEquals(0, first.countShouldNotBroadcast);
		Assert.assertEquals(0, second.countShouldNotBroadcast);
		Assert.assertEquals("my message", first.message);
		Assert.assertEquals("my message", second.message);
		Assert.assertEquals(first, first.payload);
		Assert.assertEquals(first, second.payload);

		VMMessageBroker.setSettingVMMessageBrokerForAll(false);

	}

	@Test
	public void testWeForgotToUseAMessageBroker() {
		try {
			MyFirstClass first = new MyFirstClass();
			first.sanityCheckMethod1();
			Assert.fail("We should have gotten a NoMessageBrokerException");
		} catch (NoMessageBrokerException ex) {
			// good
		}
	}

	@Test
	public void testWeTriedToAddTwoMessageBrokers() {
		try {
			VMMessageBroker broker1 = new VMMessageBroker();
			VMMessageBroker broker2 = new VMMessageBroker();			
			MyFirstClass first = new MyFirstClass();			
			broker1.add(first);
			broker2.add(first);
			Assert.fail("We should have gotten a OnlyOneMessageBrokerSupportedException");
		} catch (OnlyOneMessageBrokerSupportedException ex) {
			// good
		}
	}

	@Test
	public void testSanityCheckUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try {
			MyFirstClass first = new MyFirstClass();
			MySecondClass second = new MySecondClass();
			ArbitraryMethods firstProxy = (ArbitraryMethods) BroadcastProxy
					.newInstance(broker, first);
			Assert.assertEquals(1, broker.size());
			ArbitraryMethods secondProxy = (ArbitraryMethods) BroadcastProxy
					.newInstance(broker, second);
			Assert.assertEquals(2, broker.size());

			try {
				broker.add(first);
				Assert.fail("Our first implementation should have already been added by the firstProxy.");
			} catch (ProxyImplementationException ex) {
				// good
			}
			try {
				broker.add(firstProxy);
				Assert.fail("Our first implementation should have already been added by the firstProxy.");
			} catch (ProxyImplementationException ex) {
				// good
			}

			firstProxy.sanityCheckMethod1();
			Assert.assertEquals(1, first.countMethod1);
			Assert.assertEquals(1, second.countMethod1);
			Assert.assertEquals(0, first.countMethod2);
			Assert.assertEquals(0, second.countMethod2);
			Assert.assertEquals(0, first.countMethod3);
			Assert.assertEquals(0, second.countMethod3);
			Assert.assertEquals(0, first.countShouldNotBroadcast);
			Assert.assertEquals(0, second.countShouldNotBroadcast);
			Assert.assertNull(first.message);
			Assert.assertNull(second.message);
			Assert.assertNull(first.payload);
			Assert.assertNull(second.payload);

			firstProxy.sanityCheckMethod2("my message");
			Assert.assertEquals(1, first.countMethod1);
			Assert.assertEquals(1, second.countMethod1);
			Assert.assertEquals(1, first.countMethod2);
			Assert.assertEquals(1, second.countMethod2);
			Assert.assertEquals(0, first.countMethod3);
			Assert.assertEquals(0, second.countMethod3);
			Assert.assertEquals(0, first.countShouldNotBroadcast);
			Assert.assertEquals(0, second.countShouldNotBroadcast);
			Assert.assertEquals("my message", first.message);
			Assert.assertEquals("my message", second.message);
			Assert.assertNull(first.payload);
			Assert.assertNull(second.payload);

			firstProxy.sanityCheckMethod3(first);
			Assert.assertEquals(1, first.countMethod1);
			Assert.assertEquals(1, second.countMethod1);
			Assert.assertEquals(1, first.countMethod2);
			Assert.assertEquals(1, second.countMethod2);
			Assert.assertEquals(1, first.countMethod3);
			Assert.assertEquals(1, second.countMethod3);
			Assert.assertEquals(0, first.countShouldNotBroadcast);
			Assert.assertEquals(0, second.countShouldNotBroadcast);
			Assert.assertEquals("my message", first.message);
			Assert.assertEquals("my message", second.message);
			Assert.assertEquals(first, first.payload);
			Assert.assertEquals(first, second.payload);
		} finally {
			broker.setUsingAspectJ(true);
		}
	}

}
