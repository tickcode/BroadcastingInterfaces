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

public class InterfaceInheritanceTest {

	protected interface ParentInterface{
		public void parentMethod();
	}

	protected interface ChildInterface extends ParentInterface{
		public void childMethod();
	}

	protected class ClassForParentInterface implements ParentInterface {
		int countParentMethod;
		public void parentMethod() {
			countParentMethod++;
		}
	}

	protected class ClassForChildInterface implements ChildInterface{
		int countParentMethod;
		int countChildMethod;
		@Override
		public void childMethod() {
			countChildMethod++;
		}
		@Override
		public void parentMethod() {
			countParentMethod++;
		}

	}
	
	@Test
	public void testSanityCheck() {
		VMMessageBroker broker = new VMMessageBroker();
		ClassForParentInterface first = new ClassForParentInterface();
		Assert.assertEquals(0, broker.size());

		ClassForChildInterface second = new ClassForChildInterface();
		broker.addSubscriber(second);
		Assert.assertEquals(1, broker.size());
		broker.addSubscriber(first);
		
		(broker.createPublisher(ChildInterface.class)).childMethod();
		Assert.assertEquals(2, broker.size());
		Assert.assertEquals(0, first.countParentMethod);
		Assert.assertEquals(0, second.countParentMethod);
		Assert.assertEquals(1, second.countChildMethod);
		
		(broker.createPublisher(ChildInterface.class)).parentMethod();
		Assert.assertEquals(2, broker.size());
		Assert.assertEquals(1, first.countParentMethod);
		Assert.assertEquals(1, second.countParentMethod);
		Assert.assertEquals(1, second.countChildMethod);
		
		(broker.createPublisher(ParentInterface.class)).parentMethod();
		Assert.assertEquals(2, broker.size());
		Assert.assertEquals(2, first.countParentMethod);
		Assert.assertEquals(2, second.countParentMethod);
		Assert.assertEquals(1, second.countChildMethod);

		(broker.createPublisher(ChildInterface.class)).childMethod();
		Assert.assertEquals(2, broker.size());
		Assert.assertEquals(2, first.countParentMethod);
		Assert.assertEquals(2, second.countParentMethod);
		Assert.assertEquals(2, second.countChildMethod);

	}

}
