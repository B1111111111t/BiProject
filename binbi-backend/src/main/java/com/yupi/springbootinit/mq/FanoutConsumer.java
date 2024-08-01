package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * 1.创建连接
 * 2.声明交换机
 * 3.创建队列，并且给队列绑定交换机
 * 4.定义处理消息的方法：
 */
public class FanoutConsumer {

  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setPort(11001);
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
//    Channel channel2 = connection.createChannel();
    // 声明交换机
    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    // 创建队列，随机分配一个队列名称
    String queueName = "xiaowang_queue";
    channel.queueDeclare(queueName, true, false, false, null);
    channel.queueBind(queueName, EXCHANGE_NAME, "");

    String queueName2 = "xiaoli_queue";
    channel.queueDeclare(queueName2, true, false, false, null);
    channel.queueBind(queueName2, EXCHANGE_NAME, "");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [小王] Received '" + message + "'");
    };

    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [小李] Received '" + message + "'");
    };
    channel.basicConsume(queueName, true, deliverCallback1, consumerTag -> { });
    channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });
  }
}