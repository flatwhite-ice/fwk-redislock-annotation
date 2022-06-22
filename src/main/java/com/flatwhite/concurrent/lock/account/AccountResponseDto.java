package com.flatwhite.concurrent.lock.account;

import lombok.Data;

@Data
public class AccountResponseDto {

    private String msg;

    private String code;

    private String result;

    private AccountDto data;

}
