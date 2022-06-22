package com.flatwhite.concurrent.lock.account;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AccountDto{

    private String account;

    private Long amount;

    public AccountDto(String account, Long amount){
        this.account = account;
        this.amount  = amount;
    }

}
