package com.example.threadlock.infrastructure.redis;

public enum Lock {
    //Domain Layer, You can set the Key from Domain Logic
    KEY,
    //Controller Layer, You can set the Key from PathVariable
    PATH_VARIABLE
}
