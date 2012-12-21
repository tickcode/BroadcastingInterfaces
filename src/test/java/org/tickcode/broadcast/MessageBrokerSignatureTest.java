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

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;

public class MessageBrokerSignatureTest {

	@Test
	public void shouldThrowIllegalArgumentException() {
		try{
		MessageBrokerSignature actual = new MessageBrokerSignature("Not valid");
		Assert.fail("Should have thrown an exception");
		}catch(IllegalArgumentException ex){
			// good
		}
	}

	@Test
	public void shouldParseFromString() {
		MessageBrokerSignature expected = new MessageBrokerSignature("my.class.Name", "MyBrokerName", "127.0.0.1", 0);
		MessageBrokerSignature actual = new MessageBrokerSignature(expected.toString());
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void shouldParseFromStringWithPort() {
		MessageBrokerSignature expected = new MessageBrokerSignature("my.class.Name", "MyBrokerName", "127.0.0.1", 1000);
		MessageBrokerSignature actual = new MessageBrokerSignature(expected.toString());
		Assert.assertEquals(expected, actual);
	}
	@Test
	public void shouldDefaultToRedisMessageBroker() {
		MessageBrokerSignature actual = new MessageBrokerSignature("MyBrokerName@somehost");
		Assert.assertEquals(RedisMessageBroker.class.getName(), actual.getClassName());
	}
}
