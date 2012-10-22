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
package org.tickcode.example.swing;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.tickcode.broadcast.VMMessageBroker;


public class TextChangedBroadcastTest {

	protected class Producer implements TextChangedBroadcast{
		protected String messageReceived;
		@Override
		public void textChanged(String text) {
			this.messageReceived = text;
		}
	};
	protected class ProducerAndConsumer implements TextChangedBroadcast{
		protected String messageReceived;
		public void textChanged(String text) {
			this.messageReceived = text;
		};
	};
	protected class Consumer implements TextChangedBroadcast{
		protected String messageReceived;
		public void textChanged(String text) {
			this.messageReceived = text;
		};
	};
	
	@Test
	public void test() {
		VMMessageBroker broker = new VMMessageBroker();
		Producer producer = new Producer(); 
		//note that we do not add the producer to the broker because
		// it should never consume messages
		ProducerAndConsumer producerAndConsumer = new ProducerAndConsumer(); 
		broker.addConsumer(producerAndConsumer);
		Consumer consumer = new Consumer(); 
		broker.addConsumer(consumer);
		

		String expected = null;
		Assert.assertEquals(expected, producerAndConsumer.messageReceived);
		Assert.assertEquals(expected, consumer.messageReceived);
		
		
		expected = "Hello World from the Producer";
		((TextChangedBroadcast)broker.createProducer(TextChangedBroadcast.class)).textChanged(expected);
		Assert.assertEquals(expected, producerAndConsumer.messageReceived);
		Assert.assertEquals(expected, consumer.messageReceived);
		
		String expectedForConsumer = "Hello World from the ProducerAndConsumer";
		String expectedForProducerAndConsumer = expectedForConsumer;
		producer.messageReceived = null;
		((TextChangedBroadcast)broker.createProducer(TextChangedBroadcast.class)).textChanged(expectedForConsumer);
		Assert.assertEquals(expectedForProducerAndConsumer, producerAndConsumer.messageReceived);
		Assert.assertEquals(expectedForConsumer, consumer.messageReceived);
		Assert.assertNull(producer.messageReceived);
		
	}
	
	@Test
	public void test100Consumers(){
		VMMessageBroker broker = new VMMessageBroker();
		Producer producer = new Producer();
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		
		for(int i=0; i < 100; i++){
			consumers.add(new Consumer());
			broker.addConsumer(consumers.get(i));
		}
		
		for(int i=0; i < 100; i++){
			Assert.assertNull(consumers.get(i).messageReceived);
		}
		
		((TextChangedBroadcast)broker.createProducer(TextChangedBroadcast.class)).textChanged("Hello World");

		for(int i=0; i < 100; i++){
			Assert.assertEquals("Hello World",consumers.get(i).messageReceived);
		}

	}


}
