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

public class RemovingConsumersTest {

	protected interface DoSomethingInterface extends Broadcast {
		public void doThis();
	}

	protected class FirstImpl implements DoSomethingInterface {
		int count;

		public void doThis() {
			count++;
		}

		int getCount() {
			return count;
		}
	}

	protected class SecondImpl implements DoSomethingInterface {
		int count;

		public void doThis() {
			count++;
		}

		int getCount() {
			return count;
		}
	}

	@Test
	public void test() {
		VMMessageBroker broker = new VMMessageBroker();
		FirstImpl first = new FirstImpl();
		broker.addConsumer(first);
		SecondImpl second = new SecondImpl();
		broker.addConsumer(second);
		((DoSomethingInterface) broker
				.createProducer(DoSomethingInterface.class)).doThis();

		Assert.assertEquals(1, first.getCount());
		Assert.assertEquals(1, second.getCount());

		broker.removeConsumer(second);
		((DoSomethingInterface) broker
				.createProducer(DoSomethingInterface.class)).doThis();

		Assert.assertEquals(2, first.getCount());
		Assert.assertEquals(1, second.getCount());

	}

	@Test
	public void testUsingProxy() {
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		FirstImpl first = new FirstImpl();
		SecondImpl second = new SecondImpl();
		broker.addConsumer(first);
		broker.addConsumer(second);

		DoSomethingInterface firstProxy = broker
				.createProducer(DoSomethingInterface.class);
		DoSomethingInterface secondProxy = broker
				.createProducer(DoSomethingInterface.class);

		firstProxy.doThis();

		Assert.assertEquals(1, first.getCount());
		Assert.assertEquals(1, second.getCount());

		broker.removeConsumer(secondProxy); // making sure we can unregister the
											// proxy
		firstProxy.doThis();

		Assert.assertEquals(2, first.getCount());
		Assert.assertEquals(2, second.getCount());

		secondProxy.doThis();

		Assert.assertEquals(3, first.getCount());
		Assert.assertEquals(3, second.getCount());

	}

}
