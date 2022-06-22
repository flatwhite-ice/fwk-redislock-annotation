package com.example.threadlock.infrastructure.redis;

public enum Lock{
    //try to acquire lock, wait until wait_time, and unlock after lease_time
    TRYLOCK_WAITTIME_LEASETIME,

    //if fail unlock, occure exception
    DEFAULT,

    //lock interruptibly
    INTERRUPTIBLY,

    //get trylock with timeout
    TRYLOCK

}