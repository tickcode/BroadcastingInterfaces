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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.tickcode.trace.BreadCrumbTrail;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
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
 * {@link Parameters} class and broadcast to other RedisMessageBrokers. <br/>
 * To avoid issues with clients not processing the data quickly enough you can
 * allow unlimited buffering by setting
 * "client-output-buffer-limit pubsub 0 0 0" in your redis.conf
 * 
 * 
 * @author Eyon Land
 * 
 */
public class RedisMessageBroker extends VMMessageBroker {
	private static Logger log = Logger
			.getLogger(org.tickcode.broadcast.RedisMessageBroker.class);
	private static boolean settingRedisMessageBrokerForAll;

	private String name;
	private JedisPool jedisPool;

	ThreadLocal<ThreadSafeVariables> safeForKryo = new ThreadLocal<ThreadSafeVariables>() {
		protected ThreadSafeVariables initialValue() {
			return new ThreadSafeVariables();
		};
	};

	public class ThreadSafeVariables {
		private final Kryo kryo = new Kryo();
		public byte[] buffer = new byte[512];
		Output output = new Output(buffer);
		Input input = new Input();

		public ThreadSafeVariables() {
			kryo.setRegistrationRequired(false);
			kryo.register(Parameters.class);
			kryo.register(StackTraceElement.class).setInstantiator(
					(new SerializingInstantiatorStrategy())
							.newInstantiatorOf(StackTraceElement.class));
		}

	}

	private ConcurrentHashMap<String, Object> broadcastProxyByChannel = new ConcurrentHashMap<String, Object>();
	private ConcurrentHashMap<String, Method> methodByChannel = new ConcurrentHashMap<String, Method>();

	private MyThread thread;
	private ThreadLocal<String> channelBeingBroadcastedFromRedis = new ThreadLocal<String>();
	private long latencyFromOthers;
	private long broadcastsFromOthers;
	private long latencyFromUs;
	private long broadcastsFromUs;

	class MySubscriber extends BinaryJedisPubSub {

		int count;

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
			log.info("No longer watching pub/sub from " + name + ".*");
		}

		@Override
		public void onPSubscribe(byte[] pattern, int subscribedChannels) {
			log.info("Watching pub/sub from " + name + ".*");
		}

		public void onPMessage(byte[] pattern, byte[] _channel, byte[] message) {
			String channel = SafeEncoder.encode(_channel);
			Object producerProxy = null;
			try {
				int firstPeriod = channel.indexOf('.');
				if (firstPeriod < 1) {
					log.warn(channel);
					return; // we have an invalid channel
				}
				String redisMessageBrokerName = channel.substring(0,
						firstPeriod);
				if (!getName().equals(redisMessageBrokerName)) {
					log.warn(channel);
					return; // this channel does not belong to this message
							// broker
				}

				producerProxy = getRedisBroadcastProxy(channel);
				if (producerProxy == null) { // we don't have any Broadcast
												// consumers
					log.warn(channel);
					return;
				}
				count++;

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
					channelBeingBroadcastedFromRedis.set(channel);
					Method method = methodByChannel.get(channel);
					RedisMessageBroker.super.broadcast(producerProxy, method,
							args.getArguments(), args.getThumbprint());
					channelBeingBroadcastedFromRedis.set(null);
					latencyFromOthers = System.currentTimeMillis()
							- args.getTimeSent();
					broadcastsFromOthers++;
				} else {
					latencyFromUs = System.currentTimeMillis()
							- args.getTimeSent();
					broadcastsFromUs++;
				}

			} catch (Exception ex) {
				log.error("Unable to process the broadcast.", ex);
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

	public RedisMessageBroker(String messageBrokerName, String host) {
		this(new MessageBrokerSignature(RedisMessageBroker.class.getName(),
				messageBrokerName, host, 0));
	}

	public RedisMessageBroker(String messageBrokerName, String host, int port) {
		this(new MessageBrokerSignature(RedisMessageBroker.class.getName(),
				messageBrokerName, host, port));
	}

	public RedisMessageBroker(MessageBrokerSignature signature) {
		this(signature.getName(), createJedisPool(signature.getHost(),
				signature.getPort()));
	}

	public static JedisPool createJedisPool(String host, int port) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.maxActive = 10;
		poolConfig.maxIdle = 5;
		poolConfig.minIdle = 5;
		poolConfig.testOnBorrow = true;
		poolConfig.numTestsPerEvictionRun = 10;
		poolConfig.timeBetweenEvictionRunsMillis = 60000;
		poolConfig.maxWait = 3000;
		poolConfig.whenExhaustedAction = org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_GROW;
		if(port == 0)
			port = 6379;
		return new JedisPool(poolConfig, host, port, 0);
	}

	protected RedisMessageBroker(String name, JedisPool jedisPool) {
		this.name = name;
		this.jedisPool = jedisPool;
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

	@Override
	public void finishedBroadcasting(Object producer, Method method,
			Object[] params, String thumbprint) {
		try {
			String channel = createMethodSignatureKey(method);
			if (!channel.equals(channelBeingBroadcastedFromRedis.get()))
				broadcastToRedisServer(producer, method, params, thumbprint);
		} catch (NoSuchMethodException ex) {
			log.error("Unable to broadcast through Redis.", ex);
		}
	}

	public byte[] marshall(Parameters args) throws IOException {
		ThreadSafeVariables safe = safeForKryo.get();
		safe.output.clear();
		boolean bufferIsNotBigEnough = true;
		do {
			try {
				safe.kryo.writeObject(safe.output, args);
				safe.output.flush();
				bufferIsNotBigEnough = false;
			} catch (KryoException ex) {
				if (ex.getMessage().contains("Buffer overflow")) {
					safe.buffer = new byte[(int) (1.25 * safe.buffer.length + 1)];
					safe.output = new Output(safe.buffer);
					safe.output.clear();
				} else {
					throw ex;
				}
			}
		} while (bufferIsNotBigEnough);
		return safe.output.getBuffer();
	}

	public Parameters unmarshall(byte[] message) throws IOException,
			ClassNotFoundException {
		ThreadSafeVariables safe = safeForKryo.get();
		safe.input.setBuffer(message);
		return safe.kryo.readObject(safe.input, Parameters.class);
	}

	protected Object getRedisBroadcastProxy(String channel) {
		return broadcastProxyByChannel.get(channel);
	}

	protected void broadcastToRedisServer(Object producer, Method method,
			Object[] params, String thumbprint) throws NoSuchMethodException {
		// broadcast to Redis
		BroadcastConsumersForAGivenInterface b = interfacesByMethod.get(method);
		String channel = this.createMethodSignatureKey(method);
		Parameters args = new Parameters();
		args.setTimeSent(System.currentTimeMillis());
		args.setThumbprint(thumbprint);
		args.setArguments(params);
		Jedis jedis = null;
		try {
			byte[] _channel = SafeEncoder.encode(channel);
			jedis = jedisPool.getResource();
			jedis.publish(_channel, marshall(args));
		} catch (JedisConnectionException ex) {
			jedisPool.returnBrokenResource(jedis);
			// set to null so we do not return it to the pool below
			jedis = null;
			log.error("Unable to broadcast.", ex);
			for (WeakReference<ErrorHandler> errorHandler : errorHandlers) {
				if (errorHandler.get() != null)
					errorHandler.get().error(this.toString(), producer,
							ex.getCause(), BreadCrumbTrail.get());
				else {
					errorHandlers.remove(errorHandler);
				}
			}
		} catch (Exception ex) {
			log.error("Unable to broadcast.", ex);
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
	public <T extends Object> T createPublisher(Class<? extends T> _class) {
		ThreadSafeVariables safe = safeForKryo.get();
		safe.kryo.register(_class);
		return super.createPublisher(_class);
	}

	@Override
	public void addSubscriber(Object consumer) {
		if (consumer == null)
			throw new IllegalArgumentException(
					"You cannot add null as a valid consumer.");

		start();

		super.addSubscriber(consumer);

		ThreadSafeVariables safe = safeForKryo.get();
		safe.kryo.register(consumer.getClass());

		// create a subscriber on Redis
		for (Class _interface : consumer.getClass().getInterfaces()) {
			for (Method method : _interface.getMethods()) {
				if (Void.TYPE.equals(method.getReturnType())) {
					String channel = this.createMethodSignatureKey(method);
					if (!methodByChannel.containsKey(channel)) {
						safe.kryo.register(_interface);
						Class[] broadcastInterfaces = new Class[] { _interface };
						broadcastProxyByChannel.put(channel,
								BroadcastProducerProxy.newInstance(this,
										broadcastInterfaces));
						methodByChannel.put(channel, method);
					}
				}
			}
		}
	}

	@Override
	public void removeSubscriber(Object consumer) {
		// TODO Auto-generated method stub
		super.removeSubscriber(consumer);
		if (this.size() == 0)
			stop();
	}

	@Override
	public void removeAllSubscribers() {
		super.removeAllSubscribers();
		if (this.size() == 0)
			stop();
	}

	protected class MyThread extends Thread {
		MySubscriber subscriber = new MySubscriber();
		Jedis subscriberJedis;

	}

	protected void start() {
		if (thread == null) {
			thread = new MyThread() {
				public void run() {
					try {
						subscriberJedis = jedisPool.getResource();
						subscriberJedis.psubscribe(subscriber,
								SafeEncoder.encodeMany(name + ".*"));
						jedisPool.returnResource(subscriberJedis);
					} catch (JedisConnectionException ex) {
						jedisPool.returnBrokenResource(subscriberJedis);
					} catch (Exception ex) {
						log.error("Unable to close down the Redis connection.",
								ex);
					}
				}
			};
			thread.start();
		}
	}

	protected void stop() {
		if (thread != null) {
			try {
				if (thread.subscriber.isSubscribed())
					thread.subscriber.punsubscribe();
//				log.info("We received a total of " + thread.subscriber.count
//						+ " messages.");
			} catch (Exception ex) {
				log.error("Unable to close down the Redis connection.", ex);
			}
			thread = null;
		}
	}

	public String getName() {
		return name;
	}

	protected String createMethodSignatureKey(Method method) {
		StringBuilder builder = new StringBuilder();

		Class _interface = method.getDeclaringClass();
		String methodName = method.getName();
		Class[] parameterTypes = method.getParameterTypes();
		builder.setLength(0);
		if (name != null) {
			builder.append(name);
			builder.append(".");
		}

		builder.append(_interface.getName());
		builder.append(".");
		builder.append(methodName);
		builder.append("(");
		for (int i = 0; i < parameterTypes.length; i++) {
			if (i != 0)
				builder.append(",");
			builder.append(parameterTypes[i].getName());
		}
		builder.append(")");
		return builder.toString();
	}

	public static boolean isSettingRedisMessageBrokerForAll() {
		return settingRedisMessageBrokerForAll;
	}

	public static void setSettingRedisMessageBrokerForAll(
			boolean settingRedisMessageBrokerForAll) {
		RedisMessageBroker.settingRedisMessageBrokerForAll = settingRedisMessageBrokerForAll;
	}

	// From here down we are providing a way to check a live Redis system.

	protected static interface PingRedisMessageBroker {
		public void ping(String message, long timeSent, int index);
	}

	protected static class WatchPingMessages implements PingRedisMessageBroker {
		String message;
		CountDownLatch latch;
		long timeSent;
		long latency;
		long count;

		long expectedIndex;
		long expectedCount;

		public WatchPingMessages(CountDownLatch latch) {
			this.latch = latch;
			this.expectedCount = latch.getCount();
		}

		@Override
		public void ping(String message, long timeSent, int index) {
			this.message = message;
			this.timeSent = timeSent;
			latency += System.currentTimeMillis() - timeSent;
			if (index != expectedIndex) {
				// for (long i = expectedIndex; i < index; i++) {
				// log.error("Missed packet " + i);
				// }
				expectedIndex = index;
			}
			this.expectedIndex++;
			this.count++;
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
			broker = new RedisMessageBroker("LocalTest", "localhost");

			int totalPings = 10000;
			CountDownLatch latch = new CountDownLatch(totalPings);
			WatchPingMessages consumer = new WatchPingMessages(latch);
			broker.addSubscriber(consumer);
			Method method = PingRedisMessageBroker.class
					.getDeclaredMethod("ping", new Class[] { String.class,
							long.class, int.class });
			String channel = broker.createMethodSignatureKey(method); // this is
																		// the
																		// name
																		// of
																		// the
																		// method
																		// in
																		// PingRedisMessageBroker
			Object broadcastProxy = broker.getRedisBroadcastProxy(channel);

			// simulate getting a broadcast from another virtual machine through
			// Redis
			String thumbprint = UUID.randomUUID().toString();
			thumbprint = "my fake thumbprint";
			for (int i = 0; i < totalPings; i++) {
				broker.broadcastToRedisServer(broadcastProxy, method,
						new Object[] { "Pong", System.currentTimeMillis(), i },
						thumbprint);
			}
			latch.await(5, TimeUnit.SECONDS);
			if (latch.getCount() > 0) {
				log.error("Never received all the ping responses from Redis server.");
			}

			boolean redisServerWorking = false;
			String pingMessage = consumer.getMessage();
			if ("Pong".equals(pingMessage)) {
				log.info("Average response time was "
						+ consumer.getAverageLatency() + " microseconds for "
						+ (totalPings - latch.getCount())
						+ " pings out of an expected " + totalPings);
				log.info("Average response time from external messages was "
						+ broker.getLatencyFromOthers() + " microseconds.");
				redisServerWorking = true;
			} else {
				log.error("Redis server does not appear to be working.");
			}

			broker.removeSubscriber(consumer);

			if (redisServerWorking)
				checkInternalPing(broker);
			log.info("Finished testing redis.");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			broker.removeAllSubscribers();
		}
	}

	private static void checkInternalPing(RedisMessageBroker broker)
			throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		WatchPingMessages consumer1 = new WatchPingMessages(latch);
		WatchPingMessages consumer2 = new WatchPingMessages(latch);
		broker.addSubscriber(consumer1);
		broker.addSubscriber(consumer2);
		((PingRedisMessageBroker) broker
				.createPublisher(PingRedisMessageBroker.class)).ping(
				"Sending out a ping message", System.currentTimeMillis(), 1);
		latch.await(5, TimeUnit.SECONDS);
		if (latch.getCount() > 0) {
			log.error("Never received ping response internally.");
		} else {
			if (consumer1.getCount() > 1 || consumer2.getCount() > 1) {
				log.error("We are getting too many ping messages internally.");
				throw new Exception(
						"We are getting too many ping messages internally.");
			} else if (consumer1.getCount() == 1 && consumer2.getCount() == 1) {
				log.info("Internal broadcasting looks OK.");
				log.info("Average response time from internal messages was "
						+ broker.getLatencyFromUs() + " microseconds.");
			} else {
				log.error("There's something wrong with the internal broadcasting.");
			}
		}

	}

}
