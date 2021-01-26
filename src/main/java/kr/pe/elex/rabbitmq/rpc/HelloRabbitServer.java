/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.rpc;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-six-java.html"
 */
@Slf4j
public class HelloRabbitServer {
	static final String EXCHANGE = "elex.rpc.exchange";
	static final String QUEUE = "elex.rpc.queue";
	static final String ROUTING_KEY = "elex-routing-key";

	private Connection connection;
	private Channel channel;

	HelloRabbitServer() throws IOException, TimeoutException {
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
				log.info("Server Rx: {}", new String(body, StandardCharsets.UTF_8));

				String queue = properties.getReplyTo();
				String messageId = properties.getCorrelationId();
				String message = new String(body, StandardCharsets.UTF_8).toUpperCase();

				channel.basicPublish(EXCHANGE, queue,
						new AMQP.BasicProperties.Builder()
								.correlationId(messageId)
								.build(),
						message.getBytes(StandardCharsets.UTF_8));
				log.info("Server Tx: {}", message);
			}
		});
	}


	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
	}

	interface Handler {
		void onResponse(String message);
	}

}
