package com.flatwhite.concurrent.lock.infrastructure.redis;

public class RedisLockException extends RuntimeException{
    public RedisLockException(String redisLockFailed) {
        super(redisLockFailed);
    }
}
