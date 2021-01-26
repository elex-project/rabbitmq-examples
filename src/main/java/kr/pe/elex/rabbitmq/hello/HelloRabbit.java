/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.hello;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 다이렉트 익스체인지 샘플
 * <p>
 * 익스체인지는 브로커가 메시지를 받는 곳이다.
 * 큐는 브로커가 메시지를 보내는 곳이다.
 * 익스체인지와 큐는 라우팅-키로 서로 바인딩된다.
 * <p>
 * 익스체인지에 메시지가 도착하면 메시지의 라우팅-키와 일치하는 큐로 메시지를 보낸다.
 *
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-one-java.html"
 */
@Slf4j
public class HelloRabbit {
	private static final String EXCHANGE = "elex.direct.exchange";
	private static final String QUEUE = "elex.queue.01";
	private static final String ROUTING_KEY = "elex-routing-key";

	private Connection connection;
	private Channel channel;

	HelloRabbit() throws IOException, TimeoutException {
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

		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(QUEUE, true, ROUTING_KEY, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("Rx: {}", new String(body, StandardCharsets.UTF_8));
			}
		});
	}

	public void publish(String message) throws IOException {
		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, ROUTING_KEY, null, message.getBytes(StandardCharsets.UTF_8));
		log.info("Tx: {}", message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	public static void main(String... args) throws IOException, TimeoutException {
		HelloRabbit helloRabbit = new HelloRabbit();
		for (int i = 0; i < 10; i++) {
			helloRabbit.publish("Hello, " + i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("Interrupted..", e);
			}
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			log.error("Interrupted..", e);
		}
		helloRabbit.close();
		/*
11:01:31.502 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 0
11:01:31.502 [pool-1-thread-4] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 0
11:01:32.505 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 1
11:01:32.509 [pool-1-thread-5] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 1
11:01:33.507 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 2
11:01:33.510 [pool-1-thread-6] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 2
11:01:34.508 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 3
11:01:34.511 [pool-1-thread-7] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 3
11:01:35.509 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 4
11:01:35.512 [pool-1-thread-8] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 4
11:01:36.511 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 5
11:01:36.514 [pool-1-thread-9] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 5
11:01:37.512 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 6
11:01:37.515 [pool-1-thread-10] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 6
11:01:38.513 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 7
11:01:38.516 [pool-1-thread-11] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 7
11:01:39.514 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 8
11:01:39.517 [pool-1-thread-12] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 8
11:01:40.515 [main] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Tx: Hello, 9
11:01:40.518 [pool-1-thread-13] INFO kr.pe.elex.rabbitmq.hello.HelloRabbit - Rx: Hello, 9
		 */
	}
}
