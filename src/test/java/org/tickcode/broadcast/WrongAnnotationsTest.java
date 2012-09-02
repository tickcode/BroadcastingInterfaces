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
import org.tickcode.broadcast.Broadcast;
import org.tickcode.broadcast.BroadcastConsumer;
import org.tickcode.broadcast.BroadcastProducer;
import org.tickcode.broadcast.WrongUseOfAnnotationException;

public class WrongAnnotationsTest {

	protected interface FirstTestInterface extends Broadcast {
		public void duplicateTest();
	}

	// We "forgot" to implement FirstTestInterface!
	protected class MyTestClass implements Broadcast{
		@BroadcastConsumer
		public void someConsumerHere() {
		}
	}

	// We "forgot" to implement FirstTestInterface!
	protected class MyTestClass2 implements Broadcast{
		@BroadcastProducer
		public void someProducerHere() {
		}
	}
	

	@Test
	public void test() {
		VMMessageBroker broker = new VMMessageBroker();
		try {
			broker.add(new MyTestClass()); // not working!
			Assert.fail("We should have gotten an exception here!");
		} catch (WrongUseOfAnnotationException err) {
			// good
		}
		try {
			broker.add(new MyTestClass2()); // not working!
			Assert.fail("We should have gotten an exception here!");
		} catch (WrongUseOfAnnotationException err) {
			// good
		}
		
	}
	
	@Test
	public void testUsingProxy(){
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
			try {
				BroadcastProxy.newInstance(broker, new MyTestClass()); // not working!
				Assert.fail("We should have gotten an exception here!");
			} catch (WrongUseOfAnnotationException err) {
				// good
			}
			try {
				BroadcastProxy.newInstance(broker, new MyTestClass2()); // not working!
				Assert.fail("We should have gotten an exception here!");
			} catch (WrongUseOfAnnotationException err) {
				// good
			}

		}finally{
			broker.setUsingAspectJ(true);
		}
	}
}
