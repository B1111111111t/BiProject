package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

/**
 * fanout 交换机
 * 1.创建连接工厂（new ConnectionFactory()）,设置连接地址和端口
 * 2.创建连接（factory.newConnection)
 * 3.创建channel频道（connection.createChannel)
 * 4.创建交换机（channel.exchangeDeclare(交换机名称,交换机类型)）
 * 5.发送消息给交换机（ channel.basicPublish(交换机名称，routingKey,props,message.getBytes()) ）
 */
public class FanoutProducer {

  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    factory.setPort(11001);
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        // 创建交换机
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String message = scanner.nextLine();
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
  }
}