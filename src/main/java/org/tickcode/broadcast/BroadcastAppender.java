package org.tickcode.broadcast;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
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
		broadcastLogger.logEvent(event.getLevel().getSyslogEquivalent(), event.getMessage().toString(), exceptionMessage, elements);
    }
	
	@Override
	public void activateOptions() {
		broker = RedisMessageBroker.create(messageBrokerName, host, port);
		broadcastLogger = broker.createProducer(BroadcastLogger.class);
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
