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
