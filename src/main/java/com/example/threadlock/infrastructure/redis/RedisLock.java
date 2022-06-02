package com.example.threadlock.infrastructure.redis;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    Lock type() default Lock.KEY;

    String value() default "";

    long timeout() default DEFAULT_TIMEOUT;

    Type action() default Type.TRY_FIRST;

    long waittime() default 0;

    long leasetime() default 0;

    public static long DEFAULT_TIMEOUT    = -1L;
    public static long DEFAULT_ACQUISITION = -1L;

}


