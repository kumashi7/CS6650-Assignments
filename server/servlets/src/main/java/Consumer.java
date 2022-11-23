import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer {

    private final static String QUEUE_NAME = "threadExQ";
    private final static Integer numOfThreads = 16;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.191.240.124");
        factory.setUsername("ybohan");
        factory.setPassword("950215");
        factory.setVirtualHost("/");
        factory.setPort(5672);
        final Connection connection = factory.newConnection();
//        Map<Integer, String> skierId2Message = new ConcurrentHashMap<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Jedis jedis = new Jedis("http://localhost:6379");
                    System.out.println("Connection to server successfully" +
                            "==============================================");
                    //check whether server is running or not
                    System.out.println("Server is running: "+jedis.ping());
                    final Channel channel = connection.createChannel();
                    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                    // max one message per receiver
                    channel.basicQos(1);
                    System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        System.out.println( "Callback thread ID = " + Thread.currentThread().getId()
                                + " Received '" + message + "'");

                        String[] urlParts = message.split("/");
                        String skierId =  urlParts[7];
//                        skierId2Message.put(skierId, message);
                        jedis.lpush(skierId, message);
                    };
                    // process messages
                    channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
                } catch (IOException ex) {
                    Logger.getLogger(Consumer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        // start threads and block to receive messages
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < numOfThreads; i++) {
            threads.add(new Thread(runnable));
        }

        for (int i = 0; i < numOfThreads; i++) {
            threads.get(i).start();
        }
    }
}