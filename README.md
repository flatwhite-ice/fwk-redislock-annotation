# fwk-redislock-annotation



@RedisLock annotation


as-is : 
@RedisLock(type = Lock.KEY, value = "account", action = Type.AS_FAR_AS_POSSIBLE, waittime = 30000L, leasetime = 30000L)

to-be : 
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.DEFAULT)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK, timeout = 30000L)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.TRYLOCK_WAITTIME_LEASETIME, waittime = 30000L, leasetime = 30000L)
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY) //leasetime default = -1L
@RedisLock(keytype = Control.KEY, key = "account", locktype = Lock.INTERRUPTIBLY, leasetime = 30000L)

annotation name : RedisLock
keytype = Control.KEY, Control.PATH_VARIABLE, Control.PARAMETER
key = "value"
locktype = Lock.DEFAULT, Lock.TRYLOCK, Lock.TRYLOCK_WAITTIME_LEASETIME, Lock.INTERRUPTIBLY,
waittime = 30000L
leasetime = 30000L