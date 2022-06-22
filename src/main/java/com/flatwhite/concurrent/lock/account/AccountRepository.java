package com.flatwhite.concurrent.lock.account;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AccountRepository implements InitializingBean {

    private Map<String, Account> accounts;

    @Override
    public void afterPropertiesSet() throws Exception {
        accounts = new HashMap<>();

        Account account1 = Account.builder()
                .account("0001")
                .amount(1000L)
                .build();

        Account account2 = Account.builder()
                .account("0002")
                .amount(2000L)
                .build();

        accounts.put("0001", account1);
        accounts.put("0002", account2);
    }

    public Account getAccount(String account){
        return accounts.get(account);
    }



}
