package com.example.threadlock.account;

import lombok.*;
import org.springframework.util.Assert;


@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    private String account;

    private Long amount;


    @Builder
    public Account(String account, Long amount) {
        Assert.hasText(account, "account must not be empty");
        Assert.hasText(String.valueOf(amount), "amount must not be empty");

        this.account = account;
        this.amount  = amount;

    }

    public AccountDto convertToDto(Account account){
        return new AccountDto(account.getAccount(), account.getAmount());
    }

}
