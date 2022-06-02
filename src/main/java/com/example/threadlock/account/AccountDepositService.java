package com.example.threadlock.account;

import com.example.threadlock.infrastructure.redis.Lock;
import com.example.threadlock.infrastructure.redis.RedisLock;
import com.example.threadlock.infrastructure.redis.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountDepositService {

    private final AccountRepository accountRepository;

    public AccountDepositService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;
    }

    @RedisLock(type = Lock.KEY, value = "account", action = Type.AS_FAR_AS_POSSIBLE, waittime = 30000L, leasetime = 30000L)
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
