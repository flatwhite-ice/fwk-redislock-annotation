package com.example.threadlock.infrastructure.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix="redis")
public class RedissonConfiguration {

    private List<String> nodes;

    public List<String> getNodes(){
        return nodes;
    }

    public void setNodes(List<String> nodes){
        this.nodes = nodes;
    }

    public String mode;

    public String getMode(){
        return this.mode;
    }

    public void setMode(String mode){
        this.mode = mode;
    }


    @Bean
    public RedissonClient createRedisClient(){
        return buildRedissonClient();
    }

    private RedissonClient buildRedissonClient() {

        Config config = new Config().setCodec(StringCodec.INSTANCE);
        if ("single".equals(this.getMode())) {
            SingleServerConfig ssc = config.useSingleServer();
            ssc.setAddress(this.getNodes().get(0));
            return Redisson.create(config);
        } else if ("cluster".equals(this.getMode())) {
            ClusterServersConfig csc = config.useClusterServers();
            this.getNodes().forEach(node -> {
                csc.addNodeAddress(node);
            });
            return Redisson.create(config);
        }
        throw new IllegalArgumentException("Invalid mode: " + this.getMode());
    }


}
