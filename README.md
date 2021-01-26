# RabbitMQ Client Example

## Docker에 설치

```bash
#!/bin/bash
docker run -d --restart=no --name=rabbitmq \
--hostname rabbit-mq \
-p 5672:5672 \
-p 15672:15672 \
-v /docker/rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf \
-v docker/rabbitmq/conf.d:/etc/rabbitmq/conf.d \
-e TZ=Asia/Seoul -e LANG=ko_KR.UTF-8 \
-e RABBITMQ_DEFAULT_USER=elex -e RABBITMQ_DEFAULT_PASS=test \
rabbitmq:3.8.11-management
```

## Gradle 디펜던시 추가
```kotlin
// https://mvnrepository.com/artifact/com.rabbitmq/amqp-client
implementation("com.rabbitmq:amqp-client:5.10.0")
```

## 다이렉트 익스체인지
* 익스체인지는 브로커가 메시지를 받는 곳이다. 즉, 메시지를 보낼 때는 익스체인지로 보내야 한다.
* 큐는 브로커가 메시지를 보내는 곳이다. 즉, 메시지를 받을 때는 큐로부터 받는다.
* 익스체인지와 큐는 라우팅-키로 서로 바인딩된다. 여러 개의 라우팅-키로 여러 번 바인딩 할 수도 있다.
* 익스체인지에 메시지가 도착하면 메시지의 라우팅-키와 일치하는 큐로 메시지를 보낸다.

## 팬아웃 익스체인지
* 라우팅-키 규칙이 무시된다.

## 토픽 익스체인지
* 라우팅-키를 패턴으로 사용한다.

## TLS
```bash
!/bin/bash
docker run -d --restart=always --name=rabbitmq \
--hostname rabbitmq \
-p 5671:5671 \
-p 5672:5672 \
-p 15671:15671 \
-p 15672:15672 \
-v /media/rabbitmq/certs:/etc/certs \
-e TZ=Asia/Seoul -e LANG=ko_KR.UTF-8 \
-e RABBITMQ_DEFAULT_USER=elex \
-e RABBITMQ_DEFAULT_PASS=test \
-e RABBITMQ_SSL_CACERTFILE=/etc/certs/ca.pem \
-e RABBITMQ_SSL_CERTFILE=/etc/certs/server.pem \
-e RABBITMQ_SSL_KEYFILE=/etc/certs/server.key.pem \
-e RABBITMQ_SSL_FAIL_IF_NO_PEER_CERT=false \
-e RABBITMQ_SSL_VERIFY=verify_none \
rabbitmq:3.8.11-management
```

-----
Copyright (c) 2021 Elex.

All Rights Reserved.

https://www.elex-project.com/
