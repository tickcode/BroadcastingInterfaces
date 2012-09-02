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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.tickcode.trace.BreadCrumbTrail;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.SafeEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;

public class RedisMessageBroker extends VMMessageBroker {
	private static Logger logger = Logger.getLogger(org.tickcode.broadcast.RedisMessageBroker.class);

	private String name;
	private JedisPool jedisPool;
	
	private ConcurrentHashMap<String,Broadcast> broadcastProxyByChannel = new ConcurrentHashMap<String, Broadcast>(); 
	
	private Thread thread;
	private Jedis subscriberJedis;

	MyBinarySubscriber subscriber = new MyBinarySubscriber();

	class MyBinarySubscriber extends BinaryJedisPubSub {
		
		@Override
		public void onMessage(byte[] channel, byte[] message) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSubscribe(byte[] channel, int subscribedChannels) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUnsubscribe(byte[] channel, int subscribedChannels) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
			logger.info("RedisMessageBroker shutting down.");
		}

		@Override
		public void onPSubscribe(byte[] pattern, int subscribedChannels) {
			// TODO Auto-generated method stub
			
		}

		public void onPMessage(byte[] pattern, byte[] _channel, byte[] message) {
			String channel = SafeEncoder.encode(_channel);
			Broadcast producerProxy = null;
			try {				
				int firstPeriod = channel.indexOf('.');				
				if(firstPeriod < 1)
					return; // we have an invalid channel
				String redisMessageBrokerName = channel.substring(0, firstPeriod);
				if(!getName().equals(redisMessageBrokerName))
					return; // this channel does not belong to this message broker
				int lastPeriod = channel.lastIndexOf('.');
				if(lastPeriod < 1)
					return; // we have an invalid channel
				String methodName = channel.substring(lastPeriod + 1);
				
				producerProxy = getRedisBroadcastProxy(channel);
				if(producerProxy == null) // we dont' have any Broadcast consumers
				  return;
				
				ByteArrayInputStream bais = new ByteArrayInputStream(message);
				ObjectMapper mapper = new ObjectMapper(new BsonFactory());
				Parameters args = mapper.readValue(bais, Parameters.class);
				RedisMessageBroker.super.broadcast(producerProxy, methodName, args.getArguments());
				
			} catch (Exception ex) {
				logger.error("Unable to process the broadcast.", ex);
				for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
					if (errorHandler.get() != null)
						errorHandler.get().error(RedisMessageBroker.this,
								producerProxy, ex.getCause(), BreadCrumbTrail.get());
					else {
						errorHandlers.remove(errorHandler);
					}
				}
			}

		}

	}


	public RedisMessageBroker(String name, JedisPool jedisPool) {
		this.name = name;
		this.jedisPool = jedisPool;
	}
	
	

	@Override
	public void broadcast(Broadcast producer, String methodName, Object[] params) {
		super.broadcast(producer, methodName, params);
		broadcastToRedisServer(producer, methodName, params);
	}
	
	protected Broadcast getRedisBroadcastProxy(String channel){
		return broadcastProxyByChannel.get(channel);
	}
	
	protected void broadcastToRedisServer(Broadcast producer, String methodName, Object[] params){
		// broadcast to Redis
		BroadcastConsumersForAGivenInterface b = interfacesByMethodName
				.get(methodName);
		String channel = this.createChannelName(b.broadcastInterface.getName(), methodName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BsonFactory fac = new BsonFactory();
		fac.enable(BsonGenerator.Feature.ENABLE_STREAMING);
		ObjectMapper mapper = new ObjectMapper(fac);
		Parameters args = new Parameters();
		args.setArguments(params);
		args.setChannel(channel.toString());
		Jedis jedis = null;
		try {
			mapper.writeValue(baos, args);
			byte[] _channel = SafeEncoder.encode(channel);
			jedis = jedisPool.getResource();
			jedis.publish(_channel, baos.toByteArray());
		} catch (Exception ex) {
			for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
				if (errorHandler.get() != null)
					errorHandler.get().error(this, producer, ex.getCause(),
							BreadCrumbTrail.get());
				else {
					errorHandlers.remove(errorHandler);
				}
			}
		}finally{
			if(jedis != null)
				jedisPool.returnResource(jedis);
		}
	}
	

	@Override
	public void add(Broadcast consumer) {
		super.add(consumer);
		
		// create a subscriber on Redis
		Map<String,Class> channels = getAllBroadcastConsumerMethodNames(name, consumer.getClass());
		for (String channel : channels.keySet()) {
			if(!broadcastProxyByChannel.contains(channel)){
				Class _interface = channels.get(channel);
				Class[] broadcastInterfaces = new Class[]{_interface};
				broadcastProxyByChannel.put(channel, RedisBroadcastProxy.newInstance(this, broadcastInterfaces));
			}			
		}
	}
	
	public void start(){
		if(thread != null)
			thread.interrupt();
		thread = new Thread(){
			public void run(){
				subscriberJedis = jedisPool.getResource();
				logger.info("Whatching pub/sub from " + name + ".*");
				subscriberJedis.psubscribe(subscriber, SafeEncoder.encodeMany(name + ".*"));
			}
		};
		thread.start();
	}
	
	public void stop(){
		if(subscriber.isSubscribed())
			subscriber.punsubscribe();		
		if(subscriberJedis != null){
			jedisPool.returnResource(subscriberJedis);
		}
		thread.interrupt();
		thread = null;
	}
	
	public String getName(){
		return name;
	}
	
	private static StringBuffer builder = new StringBuffer();
	protected String createChannelName(String interfaceName, String methodName){
		builder.setLength(0);
		if(name != null){
			builder.append(name);
			builder.append(".");
		}
		builder.append(interfaceName);
		builder.append(".");
		builder.append(methodName);
		return builder.toString();
	}
	
	private Map<String,Class> getAllBroadcastConsumerMethodNames(String appendString, Class consumer){
		HashSet<String> broadcastConsumerMethods = new HashSet<String>();
		for (Method method : consumer.getMethods()) {
			if (method.isAnnotationPresent(BroadcastConsumer.class)) {
				broadcastConsumerMethods.add(method.getName());
			}
		}
		HashMap<String,Class> readableMethodName = new HashMap<String,Class>();
		for(Class _interface : consumer.getInterfaces()){
			if (Broadcast.class.isAssignableFrom(_interface)
					&& Broadcast.class != _interface) {
				for (Method method : _interface.getMethods()) {
					if(broadcastConsumerMethods.contains(method.getName())){
						readableMethodName.put(createChannelName(_interface.getName(),method.getName()),_interface);
					}
				}
			}			
		}
		return readableMethodName;
	}
	
	protected interface PingRedisMessageBroker extends Broadcast{
		public void ping(String message, long timeSent);
	}
	protected static class WatchPingMessages implements PingRedisMessageBroker{
		String message;
		CountDownLatch latch;
		long timeSent;
		long latency;
		long count;
		
		public WatchPingMessages(CountDownLatch latch){
			this.latch = latch;
		}
		
		@Override
		@BroadcastConsumer
		public void ping(String message, long timeSent) {
			this.message = message;
			this.timeSent = timeSent;
			latency += System.currentTimeMillis() - timeSent;
			count++;
			latch.countDown();
		}
		public String getMessage(){
			return message;
		}
		public long getAverageLatency(){
			return latency / count;
		}
	}
	
	public static void main(String[] args) throws Exception{
		int totalPings = 10;
		CountDownLatch latch = new CountDownLatch(totalPings);
		WatchPingMessages consumer = new WatchPingMessages(latch);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.maxActive = 10;
        poolConfig.maxIdle = 5;
        poolConfig.minIdle = 1;
        poolConfig.testOnBorrow = true;
        poolConfig.numTestsPerEvictionRun = 10;
        poolConfig.timeBetweenEvictionRunsMillis = 60000;
        poolConfig.maxWait = 3000;
        poolConfig.whenExhaustedAction = org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_FAIL;
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 0);
		RedisMessageBroker broker = new RedisMessageBroker("LocalTest", jedisPool);	
		broker.start();
		
		
		broker.add(consumer);
		
		String channel = broker.createChannelName(PingRedisMessageBroker.class.getName(), "ping");
		Broadcast broadcastProxy = broker.getRedisBroadcastProxy(channel);
		
		for(int i = 0; i < totalPings; i++)
			broker.broadcastToRedisServer(broadcastProxy, "ping", new Object[]{"Pong",System.currentTimeMillis()});		
		latch.await(5, TimeUnit.SECONDS);
		if(latch.getCount() > 0){
			logger.error("Never received ping response from Redis server.");			
		}
		
		String pingMessage = consumer.getMessage();		
		if("Pong".equals(pingMessage)){
			logger.info("Redis server appears working. Average response time was " + consumer.getAverageLatency() + " microseconds.");
		}
		else{
			logger.error("Redis server does not appear to be working.");
		}

		broker.stop();
	}
	
}
