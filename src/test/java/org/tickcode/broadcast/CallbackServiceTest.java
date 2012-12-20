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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class CallbackServiceTest {

	protected interface Question{
		public void question(String message);
	}
	protected interface Answer{
		public void answer(String message);
	}
	
	protected class Callback implements Answer{
		@Override
		public void answer(String message) {
			answer=message;
		}
	}
	
	protected class QuestionService extends CallbackService<Answer> implements Question{
		@Override
		public void question(String message) {
			// TODO Auto-generated method stub
			Answer answerCallback = getCallbackProxy();
			if(answerCallback!=null)
				answerCallback.answer("42");
			else
				log.error("We did not know to whom we shoud call?");
		}
	}
	
//	protected class Server
	
	String answer;
	
	@Before
	public void setup(){
		this.answer = null;
	}
	
	@Test
	public void expectingOnlyInterfaces(){
		VMMessageBroker broker = new VMMessageBroker();
		try{
			CallbackService<Integer> test = new CallbackService<Integer>();
			Assert.fail("We should not allow Integer");
		}catch(IllegalArgumentException ex){
			//good
		}
	}
	
	@Test
	public void helloWorldExample() {
		MessageBroker client = new VMMessageBroker();
		client.addConsumer(new Callback());
		VMMessageBroker server = new VMMessageBroker();
		
		QuestionService service = new QuestionService();
		server.addConsumer(service);
		Assert.assertEquals(1, server.size());
		Assert.assertEquals(2, server.totalMethods());
		
		Question ask = server.createServiceProducer(client, Question.class);
		ask.question("What is the answer to the universe?");
		Assert.assertEquals("42",answer);
		
	}

}
