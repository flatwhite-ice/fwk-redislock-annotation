package com.example.threadlock.infrastructure.redis;

public enum Type {
    //try to acquire lock, wait until wait_time, and unlock after lease_time
    AS_FAR_AS_POSSIBLE,
    //if fail unlock, occure exception
    TRY_FIRST
}