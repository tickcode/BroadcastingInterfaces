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
