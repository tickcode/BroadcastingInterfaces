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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.tickcode.trace.BreadCrumbTrail;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.SafeEncoder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Provides support for getting messages broadcasted through Redis (See <a
 * href="http://redis.io/">http://redis.io/</a> for details). We are currently
 * using Kryo (See <a href="http://code.google.com/p/kryo/"
 * >http://code.google.com/p/kryo/</a> for details) to move our
 * {@link Parameters} class to broadcast to other RedisMessageBrokers.
 * 
 * @author Eyon Land
 * 
 */
public class RedisMessageBroker extends VMMessageBroker {
	private static Logger logger = Logger
			.getLogger(org.tickcode.broadcast.RedisMessageBroker.class);
	private static boolean settingRedisMessageBrokerForAll;

	public static RedisMessageBroker create(String messageBrokerName,
			String host) {
		return RedisMessageBroker.create(messageBrokerName, host, 6379);
	}

	public static RedisMessageBroker create(String messageBrokerName,
			String host, int port) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.maxActive = 10;
		poolConfig.maxIdle = 5;
		poolConfig.minIdle = 2;
		poolConfig.testOnBorrow = true;
		poolConfig.numTestsPerEvictionRun = 10;
		poolConfig.timeBetweenEvictionRunsMillis = 60000;
		poolConfig.maxWait = 3000;
		poolConfig.whenExhaustedAction = org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_FAIL;
		JedisPool jedisPool = new JedisPool(poolConfig, host, port, 0);
		RedisMessageBroker broker = new RedisMessageBroker(messageBrokerName,
				jedisPool);
		broker.setHost(host);
		return broker;
	}

	private String name;
	private JedisPool jedisPool;

	public class ThreadSafeVariables {
		private final Kryo kryo = new Kryo();
		public byte[] buffer = new byte[512];
		Output output = new Output(buffer);
		Input input = new Input();

		public ThreadSafeVariables() {
			initializeKryo.initialize(kryo);
		}
	}

	ThreadLocal<ThreadSafeVariables> threadSafeVariables = new ThreadLocal<ThreadSafeVariables>() {
		protected ThreadSafeVariables initialValue() {
			return new ThreadSafeVariables();
		};
	};

	private ConcurrentHashMap<String, Broadcast> broadcastProxyByChannel = new ConcurrentHashMap<String, Broadcast>();

	private Thread thread;
	private Jedis subscriberJedis;
	private String thumbprint = UUID.randomUUID().toString();
	private volatile String methodBeingBroadcastedFromRedis;
	private long latencyFromOthers;
	private long broadcastsFromOthers;
	private long latencyFromUs;
	private long broadcastsFromUs;
	private InitializeKryo initializeKryo;

	MySubscriber subscriber = new MySubscriber();

	class MySubscriber extends BinaryJedisPubSub {

		@Override
		public void onMessage(byte[] channel, byte[] message) {
		}

		@Override
		public void onSubscribe(byte[] channel, int subscribedChannels) {
		}

		@Override
		public void onUnsubscribe(byte[] channel, int subscribedChannels) {
		}

		@Override
		public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
			logger.info("RedisMessageBroker shutting down.");
		}

		@Override
		public void onPSubscribe(byte[] pattern, int subscribedChannels) {
		}

		public void onPMessage(byte[] pattern, byte[] _channel, byte[] message) {
			String channel = SafeEncoder.encode(_channel);
			Broadcast producerProxy = null;
			try {
				int firstPeriod = channel.indexOf('.');
				if (firstPeriod < 1)
					return; // we have an invalid channel
				String redisMessageBrokerName = channel.substring(0,
						firstPeriod);
				if (!getName().equals(redisMessageBrokerName))
					return; // this channel does not belong to this message
							// broker
				int lastPeriod = channel.lastIndexOf('.');
				if (lastPeriod < 1)
					return; // we have an invalid channel
				String methodName = channel.substring(lastPeriod + 1);

				producerProxy = getRedisBroadcastProxy(channel);
				if (producerProxy == null) // we don't have any Broadcast
											// consumers
					return;

				if (latencyFromOthers > Long.MAX_VALUE / 2
						|| latencyFromUs > Long.MAX_VALUE / 2) {
					latencyFromOthers = 0;
					broadcastsFromOthers = 0;
					latencyFromUs = 0;
					broadcastsFromUs = 0;
				}

				Parameters args = unmarshall(message);
				if (args == null)
					System.out.println("Why is args null?");
				if (!thumbprint.equals(args.getThumbprint())) {
					methodBeingBroadcastedFromRedis = methodName;
					RedisMessageBroker.super.broadcast(producerProxy,
							methodName, args.getArguments());
					methodBeingBroadcastedFromRedis = null;
					latencyFromOthers = System.currentTimeMillis()
							- args.getTimeSent();
					broadcastsFromOthers++;
				}
				{
					latencyFromUs = System.currentTimeMillis()
							- args.getTimeSent();
					broadcastsFromUs++;
				}

			} catch (Exception ex) {
				logger.error("Unable to process the broadcast.", ex);
				for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
					if (errorHandler.get() != null)
						errorHandler.get().error(
								RedisMessageBroker.this.toString(),
								producerProxy, ex.getCause(),
								BreadCrumbTrail.get());
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
		this.initializeKryo = new InitializeKryo() {
			@Override
			public void initialize(Kryo kryo) {
				kryo.setRegistrationRequired(false);
				kryo.register(Parameters.class);
				kryo.register(StackTraceElement.class).setInstantiator(
						(new SerializingInstantiatorStrategy())
								.newInstantiatorOf(StackTraceElement.class));
			}
		};
	}

	public long getLatencyFromUs() {
		if (broadcastsFromUs > 0)
			return this.latencyFromUs / this.broadcastsFromUs;
		else
			return 0;
	}

	public long getLatencyFromOthers() {
		if (broadcastsFromOthers > 0)
			return this.latencyFromOthers / this.broadcastsFromOthers;
		else
			return 0;
	}

	public void finishedBroadcasting(Broadcast producer, String methodName,
			Object[] params) {
		if (!methodName.equals(methodBeingBroadcastedFromRedis))
			broadcastToRedisServer(thumbprint, producer, methodName, params);
	}

	public byte[] marshall(Parameters args) throws IOException {
		ThreadSafeVariables safe = threadSafeVariables.get();
		safe.output.clear();
		boolean bufferIsNotBigEnough = true;
		do {
			try {
				safe.kryo.writeObject(safe.output, args);
				bufferIsNotBigEnough = false;
			} catch (KryoException ex) {
				if (ex.getMessage().contains("Buffer overflow")) {
					safe.buffer = new byte[(int) (1.25 * safe.buffer.length + 1)];
					safe.output = new Output(safe.buffer);
				} else {
					throw ex;
				}
			}
		} while (bufferIsNotBigEnough);
		safe.output.flush();
		return safe.output.getBuffer();
	}

	public Parameters unmarshall(byte[] message) throws IOException,
			ClassNotFoundException {
		ThreadSafeVariables safe = threadSafeVariables.get();
		safe.input.setBuffer(message);
		return safe.kryo.readObject(safe.input, Parameters.class);
	}

	protected Broadcast getRedisBroadcastProxy(String channel) {
		return broadcastProxyByChannel.get(channel);
	}

	protected void broadcastToRedisServer(String thumbprint,
			Broadcast producer, String methodName, Object[] params) {
		// broadcast to Redis
		BroadcastConsumersForAGivenInterface b = interfacesByMethodName
				.get(methodName);
		String channel = this.createChannelName(b.broadcastInterface.getName(),
				methodName);
		Parameters args = new Parameters();
		args.setTimeSent(System.currentTimeMillis());
		args.setThumbprint(thumbprint);
		args.setArguments(params);
		args.setChannel(channel.toString());
		Jedis jedis = null;
		try {
			byte[] _channel = SafeEncoder.encode(channel);
			jedis = jedisPool.getResource();
			jedis.publish(_channel, marshall(args));
		} catch (Exception ex) {
			for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
				if (errorHandler.get() != null)
					errorHandler.get().error(this.toString(), producer,
							ex.getCause(), BreadCrumbTrail.get());
				else {
					errorHandlers.remove(errorHandler);
				}
			}
		} finally {
			if (jedis != null) {
				jedisPool.returnResource(jedis);
				jedis = null;
			}
		}
	}

	@Override
	public <T extends Broadcast> T createProducer(Class<? extends T> _class) {
		ThreadSafeVariables safe = threadSafeVariables.get();
		safe.kryo.register(_class);
		return super.createProducer(_class);
	}

	@Override
	public void addConsumer(Broadcast consumer) {
		super.addConsumer(consumer);

		ThreadSafeVariables safe = threadSafeVariables.get();
		safe.kryo.register(consumer.getClass());

		// create a subscriber on Redis
		Map<String, Class> channels = getAllBroadcastConsumerMethodNames(name,
				consumer.getClass());
		for (String channel : channels.keySet()) {
			if (!broadcastProxyByChannel.contains(channel)) {
				Class _interface = channels.get(channel);
				safe.kryo.register(_interface);
				Class[] broadcastInterfaces = new Class[] { _interface };
				broadcastProxyByChannel.put(channel, BroadcastProducerProxy
						.newInstance(this, broadcastInterfaces));
			}
		}
	}

	public void start() {
		super.start();
		if (thread == null) {
			thread = new Thread() {
				public void run() {
					subscriberJedis = jedisPool.getResource();
					logger.info("Watching pub/sub from " + name + ".*");
					subscriberJedis.psubscribe(subscriber,
							SafeEncoder.encodeMany(name + ".*"));
				}
			};
			thread.start();
		}
	}

	public void stop() {
		super.stop();
		if (thread != null) {
			if (subscriber.isSubscribed())
				subscriber.punsubscribe();
			if (subscriberJedis != null) {
				jedisPool.returnResource(subscriberJedis);
			}
			thread = null;
		}
	}

	public String getName() {
		return name;
	}

	public void setInitializeKryo(InitializeKryo initializeKryo) {
		this.initializeKryo = initializeKryo;
	}

	private static StringBuffer builder = new StringBuffer();

	protected String createChannelName(String interfaceName, String methodName) {
		builder.setLength(0);
		if (name != null) {
			builder.append(name);
			builder.append(".");
		}
		builder.append(interfaceName);
		builder.append(".");
		builder.append(methodName);
		return builder.toString();
	}

	private Map<String, Class> getAllBroadcastConsumerMethodNames(
			String appendString, Class consumer) {
		HashSet<String> broadcastConsumerMethods = new HashSet<String>();
		for (Method method : consumer.getMethods()) {
			broadcastConsumerMethods.add(method.getName());
		}
		HashMap<String, Class> readableMethodName = new HashMap<String, Class>();
		for (Class _interface : consumer.getInterfaces()) {
			if (Broadcast.class.isAssignableFrom(_interface)
					&& Broadcast.class != _interface) {
				for (Method method : _interface.getMethods()) {
					if (broadcastConsumerMethods.contains(method.getName())) {
						readableMethodName.put(
								createChannelName(_interface.getName(),
										method.getName()), _interface);
					}
				}
			}
		}
		return readableMethodName;
	}

	public static boolean isSettingRedisMessageBrokerForAll() {
		return settingRedisMessageBrokerForAll;
	}

	public static void setSettingRedisMessageBrokerForAll(
			boolean settingRedisMessageBrokerForAll) {
		RedisMessageBroker.settingRedisMessageBrokerForAll = settingRedisMessageBrokerForAll;
	}

	// From here down we are providing a way to check a live Redis system.

	protected static interface PingRedisMessageBroker extends Broadcast {
		public void ping(String message, long timeSent);
	}

	protected static class WatchPingMessages implements PingRedisMessageBroker {
		String message;
		CountDownLatch latch;
		long timeSent;
		long latency;
		long count;

		public WatchPingMessages(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public void ping(String message, long timeSent) {
			this.message = message;
			this.timeSent = timeSent;
			latency += System.currentTimeMillis() - timeSent;
			count++;
			latch.countDown();
		}

		public String getMessage() {
			return message;
		}

		public long getAverageLatency() {
			return latency / count;
		}

		public long getCount() {
			return count;
		}
	}

	public static void main(String[] args) throws Exception {

		RedisMessageBroker broker = null;
		try {
			broker = RedisMessageBroker.create("LocalTest", "localhost");
			broker.start();

			int totalPings = 1000;
			CountDownLatch latch = new CountDownLatch(totalPings);
			WatchPingMessages consumer = new WatchPingMessages(latch);
			broker.addConsumer(consumer);

			String channel = broker.createChannelName(
					PingRedisMessageBroker.class.getName(), "ping"); // this is
																		// the
																		// name
																		// of
																		// the
																		// method
																		// in
																		// PingRedisMessageBroker
			Broadcast broadcastProxy = broker.getRedisBroadcastProxy(channel);

			// simulate getting a broadcast from another virtual machine through
			// Redis
			String thumbprint = UUID.randomUUID().toString();
			for (int i = 0; i < totalPings; i++) {
				broker.broadcastToRedisServer(thumbprint, broadcastProxy,
						"ping",
						new Object[] { "Pong", System.currentTimeMillis() });
			}
			latch.await(5, TimeUnit.SECONDS);
			if (latch.getCount() > 0) {
				logger.error("Never received all the ping responses from Redis server.");
			}

			boolean redisServerWorking = false;
			String pingMessage = consumer.getMessage();
			if ("Pong".equals(pingMessage)) {
				logger.info("Average response time was "
						+ consumer.getAverageLatency() + " microseconds for "
						+ (totalPings - latch.getCount()) + " pings.");
				logger.info("Average response time from external messages was "
						+ broker.getLatencyFromOthers() + " microseconds.");
				redisServerWorking = true;
			} else {
				logger.error("Redis server does not appear to be working.");
			}

			if (redisServerWorking)
				checkInternalPing(broker);
			logger.info("Finished testing redis.");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			broker.stop();
		}
	}

	private static void checkInternalPing(RedisMessageBroker broker)
			throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		WatchPingMessages consumer1 = new WatchPingMessages(latch);
		WatchPingMessages consumer2 = new WatchPingMessages(latch);
		broker.addConsumer(consumer1);
		broker.addConsumer(consumer2);
		((PingRedisMessageBroker) broker
				.createProducer(PingRedisMessageBroker.class)).ping(
				"Sending out a ping message", System.currentTimeMillis());
		latch.await(5, TimeUnit.SECONDS);
		if (latch.getCount() > 0) {
			logger.error("Never received ping response internally.");
		} else {
			if (consumer1.getCount() > 1 || consumer2.getCount() > 1) {
				logger.error("We are getting too many ping messages internally.");
				throw new Exception(
						"We are getting too many ping messages internally.");
			} else if (consumer1.getCount() == 1 && consumer2.getCount() == 1) {
				logger.info("Internal broadcasting looks OK.");
				logger.info("Average response time from internal messages was "
						+ broker.getLatencyFromUs() + " microseconds.");
			} else {
				logger.error("There's something wrong with the internal broadcasting.");
			}
		}

	}

	@Override
	public String toString() {
		return this.name + "@" + this.host;
	}

}
