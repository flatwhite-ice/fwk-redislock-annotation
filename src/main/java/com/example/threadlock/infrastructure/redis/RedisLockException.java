package com.example.threadlock.infrastructure.redis;

public class RedisLockException extends RuntimeException{
    public RedisLockException(String redisLockFailed) {
        super(redisLockFailed);
    }
}
