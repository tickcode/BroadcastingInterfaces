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
import org.tickcode.broadcast.InternalRedisTest.ArbitraryMethods;

public class WeakReferenceTest {

	protected interface WeakReferencesTestInterface extends Broadcast {
		public void whatAboutWeakReferences();
	}

	protected class MyTestClass implements WeakReferencesTestInterface {
		int count;

		public void whatAboutWeakReferences() {
			count++;
		}
	}

	protected class MyTestClass2 implements WeakReferencesTestInterface {
		int count;

		public void whatAboutWeakReferences() {
			count++;
		}
	}

	@Test
	public void test() {
		VMMessageBroker broker = new VMMessageBroker();
		MyTestClass consumer = new MyTestClass();
		broker.addConsumer(consumer);
		MyTestClass2 producer = new MyTestClass2();
		broker.addConsumer(producer);

		Assert.assertEquals(0, consumer.count);
		Assert.assertEquals(0, producer.count);
		((WeakReferencesTestInterface)broker.createProducer(producer)).whatAboutWeakReferences(); // broadcast
		Assert.assertEquals(1, consumer.count);
		Assert.assertEquals(1, producer.count);

		broker.setWeakReferencesToNull(consumer);
		((WeakReferencesTestInterface)broker.createProducer(producer)).whatAboutWeakReferences(); // broadcast
		Assert.assertEquals(1, consumer.count); // nothing should change
		Assert.assertEquals(2, producer.count); // of course the producer
												// changes

	}
	
}
