package com.example.dlock.service;


import io.etcd.jetcd.Lease;

/**
 * 在等待其它客户端释放锁期间，通过心跳续约，保证自己的锁对应租约不会失效
 */
public class LeaseTask implements Runnable {
    private Lease leaseClient;
    private long leaseId;

    LeaseTask(Lease leaseClient, long leaseId) {
        this.leaseClient = leaseClient;
        this.leaseId = leaseId;
    }

    @Override
    public void run() {
        // 续约一次
        leaseClient.keepAliveOnce(leaseId);
    }
}
