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
import org.tickcode.broadcast.DuplicateMethodException;

public class DuplicateMethodNameTest {

	protected interface FirstTestInterface extends Broadcast {
		public void duplicateTest();
	}

	protected interface SecondTestInterface extends Broadcast {
		public void duplicateTest();
	}

	protected class MyTestClass implements FirstTestInterface,
			SecondTestInterface {
		@BroadcastConsumer
		public void duplicateTest() {
		}
	}

	protected class TestFirstClass implements FirstTestInterface {
		@BroadcastConsumer
		public void duplicateTest() {
		}
	}

	protected class TestSecondClass implements SecondTestInterface {
		@BroadcastConsumer
		public void duplicateTest() {
		}
	}

	@Test
	public void test() {
		try {
			VMMessageBroker broker = new VMMessageBroker();
			broker.add(new MyTestClass());
			Assert.fail("We should have gotten an exception here!");
		} catch (DuplicateMethodException err) {
			// good
		}
	}

	@Test
	public void testUsingProxy() {
		try {
			VMMessageBroker broker = new VMMessageBroker();
			broker.setUsingAspectJ(false);
			BroadcastProxy.newInstance(broker, new MyTestClass());
			Assert.fail("We should have gotten an exception here!");
		} catch (DuplicateMethodException err) {
			// good
		}
	}

	@Test
	public void testDuplicatesBetweenInterfaces() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.add(new TestFirstClass());
		try {
			broker.add(new TestSecondClass());
			Assert.fail("We should have gotten an exception here!");
		} catch (DuplicateMethodException err) {
			// good
		}
	}

	@Test
	public void testDuplicatesBetweenInterfacesUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.add(new TestFirstClass());
		try {
			AbstractMessageBroker.setUsingAspectJ(false);
			BroadcastProxy.newInstance(broker, new TestSecondClass());
			Assert.fail("We should have gotten an exception here!");
		} catch (DuplicateMethodException err) {
			// good
		}

	}
}
