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

	protected interface ArbitraryMethods{
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

		public void sanityCheckMethod1() {
			countMethod1++;
		}

		public void sanityCheckMethod2(String message) {
			countMethod2++;
			this.message = message;
		}

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

		public void sanityCheckMethod1() {
			countMethod1++;
		}

		public void sanityCheckMethod2(String message) {
			countMethod2++;
			this.message = message;
		}

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
	public void makeSureWeCanBroadcastWithoutEverAddingAConsuemr(){
		VMMessageBroker broker = new VMMessageBroker();
		((ArbitraryMethods)broker.createPublisher(ArbitraryMethods.class)).sanityCheckMethod1();
	}

	@Test
	public void testSanityCheck() {
		VMMessageBroker broker = new VMMessageBroker();
		MyFirstClass first = new MyFirstClass();
		Assert.assertEquals(0, broker.size());

		MySecondClass second = new MySecondClass();
		broker.addSubscriber(second);
		Assert.assertEquals(1, broker.size());
		broker.addSubscriber(first);
		
		(broker.createPublisher(ArbitraryMethods.class)).sanityCheckMethod1();
		Assert.assertEquals(2, broker.size());
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

		(broker.createPublisher(ArbitraryMethods.class)).sanityCheckMethod2("my message");
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

		(broker.createPublisher(ArbitraryMethods.class)).sanityCheckMethod3(first);
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

	}

}
