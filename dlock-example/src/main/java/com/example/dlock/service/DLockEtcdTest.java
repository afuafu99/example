package com.example.dlock.service;


/**
 * 这里强调一个V3的客户端，jetcd-core。使用0.0.2版本连接V3会报错，具体的错误没有深究。
 * “jetcd is the official java client for etcd v3.”
 * https://github.com/etcd-io/jetcd
 */
public class DLockEtcdTest {


    public static void main(String[] args) {
        new DLockEtcdTest().dlockTest();
    }

    public void dlockTest() {
        DLockService dLockService = new DLockService();
        for (int i = 0; i < 5; i++) {
            new Thread(new Business(i, dLockService)).start();
        }

        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
        }
    }

    public static class Business implements Runnable {

        private int orderId;

        private DLockService dLockService;

        public Business(int orderId, DLockService dLockService) {
            this.orderId = orderId;
            this.dLockService = dLockService;
        }

        @Override
        public void run() {
            DLockEtcd dLockEtcd = null;
            try {
                //通常是对同一个订单互斥orderId
                String lockName = "order_";
                // 加锁
                //在测试的环境，我使用的是普通的机械硬盘，ttl的时间要设长一点，不然会报错
                dLockEtcd = dLockService.getLock(lockName, 3000, 20000);
                System.out.println("Thread.Id:" + Thread.currentThread().getId() + "," + dLockEtcd.isLock());
                if (dLockEtcd.isLock()) {
                    System.out.println("Locked.Thread.Id:" + Thread.currentThread().getId());
                    // 获得锁后，执行业务，用sleep方法模拟.
                    Thread.sleep(3000);
                } else {
                    System.out.println("getLock.fail...." + lockName);
                }
            } catch (Exception e) {
                System.err.println("error:" + e);
            } finally {
                if (null != dLockService)
                    dLockService.releaseLock(dLockEtcd);
            }
        }
    }
}
