package com.flatwhite.concurrent.lock.account;


import com.flatwhite.concurrent.lock.infrastructure.redis.Control;
import com.flatwhite.concurrent.lock.infrastructure.redis.Lock;
import com.flatwhite.concurrent.lock.infrastructure.redis.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountDepositService {

    private final AccountRepository accountRepository;

    public AccountDepositService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    //@RedisLock(keytype = Control.MANUAL, key = "0001", locktype = Lock.TRYLOCK, waittime = 30000L, leasetime = 30000L)
    //@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK_WAITTIME_LEASETIME, waittime = 30000L, leasetime = 30000L)
    //@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK, timeout = 5000L)
    @RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY)
    public AccountResponseDto deposit(String account, Long amount){

        AccountResponseDto response = new AccountResponseDto();

        try{
            Thread.sleep(7000L);
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
