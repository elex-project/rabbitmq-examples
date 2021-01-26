/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.topic;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 토픽 익스체인지는 라우팅-키를 패턴으로 사용한다.
 *
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-five-java.html"
 */
@Slf4j
public class RabbitClient {
	private static final String EXCHANGE = "elex.topic.exchange";

	private String name;

	private Connection connection;
	private Channel channel;
	private String queue;

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

		// topic은 routing-key를 패턴으로 사용합니다.
		channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, false);

		// 큐 이름을 랜덤으로 생성합니다.
		queue = channel.queueDeclare().getQueue();

	}

	public void consume(String topic) throws IOException {
		// 익스체인지와 큐를 묶습니다.
		channel.queueBind(queue, EXCHANGE, topic);
		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(queue, true, queue, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("Rx: [{}] {} : {}", name, envelope.getRoutingKey(), new String(body, StandardCharsets.UTF_8));
			}
		});
	}

	public void publish(String topic, String message) throws IOException {
		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, topic, null, message.getBytes(StandardCharsets.UTF_8));
		log.info("Tx: [{}] {} : {}", name, topic, message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	public static void main(String... args) throws IOException, TimeoutException {
		RabbitClient producer = new RabbitClient("Producer");
		RabbitClient consumer1 = new RabbitClient("Consumer1");
		RabbitClient consumer2 = new RabbitClient("Consumer2");
		consumer1.consume("message.apple.#");
		consumer2.consume("message.#");

		producer.publish("message.hello", "Hello, there.");
		producer.publish("message.apple", "Hello, apple.");
		producer.publish("message.banana", "Hello, banana.");

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.error("Interrupted..", e);
		}
		producer.close();
		consumer1.close();
		consumer2.close();
		/*
11:29:47.699 [main] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Tx: [Producer] message.hello : Hello, there.
11:29:47.700 [pool-5-thread-4] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Rx: [Consumer2] message.hello : Hello, there.
11:29:47.703 [main] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Tx: [Producer] message.apple : Hello, apple.
11:29:47.703 [main] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Tx: [Producer] message.banana : Hello, banana.
11:29:47.704 [pool-5-thread-5] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Rx: [Consumer2] message.apple : Hello, apple.
11:29:47.704 [pool-5-thread-5] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Rx: [Consumer2] message.banana : Hello, banana.
11:29:47.704 [pool-3-thread-4] INFO kr.pe.elex.rabbitmq.topic.RabbitClient - Rx: [Consumer1] message.apple : Hello, apple.
		 */
	}
}
