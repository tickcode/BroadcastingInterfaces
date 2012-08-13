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

public class SanityCheckTest {

	protected interface ArbitraryMethods extends Broadcast {
		public void sanityCheckMethod1();

		public void sanityCheckMethod2(String message);

		public void sanityCheckMethod3(ArbitraryMethods myself);
	}

	protected class MyFirstClass implements ArbitraryMethods {
		int countMethod1;
		int countMethod2;
		int countMethod3;

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
	}

	protected class MySecondClass implements ArbitraryMethods {
		int countMethod1;
		int countMethod2;
		int countMethod3;

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

	}

	@Test
	public void testSanityCheck() {
		MyFirstClass first = new MyFirstClass();
		MySecondClass second = new MySecondClass();

		first.sanityCheckMethod1();
		Assert.assertEquals(1, first.countMethod1);
		Assert.assertEquals(1, second.countMethod1);
		Assert.assertEquals(0, first.countMethod2);
		Assert.assertEquals(0, second.countMethod2);
		Assert.assertEquals(0, first.countMethod3);
		Assert.assertEquals(0, second.countMethod3);
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
		Assert.assertEquals("my message", first.message);
		Assert.assertEquals("my message", second.message);
		Assert.assertEquals(first, first.payload);
		Assert.assertEquals(first, second.payload);

	}

}
