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
		try {
			new MyTestClass(); // not working!
			Assert.fail("We should have gotten an exception here!");
		} catch (WrongUseOfAnnotationException err) {
			// good
		}
		try {
			new MyTestClass2(); // not working!
			Assert.fail("We should have gotten an exception here!");
		} catch (WrongUseOfAnnotationException err) {
			// good
		}
		
	}

}
