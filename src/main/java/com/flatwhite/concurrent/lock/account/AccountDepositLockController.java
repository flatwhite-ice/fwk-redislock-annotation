package com.flatwhite.concurrent.lock.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/account")
public class AccountDepositLockController {

    private final AccountDepositService accountDepositService;

    public AccountDepositLockController(AccountDepositService accountDepositService){
        this.accountDepositService = accountDepositService;
    }

    @GetMapping("/locked/{account}/deposit/{amount}")
    public AccountResponseDto deposit(@PathVariable String account, @PathVariable String amount){
        return this.accountDepositService.deposit(account, Long.valueOf(amount));
    }


}