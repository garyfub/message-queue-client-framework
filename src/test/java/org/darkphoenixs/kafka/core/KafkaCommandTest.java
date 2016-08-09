package org.darkphoenixs.kafka.core;

import kafka.admin.TopicCommand;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZkUtils;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.security.JaasUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class KafkaCommandTest {

    private int brokerId = 0;
    private String topic = "QUEUE.TEST";
    private String zkConnect;
    private EmbeddedZookeeper zkServer;
    private ZkClient zkClient;
    private KafkaServer kafkaServer;
    private int port;
    private Properties kafkaProps;

    @Before
    public void before() {

        zkServer = new EmbeddedZookeeper();
        zkConnect = String.format("localhost:%d", zkServer.port());
        ZkUtils zkUtils = ZkUtils.apply(zkConnect, 30000, 30000,
                JaasUtils.isZkSecurityEnabled());
        zkClient = zkUtils.zkClient();

        final Option<java.io.File> noFile = scala.Option.apply(null);
        final Option<SecurityProtocol> noInterBrokerSecurityProtocol = scala.Option
                .apply(null);

        port = TestUtils.RandomPort();
        kafkaProps = TestUtils.createBrokerConfig(brokerId, zkConnect, false,
                false, port, noInterBrokerSecurityProtocol, noFile, true,
                false, TestUtils.RandomPort(), false, TestUtils.RandomPort(),
                false, TestUtils.RandomPort());

        kafkaProps.setProperty("auto.create.topics.enable", "true");
        kafkaProps.setProperty("num.partitions", "1");
        // We *must* override this to use the port we allocated (Kafka currently
        // allocates one port
        // that it always uses for ZK
        kafkaProps.setProperty("zookeeper.connect", this.zkConnect);

        KafkaConfig config = new KafkaConfig(kafkaProps);
        Time mock = new MockTime();
        kafkaServer = TestUtils.createServer(config, mock);

        // create topic
        TopicCommand.TopicCommandOptions options = new TopicCommand.TopicCommandOptions(
                new String[]{"--create", "--topic", topic,
                        "--replication-factor", "1", "--partitions", "1"});

        TopicCommand.createTopic(zkUtils, options);

        List<KafkaServer> servers = new ArrayList<KafkaServer>();
        servers.add(kafkaServer);
        TestUtils.waitUntilMetadataIsPropagated(
                scala.collection.JavaConversions.asScalaBuffer(servers), topic,
                0, 5000);
    }

    @After
    public void after() {
        kafkaServer.shutdown();
        zkClient.close();
        zkServer.shutdown();
    }

    @Test
    public void test() throws Exception {

        Assert.assertNotNull(new KafkaCommand());

        KafkaCommand.createTopic(zkConnect, "COMMAND.TEST", 1, 1);

        KafkaCommand.listTopics(zkConnect);

        KafkaCommand.describeTopic(zkConnect, "COMMAND.TEST");

        KafkaCommand.alterTopic(zkConnect, "COMMAND.TEST", 2);

        KafkaCommand.alterTopic(zkConnect, "COMMAND.TEST", "flush.messages=1",
                "max.message.bytes");

        KafkaCommand.alterTopic(zkConnect, "COMMAND.TEST", 3, "flush.messages",
                "max.message.bytes=64000");

        KafkaCommand.deleteTopic(zkConnect, "COMMAND.TEST");

//		KafkaCommand.topicCommand("--list", "--zookeeper", zkConnect);
    }

}
