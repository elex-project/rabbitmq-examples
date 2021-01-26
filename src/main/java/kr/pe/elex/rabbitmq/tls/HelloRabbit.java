/*
 * Copyright (c) 2021. Elex. All Rights Reserved.
 * https://www.elex-project.com/
 */

package kr.pe.elex.rabbitmq.tls;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeoutException;

/**
 * TLS 샘플
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

	private SSLContext sslContext()
			throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, IOException, CertificateException {

		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		// 클라이언트 키와 인증서가 들어있습니다. OpenSSL로 만들고 PKCS12 키 저장소에 넣었습니다.
		keyStore.load(getClass().getResourceAsStream("/clientstore.p12"), "test".toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore, "test1".toCharArray());

		KeyStore trustKeyStore = KeyStore.getInstance("PKCS12");
		// CA 인증서가 들어있습니다.
		trustKeyStore.load(getClass().getResourceAsStream("/truststore.p12"), "test".toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustKeyStore);

		SSLContext context = SSLContext.getInstance("TLSv1.3");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return context;
	}

	HelloRabbit() throws IOException, TimeoutException, KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, CertificateException {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("localhost");
		connectionFactory.setPort(5671); // 포트 번호가 다릅니다.
		connectionFactory.setUsername("elex");
		connectionFactory.setPassword("test");
		connectionFactory.setVirtualHost("/");
		//connectionFactory.useSslProtocol(); // 테스트 환경에서 사용됩니다.
		connectionFactory.useSslProtocol(sslContext());
		//connectionFactory.enableHostnameVerification(); // 인증서 내용과 호스트네임을 검증합니다.

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

	public static void main(String... args) throws IOException, TimeoutException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
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

	}
}
