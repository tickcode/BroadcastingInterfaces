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

public class WeakReferenceTest {

	protected interface WeakReferencesTestInterface extends Broadcast {
		public void whatAboutWeakReferences();
	}

	protected class MyTestClass implements WeakReferencesTestInterface {
		int count;

		@BroadcastConsumer
		public void whatAboutWeakReferences() {
			count++;
		}
	}

	protected class MyTestClass2 implements WeakReferencesTestInterface {
		int count;

		@BroadcastProducer
		public void whatAboutWeakReferences() {
			count++;
		}
	}

	@Test
	public void test() {
		MyTestClass consumer = new MyTestClass();
		MyTestClass2 producer = new MyTestClass2();

		Assert.assertEquals(0, consumer.count);
		Assert.assertEquals(0, producer.count);
		producer.whatAboutWeakReferences(); // broadcast
		Assert.assertEquals(1, consumer.count);
		Assert.assertEquals(1, producer.count);

		MessageBroker.get().setWeakReferencesToNull(consumer);
		producer.whatAboutWeakReferences(); // broadcast
		Assert.assertEquals(1, consumer.count); // nothing should change
		Assert.assertEquals(2, producer.count); // of course the producer
												// changes

	}

	@Test
	public void testUsngProxy() {
		try {
			MessageBroker.get().setUsingAspectJ(false);
			MyTestClass consumer = new MyTestClass();
			MyTestClass2 producer = new MyTestClass2();

			WeakReferencesTestInterface consumerProxy = (WeakReferencesTestInterface) BroadcastProxy
					.newInstance(consumer);
			WeakReferencesTestInterface producerProxy = (WeakReferencesTestInterface) BroadcastProxy
					.newInstance(producer);

			Assert.assertEquals(0, consumer.count);
			Assert.assertEquals(0, producer.count);
			producerProxy.whatAboutWeakReferences(); // broadcast
			Assert.assertEquals(1, consumer.count);
			Assert.assertEquals(1, producer.count);

			MessageBroker.get().setWeakReferencesToNull(consumer);
			producerProxy.whatAboutWeakReferences(); // broadcast
			Assert.assertEquals(1, consumer.count); // nothing should change
			Assert.assertEquals(2, producer.count); // of course the producer
													// changes

		} finally {
			MessageBroker.get().setUsingAspectJ(true);
		}
	}

}
