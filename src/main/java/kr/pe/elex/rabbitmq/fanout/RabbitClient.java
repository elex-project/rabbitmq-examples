/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.fanout;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 팬 아웃 익스체인지는 라우팅-키 규칙에 무관하게 메시지를 전달한다.
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-two-java.html"
 */
@Slf4j
public class RabbitClient {
	private static final String EXCHANGE = "elex.fanout.exchange";

	private Connection connection;
	private Channel channel;
	private String queue;

	RabbitClient() throws IOException, TimeoutException {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("localhost");
		connectionFactory.setPort(5672);
		connectionFactory.setUsername("elex");
		connectionFactory.setPassword("test");
		connectionFactory.setVirtualHost("/");

		connection = connectionFactory.newConnection();
		channel = connection.createChannel();

		// fanout은 routing-key 규칙을 무시하고, 모든 큐에 메시지를 전달합니다.
		channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT, false);

		// 큐 이름을 랜덤으로 생성합니다.
		queue = channel.queueDeclare().getQueue();

		// 익스체인지와 큐를 묶습니다.
		channel.queueBind(queue, EXCHANGE, "");

	}

	public void consume(String consumerTag) throws IOException {
		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(queue, true, consumerTag, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("Rx: [{}] {}", consumerTag, new String(body, StandardCharsets.UTF_8));
			}
		});
	}

	public void publish(String routingKey, String message) throws IOException {
		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
		log.info("Tx: [{}] {}", routingKey, message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	public static void main(String... args) throws IOException, TimeoutException {
		RabbitClient producer = new RabbitClient();
		RabbitClient consumer1 = new RabbitClient();

		consumer1.consume("");

		for (int i = 0; i < 10; i++) {
			producer.publish(String.valueOf(i), "Hello, " + i);
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

		/*
11:12:40.969 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [0] Hello, 0
11:12:40.969 [pool-3-thread-4] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 0
11:12:41.074 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [1] Hello, 1
11:12:41.077 [pool-3-thread-5] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 1
11:12:41.175 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [2] Hello, 2
11:12:41.178 [pool-3-thread-6] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 2
11:12:41.276 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [3] Hello, 3
11:12:41.279 [pool-3-thread-7] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 3
11:12:41.377 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [4] Hello, 4
11:12:41.380 [pool-3-thread-8] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 4
11:12:41.478 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [5] Hello, 5
11:12:41.481 [pool-3-thread-9] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 5
11:12:41.579 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [6] Hello, 6
11:12:41.582 [pool-3-thread-10] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 6
11:12:41.681 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [7] Hello, 7
11:12:41.684 [pool-3-thread-11] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 7
11:12:41.782 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [8] Hello, 8
11:12:41.785 [pool-3-thread-12] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 8
11:12:41.883 [main] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Tx: [9] Hello, 9
11:12:41.886 [pool-3-thread-13] INFO kr.pe.elex.rabbitmq.fanout.RabbitClient - Rx: [amq.ctag-NJH49xhJFHvgAo90c1qEiA] Hello, 9

		 */
	}
}
