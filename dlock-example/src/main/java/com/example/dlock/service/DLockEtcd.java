package com.example.dlock.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * etcd实现的锁对象
 */
public class DLockEtcd  {
    public static DLockEtcd builderDLockEtcd(String lockName) {
        return new DLockEtcd(false, Executors.newSingleThreadScheduledExecutor(), lockName);
    }

    private DLockEtcd(boolean isLock, ScheduledExecutorService service, String lockName) {
        this.isLock = isLock;
        this.service = service;
        this.lockName = lockName;
    }

    private boolean isLock;

    private long leaseId;

    private ScheduledExecutorService service;

    private String lockName;

    public String getLockName() {
        return lockName;
    }

    public void setLeaseId(long leaseId) {
        this.leaseId = leaseId;
    }


    public long getLeaseId() {
        return this.leaseId;
    }

    public ScheduledExecutorService getService() {
        return this.service;
    }

    public boolean isLock() {
        return isLock;
    }

    private void setLock(boolean lock) {
        isLock = lock;
    }

    public void setLockSuccess() {
        this.setLock(true);
    }

    /**
     * 启动定时任务续约，心跳周期和初次启动延时计算公式如下，可根据实际业务制定。
     * 要考虑一个问题就是full.gc的情况下,period太短导致锁的租期不能续租，不然会出现二个业务同时获得分布式锁的情况，这种问题很难定位。
     * 这个问题要结合具体的环境优化后的full.gc时间考虑，至少period>full.gc
     * @param leaseTask
     * @param ttl
     */
    public void setLeaseTask(Runnable leaseTask, long ttl) {
        long initialDelay = ttl / 2;
        long period = ttl - ttl / 3;
        this.service.scheduleAtFixedRate(leaseTask, initialDelay, period, TimeUnit.MILLISECONDS);
    }


    // 关闭定时任务
    public void shutdownSchedule() {
        if (null != this.service)
            this.service.shutdown();
    }
}
