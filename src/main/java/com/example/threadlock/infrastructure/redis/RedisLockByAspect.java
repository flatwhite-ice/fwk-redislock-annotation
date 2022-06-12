package com.example.threadlock.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.example.threadlock.infrastructure.redis.Control.KEY;
import static com.example.threadlock.infrastructure.redis.Control.PATH_VARIABLE;

@Aspect
@Slf4j
@Component
public final class RedisLockByAspect {

    private final RedissonClient redissonClient;

    public RedisLockByAspect(RedissonClient redissonClient) {
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
        Lock actionType  = redisLock.locktype();
        Long waitTime    = redisLock.waittime();
        Long leaseTime   = redisLock.leasetime();


        if(log.isDebugEnabled()){
            log.debug("redisLock key is : {}", lockKey);
        }

        RLock lock       = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        Object finished   = null;

        if(log.isDebugEnabled()){
            log.debug("[thread : {}] rlock[{}] started at : {}", Thread.currentThread().getId(), lockKey, LocalDateTime.now());
        }


        switch(redisLock.locktype()) {

            case DEFAULT:
                log.debug("lock type is default");
                finished = this.proceedDefault(lockKey, lock, joinPoint);
                break;

            case INTERRUPTIBLY:
                log.debug("lock type is interruptibly");
                finished = this.proceedLockInterruptibly(leaseTime, lockKey, lock, joinPoint);
                break;

            case TRYLOCK:
                log.debug("lock type is tryLock");
                finished = this.proceedTryLockTimeout(timeout, lockKey, lock, joinPoint);
                break;

            case TRYLOCK_WAITTIME_LEASETIME:
                log.debug("lock type is trylock waittime and lease time");
                if(waitTime <= 0 || leaseTime <= 0){
                    throw new RedisLockException("waittime or leasetime not found exception.");
                }

                finished = this.proceedTryLockWaittimeLeaseTime(waitTime, leaseTime, lockKey, lock, joinPoint);
                break;

            default:
                finished = joinPoint.proceed();
                break;
        }

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
     * Timeout내에 request가 종료되지 못하면 Lock을 획득하지 못하고 실패처리된다.
     * @param timeout
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     * @throws InterruptedException
     */
    private Object proceedTryLockTimeout(Long timeout, String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws InterruptedException {

        log.info(">>proceed trylock timeout");
        boolean isLocked = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if(!isLocked){
            String message = "(" + lockKey  + ") is locked by another request[" + Thread.currentThread().getId() +"]";
            throw new RedisLockException(message);
        }
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
     * locktype = Lock.TRYLOCK_WAITTIME_LEASETIME
     * unlock될때까지 aquition기간동안은 lock을 획득하기위해 대기한다.
     *
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     */
    private Object proceedTryLockWaittimeLeaseTime(Long waitTime, Long leaseTime, String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws InterruptedException {

        log.info(">>proceed attempt till unlock, waitTime : {}, leaseTime : {}", waitTime, leaseTime);

        boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        if(!isLocked){
            String message = "(" + lockKey  + ") is locked by another request[" + Thread.currentThread().getId() +"]";
            throw new RedisLockException(message);
        }

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
     * locktype = Lock.INTERRUPTIBLY
     * lock을 획득하기위해 대기한다.
     *
     * @param leaseTime
     * @param lockKey
     * @return
     * @throws InterruptedException
     */
    private Object proceedLockInterruptibly(Long leaseTime, String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws InterruptedException{

        log.info(">>proceed lockInterruptibly, leaseTime : {}",  leaseTime);

        lock.lockInterruptibly(leaseTime, TimeUnit.MILLISECONDS);
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
     * locktype = Lock.INTERRUPTIBLY
     * leaseTime이 주어지지않을경우 default leaseTime으로 수행한다.
     * default leaseTime : -1L
     * @param lockKey
     * @param lock
     * @param joinPoint
     * @return
     * @throws InterruptedException
     */
    private Object proceedLockInterruptibly(String lockKey, RLock lock, ProceedingJoinPoint joinPoint) throws  InterruptedException{
        return this.proceedLockInterruptibly(-1L, lockKey, lock, joinPoint);
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

        log.debug("redisLock type is {}", redisLock.keytype().name());
        switch(redisLock.keytype()){

            case KEY:
                //Domain Layer (from Parameter)
                first = Arrays.stream(parameters)
                        .filter(p -> p.getName().equals(redisLock.key()))
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
                        .filter(p -> p.getName().equals(redisLock.key()))
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
