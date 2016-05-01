package org.darkphoenixs.kafka.pool;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import kafka.consumer.FetchedDataChunk;
import kafka.consumer.KafkaStream;
import kafka.serializer.DefaultDecoder;
import kafka.utils.VerifiableProperties;

import org.darkphoenixs.kafka.codec.MessageDecoderImpl;
import org.darkphoenixs.kafka.consumer.MessageConsumer;
import org.darkphoenixs.kafka.core.KafkaConstants;
import org.darkphoenixs.kafka.core.KafkaDestination;
import org.darkphoenixs.kafka.core.KafkaMessageAdapter;
import org.darkphoenixs.kafka.listener.MessageConsumerListener;
import org.darkphoenixs.mq.codec.MessageDecoder;
import org.darkphoenixs.mq.message.MessageBeanImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class KafkaMessageReceiverPoolTest {

	static Properties props = new Properties();

	static {
		Resource resource = new DefaultResourceLoader()
				.getResource("kafka/consumer.properties");

		try {
			PropertiesLoaderUtils.fillProperties(props, resource);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test() throws Exception {

		final KafkaMessageReceiverPool<byte[], byte[]> pool = new KafkaMessageReceiverPool<byte[], byte[]>();

		Assert.assertNotNull(pool.getThreadFactory());
		pool.setThreadFactory(new KafkaPoolThreadFactory());

		Assert.assertNotNull(pool.getProps());
		pool.setProps(new Properties());

		Assert.assertEquals(0, pool.getPoolSize());
		pool.setPoolSize(10);

		Assert.assertNull(pool.getZookeeperStr());
		pool.setZookeeperStr("");

		Assert.assertNull(pool.getClientId());
		pool.setClientId("test");

		Assert.assertTrue(pool.getAutoCommit());
		pool.setAutoCommit(false);

		Assert.assertNull(pool.getConfig());
		pool.setConfig(new DefaultResourceLoader()
				.getResource("kafka/consumer1.properties"));
		pool.setConfig(new DefaultResourceLoader()
				.getResource("kafka/consumer.properties"));

		Assert.assertSame(DefaultDecoder.class, pool.getKeyDecoderClass());
		pool.setKeyDecoderClass(DefaultDecoder.class);

		Assert.assertSame(DefaultDecoder.class, pool.getValDecoderClass());
		pool.setValDecoderClass(DefaultDecoder.class);

		Assert.assertNull(pool.getMessageAdapter());
		pool.setMessageAdapter(getAdapter());

		Assert.assertNotNull(pool.getReceiver());

		Thread thread1 = new Thread(new Runnable() {

			@Override
			public void run() {
				Thread thread1 = new Thread(new Runnable() {

					@Override
					public void run() {
						pool.getBrokerStr("QUEUE.TEST");
					}
				});

				thread1.setDaemon(true);

				thread1.start();
			}
		});

		thread1.setDaemon(true);

		thread1.start();

		Thread thread2 = new Thread(new Runnable() {

			@Override
			public void run() {
				Thread thread2 = new Thread(new Runnable() {

					@Override
					public void run() {
						pool.getPartitionNum("QUEUE.TEST");
					}
				});

				thread2.setDaemon(true);

				thread2.start();
			}
		});

		thread2.setDaemon(true);

		thread2.start();

		Thread thread3 = new Thread(new Runnable() {

			@Override
			public void run() {
				pool.init();
			}
		});

		thread3.setDaemon(true);

		thread3.start();

		pool.destroy();

	}

	@Test
	public void test1() throws Exception {

		KafkaMessageReceiverPool<byte[], byte[]> pool = new KafkaMessageReceiverPool<byte[], byte[]>();

		pool.setConfig(new DefaultResourceLoader()
				.getResource("kafka/consumer.properties"));
		pool.setMessageAdapter(getAdapter());
		pool.setAutoCommit(false);

		KafkaStream<byte[], byte[]> stream = new KafkaStream<byte[], byte[]>(
				new LinkedBlockingQueue<FetchedDataChunk>(), 1000,
				new DefaultDecoder(new VerifiableProperties(pool.getProps())),
				new DefaultDecoder(new VerifiableProperties(pool.getProps())),
				"test");

		KafkaMessageReceiverPool<byte[], byte[]>.ReceiverThread thread = pool.new ReceiverThread(
				stream, pool.getMessageAdapter(), 1);

		Thread thread4 = new Thread(thread);

		// thread4.setDaemon(true);

		thread4.start();

	}

	@Test
	public void test2() throws Exception {

		KafkaMessageReceiverPool<byte[], byte[]> pool = new KafkaMessageReceiverPool<byte[], byte[]>();

		pool.setConfig(new DefaultResourceLoader()
				.getResource("kafka/consumer.properties"));
		pool.setMessageAdapter(getAdapter());
		pool.setAutoCommit(false);

		KafkaMessageReceiverPool<byte[], byte[]>.ReceiverThread thread = pool.new ReceiverThread(
				null, pool.getMessageAdapter(), 1);

		Thread thread4 = new Thread(thread);

		// thread4.setDaemon(true);

		thread4.start();

	}

	@Test
	public void test3() throws Exception {

		KafkaMessageReceiverPool<byte[], byte[]> pool = new KafkaMessageReceiverPoolImpl();

		pool.setConfig(new DefaultResourceLoader()
				.getResource("kafka/consumer.properties"));
		pool.setMessageAdapter(getAdapter());
		pool.setAutoCommit(false);
		
		try {
			pool.init();
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}
		
		Assert.assertNotNull(pool.getReceiver());

		pool.destroy();
	}

	private KafkaMessageAdapter<MessageBeanImpl> getAdapter() {

		MessageDecoder<MessageBeanImpl> messageDecoder = new MessageDecoderImpl();

		KafkaDestination kafkaDestination = new KafkaDestination("QUEUE.TEST");

		MessageConsumer<MessageBeanImpl> MessageConsumer = new MessageConsumer<MessageBeanImpl>();

		MessageConsumerListener<MessageBeanImpl> messageConsumerListener = new MessageConsumerListener<MessageBeanImpl>();

		messageConsumerListener.setConsumer(MessageConsumer);

		KafkaMessageAdapter<MessageBeanImpl> messageAdapter = new KafkaMessageAdapter<MessageBeanImpl>();

		messageAdapter.setDecoder(messageDecoder);

		messageAdapter.setDestination(kafkaDestination);

		messageAdapter.setMessageListener(messageConsumerListener);

		return messageAdapter;
	}

	private class KafkaMessageReceiverPoolImpl extends
			KafkaMessageReceiverPool<byte[], byte[]> {

		@Override
		public String getBrokerStr(String topic) {

			return props.getProperty(KafkaConstants.ZOOKEEPER_LIST);
		}

		@Override
		public int getPartitionNum(String topic) {

			return 1;
		}
	}
}
