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

import static kr.pe.elex.rabbitmq.rpc.HelloRabbitServer.EXCHANGE;
import static kr.pe.elex.rabbitmq.rpc.HelloRabbitServer.ROUTING_KEY;

/**
 * {@link AMQP.BasicProperties.Builder#replyTo(String)}를 사용해서 응답받을 라우팅-키를 전달할 수 있다.
 *
 * @author Elex
 * @see "https://www.rabbitmq.com/tutorials/tutorial-six-java.html"
 */
@Slf4j
public class HelloRabbit {
	private static final String CLIENT_ROUTING_KEY = "client-routing-key";
	private Connection connection;
	private Channel channel;
	private String queue;

	private Map<UUID, Handler> handlers = new HashMap<>();

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
		queue = channel.queueDeclare().getQueue();
		// 익스체인지와 큐를 묶습니다.
		channel.queueBind(queue, EXCHANGE, CLIENT_ROUTING_KEY);

		// 큐로부터 메시지를 받습니다.
		channel.basicConsume(queue, false, queue, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				// 메시지 아이디로 핸들러를 가져옵니다.
				Handler handler = handlers.remove(UUID.fromString(properties.getCorrelationId()));
				if (null != handler) {
					handler.onResponse(new String(body, StandardCharsets.UTF_8));
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		});
	}

	public void publish(String message, Handler handler) throws IOException {
		UUID uuid = UUID.randomUUID(); // 메시지 아이디로 사용됩니다.
		handlers.put(uuid, handler); // 응답 처리를 위해 핸들러를 저장해둡니다.

		// 익스체인지에 메시지를 보냅니다.
		channel.basicPublish(EXCHANGE, ROUTING_KEY,
				new AMQP.BasicProperties.Builder()
						.replyTo(CLIENT_ROUTING_KEY)
						.contentEncoding(StandardCharsets.UTF_8.name())
						.contentType("text/plain")
						.correlationId(uuid.toString())
						.deliveryMode(MessageProperties.PERSISTENT_BASIC.getDeliveryMode())
						.build(),
				message.getBytes(StandardCharsets.UTF_8));
		log.info("Tx: {}", message);
	}

	public void close() throws IOException, TimeoutException {
		channel.close();
		connection.close();
		handlers.clear();
	}

	interface Handler {
		void onResponse(String message);
	}

	public static void main(String... args) throws IOException, TimeoutException {
		HelloRabbitServer server = new HelloRabbitServer();
		HelloRabbit client = new HelloRabbit();
		client.publish("Hello, there", new Handler() {
			@Override
			public void onResponse(String message) {
				log.info("Rx: {}", message);
			}
		});
		client.publish("Lorem ipsum ...", new Handler() {
			@Override
			public void onResponse(String message) {
				log.info("Rx: {}", message);
			}
		});

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.error("Interrupted..", e);
		}
		client.close();
		server.close();
	}

}
