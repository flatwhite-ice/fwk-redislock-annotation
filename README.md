# fwk-redislock-annotation


@RedisLock annotation usage updated
===================================

updated
-------
```java
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.DEFAULT)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK, timeout = 30000L)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK_WAITTIME_LEASETIME, waittime = 30000L, leasetime = 30000L)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY) //leasetime default = -1L
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY, leasetime = 30000L)
```


redislock(key) controlled by path-variable : controller layer
-------------------------------------------------------------

```java

@Slf4j
@RestController
@RequestMapping("/account")
public class AccountWithdrawLockController{

    // ...

    @RedisLock(keytype = Control.PATH_VARIABLE, key = "account",  locktype = Lock.TRYLOCK_WAITTIME_LEASETIME, waittime = 30000L, leasetime = 30000L)
    @GetMapping("/locked/{account}/withdraw/{amount}")
    public AccountResponseDto withdraw(@PathVariable String account, @PathVariable String amount) {

        Account accountEntity = accountRepository.getAccount(account);
        AccountResponseDto response = new AccountResponseDto();
        accountEntity.setAmount(accountEntity.getAmount() - Long.valueOf(amount));

        // ...

        AccountDto accountDto = accountEntity.convertToDto(accountEntity);
        response.setData(accountDto);
        response.setResult("success");
        response.setMsg("withdraw success, amount " + accountEntity.getAmount());

        return response;
    }

}
```

redislock(key) controlled by method parameter : component layer
---------------------------------------------------------------
```java
@Service
public class AccountDepositService {

    private final AccountRepository accountRepository;

    public AccountDepositService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY)
    public AccountResponseDto deposit(String account, Long amount){

        AccountResponseDto response = new AccountResponseDto();

        try{
            // Thread.sleep(7000L);
            Account accountEnitity = accountRepository.getAccount(account);
            accountEnitity.setAmount(accountEnitity.getAmount() + amount);

            AccountDto accountDto = accountEnitity.convertToDto(accountEnitity);
            response.setData(accountDto);
            response.setResult("success");
            response.setMsg("deposit success, amount " + accountEnitity.getAmount());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return response;
    }

}
````

annotation manual
-----------------

|  item | usage | @see |
|-------|------|------|
|annotation name|@RedisLock| https://github.com/flatwhite-ice/spring-redislock-annotation/blob/main/src/main/java/com/flatwhite/concurrent/lock/infrastructure/redis/RedisLock.java |
|keytype|Control.KEY, Control.PATH_VARIABLE, Control.MANUAL| https://github.com/flatwhite-ice/spring-redislock-annotation/blob/main/src/main/java/com/flatwhite/concurrent/lock/infrastructure/redis/Control.java |
|key|value from pathvariable or method parameter or manually||
|locktype|Lock.DEFAULT, Lock.TRYLOCK, Lock.TRYLOCK_WAITTIME_LEASETIME, Lock.INTERRUPTIBLY|https://github.com/flatwhite-ice/spring-redislock-annotation/blob/main/src/main/java/com/flatwhite/concurrent/lock/infrastructure/redis/Lock.java|
|waittime|long, waiting to acquire||
|leasetime|long, unlock after lease_time||

* aspect : https://github.com/flatwhite-ice/spring-redislock-annotation/blob/main/src/main/java/com/flatwhite/concurrent/lock/infrastructure/redis/RedisLockByAspect.java
