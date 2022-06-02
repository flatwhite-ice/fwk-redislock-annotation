package com.example.threadlock.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Aspect
@Slf4j
@Component
public final class RedisLockAspect {

    private final RedissonClient redissonClient;

    public RedisLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Pointcut("@annotation(RedisLock)")
    public void redisLock(){}

    @Around("redisLock()")
    public Object getLock(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Object[] joinPointArgs    = joinPoint.getArgs();
        Method method             = signature.getMethod();
        Parameter[] parameters    = method.getParameters();
        RedisLock redisLock       = method.getAnnotation(RedisLock.class);

        String lockKey   = this.lockKey(redisLock, parameters, joinPointArgs);
        Long timeout     = redisLock.timeout();
        Type actionType  = redisLock.action();
        Long waitTime    = redisLock.waittime();
        Long leaseTime   = redisLock.leasetime();


        //TODO: Timeout Action 구현, 즉시 exception or waiting, default timeout시 action

        if(log.isDebugEnabled()){
            log.debug("redisLock key is : {}", lockKey);
        }

        RLock lock       = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        Object finished   = null;

        if(log.isDebugEnabled()){
            log.debug("[thread : {}] rlock[{}] started at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
        }

//        if(timeout == RedisLock.DEFAULT_TIMEOUT && acquisition == RedisLock.DEFAULT_ACQUISITION){
//            finished = this.proceedDefault(lockKey, lock, joinPoint);
//        }else if(timeout >= RedisLock.DEFAULT_TIMEOUT && acquisition == RedisLock.DEFAULT_ACQUISITION){
//            finished = this.proceedTimeout(timeout, lockKey, lock, joinPoint);
//        }else if(timeout >= RedisLock.DEFAULT_TIMEOUT && acquisition >= RedisLock.DEFAULT_ACQUISITION){
//            finished = this.proceedDefault(lockKey, lock, joinPoint);
//        }else if(timeout == RedisLock.DEFAULT_TIMEOUT && acquisition >= RedisLock.DEFAULT_ACQUISITION){
//            finished = this.proceedAttemptTillUnlock(acquisition, lockKey, lock, joinPoint);
//        }

        finished = this.proceedAttemptTillUnlock(waitTime, leaseTime, lockKey, lock, joinPoint);

        return finished;

    }


    /**
     * Request시 Lock을 획득하고, 동일한 Key로 Lock을 획득 시도하였으나, 획득에 실패한 경우 exception을 발생시킨다.
     * completed
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     */
    private Object proceedDefault(String lockKey, RLock lock, ProceedingJoinPoint joinPoint){

        log.info(">>proceed default");

        boolean isLocked = lock.tryLock();
        Object finished   = null;
        try{

            if(!isLocked){
                String message = "(" + lockKey  + ") is locked by another request[" + Thread.currentThread().getId() +"]";
                throw new RedisLockException(message);
            }

            finished = joinPoint.proceed();

        } catch (RedisLockException rle) {
            rle.printStackTrace();
            throw rle;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally{
            if(isLocked){
                lock.unlock();
            }

            if(log.isDebugEnabled()){
                log.debug("[thread : {}] rlock[{}] ended at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
            }

        }

        return finished;

    }


    /**
     * RedisLock에 Timeout을 걸어준다.
     * Timeout내에 request가 종료되지 못하면 Lock을 획득하지 못하고 실패처리된다.(로 수정하자 ..보완필요)
     * @param timeout
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     * @throws InterruptedException
     */
    private Object proceedTimeout(Long timeout, String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws InterruptedException {

        log.info(">>proceed timeout");
        boolean isLocked = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        log.debug("proceedTimeout isLocked : {}", isLocked);
        Object finished = null;
        try{
            log.debug("[thread : {}] rlock[{}] request started at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
            finished = joinPoint.proceed();
            log.debug("[thread : {}] rlock[{}] request finished at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
        }catch(Throwable e){
            e.printStackTrace();
        }finally{
            if(lock.isLocked()){
                lock.unlock();
            }

            if(log.isDebugEnabled()){
                log.debug("[thread : {}] rlock[{}] ended at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
            }
        }

        return finished;
    }


    /**
     * unlock될때까지 aquition기간동안은 lock을 획득하기위해 대기한다.
     * TODO:  wait_time과 lease_time을 어노테이션으로 넣자, 이때 action type을 AS_FAR_AS_POSSIBLE로 처리한다.
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     */
    private Object proceedAttemptTillUnlock(Long waitTime, Long leaseTime, String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws InterruptedException {

        log.info(">>proceed attempt till unlock, waitTime : {}, leaseTime : {}", waitTime, leaseTime);

        boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        Object finished = null;
        try{
            log.debug("lock acquired at : {}", LocalDateTime.now());
            finished = joinPoint.proceed();

        } catch (RedisLockException rle) {
            rle.printStackTrace();
            throw rle;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally{
            if(isLocked){
                lock.unlock();
            }

            if(log.isDebugEnabled()){
                log.debug("[thread : {}] rlock[{}] ended at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
            }

        }

        return finished;
    }


    /**
     * 어노테이션으로부터 Lock Key를 획득한다.
     * @param redisLock
     * @param parameters
     * @param joinPointArgs
     * @return
     */
    private String lockKey(RedisLock redisLock, Parameter[] parameters, Object[] joinPointArgs){

        String lockKey = null;
        Optional<String> first;

        log.debug("redisLock type is {}", redisLock.type().name());
        switch(redisLock.type()){

            case KEY:
                //Domain Layer
                first = Arrays.stream(parameters)
                        .filter(p -> p.getName().equals(redisLock.value()))
                        .map  (p -> {
                            String value = "";
                            for(int i = 0; i < joinPointArgs.length; i++){
                                if(parameters[i].getName().equals((p.getName()))){
                                    value = joinPointArgs[i].toString();
                                    break;
                                }
                            }
                            return value;
                        })
                        .findFirst();
                lockKey = first.get();
                break;

            case PATH_VARIABLE:
                //Controller Layer
                first = Arrays.stream(parameters)
                        .filter(p -> p.getDeclaredAnnotation(PathVariable.class) != null)
                        .filter(p -> p.getName().equals(redisLock.value()))
                        .map  (p -> {
                            String value = "";
                            //if(log.isDebugEnabled()){
                            //    log.debug("p.getName : {}", p.getName());
                            //}
                            for(int i = 0; i < joinPointArgs.length; i++){
                                //if(log.isDebugEnabled()){
                                //    log.debug("joinPoint.getArgs[{}] : {}", i, joinPoint.getArgs()[i]);
                                //    log.debug("method.getParameters()[{}].getName() : {}", i,  method.getParameters()[i].getName());
                                //}
                                if(parameters[i].getName().equals(p.getName())){
                                    value = joinPointArgs[i].toString();
                                    break;
                                }
                            }
                            return value;
                        })
                        .findFirst();

                lockKey = first.get();
                break;

        }

        return lockKey;
    }

}
