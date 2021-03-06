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

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.tickcode.broadcast.RedisMessageBroker.ThreadSafeVariables;

import com.esotericsoftware.kryo.io.Output;

public class RedisMessageBrokerTest implements java.io.Serializable {

	@Test
	public void testMarshallingData() throws Exception {
		RedisMessageBroker broker = new RedisMessageBroker(new MessageBrokerSignature("LocalTest@localhost"));
		ThreadSafeVariables safe = broker.safeForKryo.get();
		safe.buffer = new byte[3];
		safe.output = new Output(safe.buffer);

		Parameters expectedArgs = new Parameters();
		expectedArgs.setArguments(new Object[] { "Hello", new TickCode() });
		expectedArgs.setTimeSent(System.currentTimeMillis());
		byte[] message = broker.marshall(expectedArgs);
		//Assert.assertEquals(98, message.length);
		Parameters actualArgs = broker.unmarshall(message);

		Assert.assertEquals(expectedArgs.getTimeSent(),
				actualArgs.getTimeSent());
		Assert.assertEquals(expectedArgs.getArguments()[0],
				actualArgs.getArguments()[0]);
	}

	@Test
	public void testManyMarshallingDataCalls() throws Exception {
		RedisMessageBroker broker = new RedisMessageBroker(new MessageBrokerSignature("LocalTest@localhost"));
		Parameters expectedArgs = new Parameters();

		for (int i = 0; i < 1000; i++) {
			expectedArgs.setArguments(new Object[] { "Hello", new TickCode() });
			expectedArgs.setTimeSent(i);
			byte[] message = broker.marshall(expectedArgs);
			Parameters actualArgs = broker.unmarshall(message);

			Assert.assertEquals(expectedArgs.getTimeSent(),
					actualArgs.getTimeSent());
			Assert.assertEquals(expectedArgs.getArguments()[0],
					actualArgs.getArguments()[0]);
			
			String string =  Base64.encodeBase64String(message);
			byte[] bytes = Base64.decodeBase64(string);
			actualArgs = broker.unmarshall(bytes);

			Assert.assertEquals(expectedArgs.getTimeSent(),
					actualArgs.getTimeSent());
			Assert.assertEquals(expectedArgs.getArguments()[0],
					actualArgs.getArguments()[0]);

		}
	}

	private static class TickCode implements java.io.Serializable {
		private double[] array;
		long time;

		public double get(int index) {
			return array[index];
		}
	}

}
