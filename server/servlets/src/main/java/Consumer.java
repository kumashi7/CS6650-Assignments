import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Consumer {

    private final static String QUEUE_NAME = "threadExQ";
    private final static Integer numOfThreads = 16;
    private static Gson gson = new Gson();

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("35.167.242.36");
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
//                        String skierId =  urlParts[7];
//                        skierId2Message.put(skierId, message);
//                        String message = new String(delivery.getBody(), "UTF-8");
//                        JsonObject json = gson.fromJson(message, JsonObject.class);
                        String skierId = urlParts[7];
                        String resortID = urlParts[1];
                        String seasonId = urlParts[3];
                        String dayId = urlParts[5];
                        int vertical = Integer.parseInt(dayId) * 8;
                        // For skier N, how many days have they skied this season?
                        // update total days
                        // hash => skierId => seasonId => days
                        Map<String, String> skierFields = jedis.exists(skierId)? jedis.hgetAll(skierId): new HashMap<>();
                        // For skier N, what are the vertical totals for each ski season?
                        // update total verticals
                        // hash => skierId => dayId => total verticals
                        if(skierFields.containsKey(seasonId)) {
                            int preVertical = Integer.parseInt(jedis.hget(skierId, seasonId));
                            jedis.hset(skierId, seasonId, String.valueOf(preVertical + vertical));
                        } else {
                            jedis.hset(skierId, seasonId, String.valueOf(vertical));
                        }
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