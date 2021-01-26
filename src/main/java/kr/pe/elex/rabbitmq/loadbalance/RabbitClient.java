/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.loadbalance;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 하나의 큐에 여러 개의 컨슈머를 할당합니다.
 *
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-two-java.html"
 */
@Slf4j
public class RabbitClient {
	private static final String EXCHANGE = "elex.direct.exchange";
	private static final String QUEUE = "elex.queue";
	private static final String ROUTING_KEY = "elex-routing-key";

	private String name;

	private Connection connection;
	private Channel channel;

	RabbitClient(String name) throws IOException, TimeoutException {
		this.name = name;

		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("localhost");
		connectionFactory.setPort(5672);
		connectionFactory.setUsername("elex");
		connectionFactory.setPassword("test");
		connectionFactory.setVirtualHost("/");

		connection = connectionFactory.newConnection();
		channel = connection.createChannel();

		// 익스체인지는 브로커가 메시지를 받는 곳입니다.
		channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT, true);

		// 큐는 브로커가 메시지를 보내는 곳입니다.
		channel.queueDeclare(QUEUE, false, false, false, null);

		// 익스체인지와 큐를 묶습니다.
		channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);

		// 메시지 소비자에 전달할 메시지의 최대 개수입니다. ack를 받을 때까지 메시지 전송을 미룰 수 있습니다.
		channel.basicQos(1);
	}

	public void consume(String consumerTag) throws IOException {
		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(QUEUE, false, consumerTag, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("Rx: [{}] {}", name, new String(body, StandardCharsets.UTF_8));
				try {
					// 메시지를 처리하는데 시간이 좀 걸린다고 가정합니다.
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.error("Interrupted..", e);
				}
				// 메시지 처리 후 ack를 보냅니다.
				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		});
	}

	public void publish(String message) throws IOException {
		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, ROUTING_KEY, null, message.getBytes(StandardCharsets.UTF_8));
		log.info("Tx: [{}] {}", name, message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	public static void main(String... args) throws IOException, TimeoutException {
		RabbitClient producer = new RabbitClient("Producer");
		RabbitClient consumer1 = new RabbitClient("Consumer1");
		RabbitClient consumer2 = new RabbitClient("Consumer2");
		RabbitClient consumer3 = new RabbitClient("Consumer3");
		consumer1.consume(ROUTING_KEY);
		consumer2.consume(ROUTING_KEY);
		consumer3.consume(ROUTING_KEY);
		for (int i = 0; i < 10; i++) {
			producer.publish("Hello, " + i);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.error("Interrupted..", e);
			}
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.error("Interrupted..", e);
		}
		producer.close();
		consumer1.close();
		consumer2.close();
		consumer3.close();
		/*
10:59:25.659 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 0
10:59:25.659 [pool-3-thread-4] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer1] Hello, 0
10:59:25.762 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 1
10:59:25.765 [pool-5-thread-4] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer2] Hello, 1
10:59:25.863 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 2
10:59:25.866 [pool-7-thread-4] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer3] Hello, 2
10:59:25.965 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 3
10:59:26.066 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 4
10:59:26.167 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 5
10:59:26.268 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 6
10:59:26.369 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 7
10:59:26.470 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 8
10:59:26.571 [main] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Tx: [Producer] Hello, 9
10:59:26.664 [pool-3-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer1] Hello, 3
10:59:26.767 [pool-5-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer2] Hello, 4
10:59:26.869 [pool-7-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer3] Hello, 5
10:59:27.665 [pool-3-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer1] Hello, 6
10:59:27.768 [pool-5-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer2] Hello, 7
10:59:27.870 [pool-7-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer3] Hello, 8
10:59:28.666 [pool-3-thread-5] INFO kr.pe.elex.rabbitmq.loadbalance.RabbitClient - Rx: [Consumer1] Hello, 9
*/
	}
}
