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
 * 메시지 소비자는 메시지 처리 후 ack를 보내야 한다.
 * QoS를 지정해서 소비자에 전달할 메시지의 최대 개수를 지정할 수 있다.
 *
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-two-java.html"
 */
@Slf4j
public class HelloRabbit2 {
	private static final String EXCHANGE = "elex.direct.exchange";
	private static final String QUEUE = "elex.queue";
	private static final String ROUTING_KEY = "elex-routing-key";

	private Connection connection;
	private Channel channel;

	HelloRabbit2() throws IOException, TimeoutException {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("localhost");
		connectionFactory.setPort(5672);
		connectionFactory.setUsername("elex");
		connectionFactory.setPassword("test");
		connectionFactory.setVirtualHost("/");
		connectionFactory.setAutomaticRecoveryEnabled(true);

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

		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(QUEUE, false, ROUTING_KEY, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				log.info("Rx: {}", new String(body, StandardCharsets.UTF_8));

				// 만일, 수신 확인을 하지 않으면, 브로커는 다시 전송을 시도할겁니다.
				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		});

	}

	public void publish(String message) throws IOException, TimeoutException, InterruptedException {
		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, ROUTING_KEY,
				// 브로커가 메시지를 디스크에 저장해둠으로써, 오류 등으로 브로커가 종료되었을 경우에
				// 미처 전달되지 못한 메시지가 사라지는 것을 예방합니다.
				MessageProperties.PERSISTENT_TEXT_PLAIN,
				message.getBytes(StandardCharsets.UTF_8));

		// 메시지가 전달되지 않으면 예외가 발생합니다.
		channel.waitForConfirmsOrDie(1000);

		log.info("Tx: {}", message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	public static void main(String... args) throws IOException, TimeoutException {
		HelloRabbit2 helloRabbit = new HelloRabbit2();
		for (int i = 0; i < 10; i++) {
			try {
				helloRabbit.publish("Hello, " + i);
			} catch (TimeoutException | InterruptedException e) {
				log.error("Publish fail..", e);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("Interrupted..", e);
			}
		}
		helloRabbit.close();
	}
}
