package com.gotogames.common.lock;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class enables to use lock/unlock mechanism for value key like string, int ... It use array of ReetrantLock which the selection of element is realized using the hashCode % array size.
 * It's important to choose the capacity according to the use of lock.
 * WARNING : Do not use many locks successively (overlap). This sample code is not a good idea : l1.lock();l2.lock(); try{...} finally {l1.unlock();l2.unlock();} l1 and l2 can have the same hashCode event if they are different !
 */
public class LockValue {
    private ReentrantLock[] locks = null;

    public LockValue(int arrayCapacity) {
        if (arrayCapacity <= 0) {
            arrayCapacity = 1;
        }
        locks = new ReentrantLock[arrayCapacity];
        Arrays.setAll(locks, value -> new ReentrantLock());
    }

    private ReentrantLock getLockAtIndex(int idx) {
        if (idx >=0 && idx < locks.length) {
            return locks[idx];
        }
        return locks[0];
    }

    public ReentrantLock getLockForString(String str) {
        return getLockAtIndex(Math.abs(str.hashCode() % locks.length));
    }

    public ReentrantLock getLockForLong(Long value) {
        return getLockAtIndex(Math.abs(value.hashCode() % locks.length));
    }

    public ReentrantLock getLockForInt(int value) {
        return getLockAtIndex(Math.abs(value % locks.length));
    }
}
