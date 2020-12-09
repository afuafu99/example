package com.example.dlock.service;


import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 基于V3版本的实现，如果要在生产环境使用，需要进一步完善。
 */
public class DLockService {

    private Client client;

    private Lock lockClient;

    private Lease leaseClient;

    public DLockService() {
        this.client = Client.builder().endpoints(new String[]{"http://etcdtest:2379"}).build();
        this.lockClient = client.getLockClient();
        this.leaseClient = client.getLeaseClient();
        System.out.println("....init.DLock..");
    }


    public DLockEtcd getLock(String lockName, long ttl, long timeout) {
        DLockEtcd dLockEtcd = DLockEtcd.builderDLockEtcd(lockName);

        // 创建一个租约，租约有效期为TTL。
        try {
            //获取租约ID
            Long leaseId = leaseClient.grant(ttl).get(timeout, TimeUnit.MILLISECONDS).getID();
            dLockEtcd.setLeaseId(leaseId);
            //锁的租期，通过定时器续租期
            dLockEtcd.setLeaseTask(new LeaseTask(leaseClient, leaseId), ttl);
        } catch (Exception e) {
            //todo 处理异常
            return dLockEtcd;
        }

        // 执行加锁操作，并为锁对应的key绑定租约
        try {
            LockResponse lr = lockClient.lock(ByteSequence.from(lockName, Charsets.UTF_8), dLockEtcd.getLeaseId()).get();
            dLockEtcd.setLockSuccess();
        } catch (Exception e) {
            //todo 处理异常,输出日志
        }

        return dLockEtcd;
    }

    /**
     * 解锁操作，释放锁、关闭定时任务、解除租约
     *
     * @param dLockEtcd:加锁操作返回的结果
     */
    public void releaseLock(DLockEtcd dLockEtcd) {
        System.out.println("releaseLock.Thread.Id:"+Thread.currentThread().getId());
        System.out.println("---------------------------------------------------");
        if (null == dLockEtcd || Strings.isNullOrEmpty(dLockEtcd.getLockName())) {
            //todo 输出日志
            return;
        }

        try {
            UnlockResponse ulr = lockClient.unlock(ByteSequence.from(dLockEtcd.getLockName(), Charsets.UTF_8)).get();
        } catch (Exception e) {
            //todo 处理异常,输出日志
        }

        try {
            dLockEtcd.shutdownSchedule();
        } catch (Exception e) {
            //todo 处理异常,输出日志
        }

        try {
            if (dLockEtcd.getLeaseId() != 0L) {
                leaseClient.revoke(dLockEtcd.getLeaseId());
            }
        } catch (Exception e) {
            //todo 处理异常,输出日志
        }
    }

}
