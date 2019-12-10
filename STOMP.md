# STOMP

> org.springframework.messaging

## 主要概念

* websocket

  Websocket是html5提出的一个协议规范，是为解决客户端与服务端实时通信。本质上是一个基于tcp，先通过HTTP/HTTPS协议发起一条特殊的http请求进行握手后创建一个用于交换数据的TCP连接。只需要要做一个握手的动作，在建立连接之后，双方可以在任意时刻，相互推送信息。同时，服务器与客户端之间交换的头信息很小。

* stomp

  STOMP是基于帧的协议，客户端和服务器使用STOMP帧流通讯

  一个STOMP客户端是一个可以以两种模式运行的用户代理，可能是同时运行两种模式。

  - 作为生产者，通过`SEND`框架将消息发送给服务器的某个服务
  - 作为消费者，通过`SUBSCRIBE`制定一个目标服务，通过`MESSAGE`框架，从服务器接收消息。
* 比较
  其实stomp是基于websocket的一种协议，制定类似常规http请求的参数规范。

## 核心代码
```java 
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
  	//客户端订阅消息的请求前缀，topic一般用于广播，queue一般用于点对点推送
    config.enableSimpleBroker("/topic", "/queue");
    //客户端发送消息的请求前缀
    config.setApplicationDestinationPrefixes("/app");
    //服务端通知客户端的前缀，默认是/user
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
  //注册一个stomp协议的endpoint 支持sockjs 支持跨域
    registry.addEndpoint("/gs-guide-websocket").setAllowedOrigins("*").withSockJS();
  }

}
```
## websocket是否握手成功
请求中是否包含参数
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: xqBt3ImNzJbYqRINxEFlkg==
## 握手拦截器
implements HandshakeInterceptor 
override `beforeHandshake` `afterHandshake`


## 消息体
STOMP在WebSocket之上提供了一个基于帧的线路格式

* 订阅

``` json
>>>MESSAGE
SUBSCRIBE-id:sub-0
destination:/topic/kafka
```
*  主动拉取
```json
>>> SEND
transaction:tx-0
destination:/app/marco
content-length:20
{"message":"Marco!"}
```

## 前端

sockjs（）地址为http或者https，而不是ws

sockjs会根据不同的浏览器对websocket的支持情况，选择轮询还是websocket，是一种兼容的措施

## 坑

前端需要注意如果使用sockjs，URL为http:.....

后端注意@sendTo和@MessageMapping

MessageMapping和setApplicationDestinationPrefixes路径不要重叠。

例如：

stompClient.send("/app/hello", {}, null));

@MessageMapping("/hello")