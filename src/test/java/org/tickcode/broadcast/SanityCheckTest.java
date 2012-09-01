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
import org.tickcode.broadcast.VMMessageBroker;
import org.tickcode.broadcast.BroadcastProducer;
import org.tickcode.broadcast.ErrorHandler;

public class SanityCheckTest {

	protected interface ArbitraryMethodsNoBroadcast{
		public void sanityCheckMethod1();
		public void shouldNotBroadcast();
	}
	protected interface ArbitraryMethods extends Broadcast {
		public void sanityCheckMethod1();

		public void sanityCheckMethod2(String message);

		public void sanityCheckMethod3(ArbitraryMethods myself);
	}

	protected class MyFirstClass implements ArbitraryMethods,ArbitraryMethodsNoBroadcast {
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

	protected class MySecondClass implements ArbitraryMethods,ArbitraryMethodsNoBroadcast {
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
		MySecondClass second = new MySecondClass();
// this is not necessary because we have VMMessageBroker.setSettingVMMessageBrokerForAll(true);
//		broker.add(first);
//		broker.add(second);

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
	
	public void testWeForgotToUseAMessageBroker(){
		try{
			MyFirstClass first = new MyFirstClass();
			first.sanityCheckMethod1();
			Assert.fail("We should have gotten a NoMessageBrokerException");
		}catch(NoMessageBrokerException ex){
			// good
		}
	}

	@Test
	public void testSanityCheckUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
			MyFirstClass first = new MyFirstClass();
			MySecondClass second = new MySecondClass();
			ArbitraryMethods firstProxy = (ArbitraryMethods)BroadcastProxy.newInstance(broker, first);
			ArbitraryMethods secondProxy = (ArbitraryMethods)BroadcastProxy.newInstance(broker, second);
			
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
		}finally{
			broker.setUsingAspectJ(true);
		}
	}

}
