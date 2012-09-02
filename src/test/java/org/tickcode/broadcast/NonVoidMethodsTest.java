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
import org.tickcode.broadcast.VMMessageBroker;
import org.tickcode.broadcast.BroadcastProducer;
import org.tickcode.broadcast.ErrorHandler;

public class NonVoidMethodsTest {

	protected interface NonVoidMethod extends Broadcast {
		public int myNonVoidMethod();
	}

	protected class ThisClassAttemptedANonVoidBroadcastMethod implements NonVoidMethod{
		@BroadcastConsumer
		@BroadcastProducer
		public int myNonVoidMethod() {
			return 0;
		}
	}


	@Test
	public void testWeTriedToMakeANonVoidMethod() {
		VMMessageBroker broker = new VMMessageBroker();
		try{
			ThisClassAttemptedANonVoidBroadcastMethod _instance = new ThisClassAttemptedANonVoidBroadcastMethod();
			broker.add(_instance);
			_instance.myNonVoidMethod();
			broker.add(_instance);
			Assert.fail("We should be throwing an exception here because we tried to create a non-void broadcast method!");
		}catch(NonVoidBroadcastMethodException ex){
			// good
			System.out.println(ex.getMessage());
		}
	}

	@Test
	public void testWeTriedToMakeANonVoidMethodUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
		
			try{
				ThisClassAttemptedANonVoidBroadcastMethod _instance = new ThisClassAttemptedANonVoidBroadcastMethod();
				BroadcastProxy.newInstance(broker, _instance);
				_instance.myNonVoidMethod();
				Assert.fail("We should be throwing an exception here because we tried to create a non-void broadcast method!");
			}catch(NonVoidBroadcastMethodException ex){
				// good
			}
		}finally{
			broker.setUsingAspectJ(true);
		}
	}
	
}
