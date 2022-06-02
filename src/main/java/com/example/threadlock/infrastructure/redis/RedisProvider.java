package com.example.threadlock.infrastructure.redis;


import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisProvider {

    private final RedissonClient redissonClient;

    public RedisProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void save(String key, String value) {
        getBucket(key).set(value);
    }

    public String get(String key) {
        return getBucket(key).get().toString();
    }

    private RBucket getBucket(String key) {
        return redissonClient.getBucket(key);
    }
}
