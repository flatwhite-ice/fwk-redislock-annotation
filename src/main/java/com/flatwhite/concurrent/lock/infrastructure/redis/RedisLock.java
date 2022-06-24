package com.flatwhite.concurrent.lock.infrastructure.redis;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    Control keytype() default Control.KEY;

    String key() default "";

    long timeout() default DEFAULT_TIMEOUT;

    Lock locktype() default Lock.DEFAULT;

    long waittime() default 0;

    long leasetime() default DEFAULT_LEASETIME;

    public static long DEFAULT_LEASETIME = -1L;

    public static long DEFAULT_TIMEOUT   = -1L;

}


