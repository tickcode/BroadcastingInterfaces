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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class BroadcastAppender extends AppenderSkeleton{
	

	RedisMessageBroker broker = null;
	String messageBrokerName = "TEST";
	String host = "localhost";
	int port = 6379;
	
	BroadcastLogger broadcastLogger;
	
	
	public BroadcastAppender() {
	}
	
	@Override
    protected void append(LoggingEvent event) {
		String exceptionMessage = null;
		StackTraceElement[] elements = null;
		if(event.getThrowableInformation() != null && event.getThrowableInformation().getThrowable() != null){
			Throwable throwable = event.getThrowableInformation().getThrowable();
			exceptionMessage = throwable.getMessage();
			elements = throwable.getStackTrace();
		}
		if(event != null && event.getLevel()!=null && event.getMessage() != null)
			broadcastLogger.logEvent(event.getLevel().getSyslogEquivalent(), event.getMessage().toString(), exceptionMessage, elements);
    }
	
	@Override
	public void activateOptions() {
		broker = new RedisMessageBroker(new MessageBrokerSignature(RedisMessageBroker.class.getName(),messageBrokerName, host, port));
		broadcastLogger = broker.createPublisher(BroadcastLogger.class);
		broker.start();
	}

    public void close() {
    	broker.stop();
    }

    public boolean requiresLayout() {
        return false;
    }

    public String getMessageBrokerName() {
		return messageBrokerName;
	}

	public void setMessageBrokerName(String messageBrokerName) {
		this.messageBrokerName = messageBrokerName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
