package com.example.threadlock.infrastructure.redis;

public enum Control {
    //Domain Layer, You can set the Key from Domain Logic
    KEY,
    //Controller Layer, You can set the Key from PathVariable
    PATH_VARIABLE
}
