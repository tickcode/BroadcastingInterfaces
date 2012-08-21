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
import org.tickcode.broadcast.NoMessageBrokerException;

public class RemovingConsumersTest {

	protected interface DoSomethingInterface extends Broadcast {
		public void doThis();
	}

	protected class FirstImpl implements DoSomethingInterface{
		int count;
		@BroadcastConsumer
		@BroadcastProducer
		public void doThis() {
			count++;
		}
		int getCount(){
			return count;
		}
	}

	protected class SecondImpl implements DoSomethingInterface{
		int count;
		@BroadcastConsumer
		@BroadcastProducer
		public void doThis() {
			count++;
		}
		int getCount(){
			return count;
		}
	}

	@Test
	public void test() {
		AbstractMessageBroker.setUsingAspectJ(true);
		VMMessageBroker broker = new VMMessageBroker();
		FirstImpl first = new FirstImpl();
		broker.add(first);
		SecondImpl second = new SecondImpl();
		broker.add(second);
		first.doThis();
		
		Assert.assertEquals(1, first.getCount());
		Assert.assertEquals(1, second.getCount());
		
		broker.remove(second);
		first.doThis();

		Assert.assertEquals(2, first.getCount());
		Assert.assertEquals(1, second.getCount());

		try{
			second.doThis();
			Assert.fail("Should be getting NoMessageBrokerException");
		}catch(NoMessageBrokerException ex){
			// good
		}
		Assert.assertEquals(2, first.getCount());
		Assert.assertEquals(2, second.getCount());

	}
	
	@Test
	public void testUsingProxy(){
		VMMessageBroker broker = new VMMessageBroker();
		broker.clear();
		broker.setUsingAspectJ(false);
		try{
			FirstImpl first = new FirstImpl();
			SecondImpl second = new SecondImpl();
			
			DoSomethingInterface firstProxy = (DoSomethingInterface)BroadcastProxy.newInstance(broker, first);
			DoSomethingInterface secondProxy = (DoSomethingInterface)BroadcastProxy.newInstance(broker, second);
			
			// these four methods are unnecessary but should not hurt anything
			broker.add(first);
			broker.add(second);
			broker.add(firstProxy);
			broker.add(secondProxy);
			
			firstProxy.doThis();
			
			Assert.assertEquals(1, first.getCount());
			Assert.assertEquals(1, second.getCount());
			
			broker.remove(secondProxy); // making sure we can unregister the proxy
			firstProxy.doThis();

			Assert.assertEquals(2, first.getCount());
			Assert.assertEquals(1, second.getCount());

			secondProxy.doThis();

			Assert.assertEquals(3, first.getCount());
			// second get's invoked because we explicitly call the method, not because
			// it was invoked through the broadcast
			Assert.assertEquals(2, second.getCount());
			
		}finally{
			broker.setUsingAspectJ(true);
		}
	}


}
