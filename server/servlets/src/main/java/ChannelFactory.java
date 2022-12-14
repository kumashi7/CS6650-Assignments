import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ChannelFactory extends BasePooledObjectFactory<Channel> {
    @Override
    public Channel create() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("35.167.242.36");
        factory.setUsername("ybohan");
        factory.setPassword("950215");
        factory.setVirtualHost("/");
        factory.setPort(5672);
        final Connection conn = factory.newConnection();
        return conn.createChannel();
    }

    /**
     * Use the default PooledObject implementation.
     */
    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }
}
