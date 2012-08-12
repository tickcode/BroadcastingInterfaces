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
import org.tickcode.broadcast.BroadcastManager;
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
		try{
			ThisClassAttemptedANonVoidBroadcastMethod _instance = new ThisClassAttemptedANonVoidBroadcastMethod();
			_instance.myNonVoidMethod();
			Assert.fail("We should be throwing an exception here because we tried to create a non-void broadcast method!");
		}catch(NonVoidBroadcastMethodException ex){
			// good
		}
		
	}

}
