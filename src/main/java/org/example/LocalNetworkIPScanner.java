package org.example;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author: angeya
 * @Date: 2024/7/2 19:13
 * @Description:
 */
public class LocalNetworkIPScanner {

    private static final String SUBNET = "10.3.24";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        async();
        // sync();
        System.out.println("耗时：" + (System.currentTimeMillis() - startTime));
    }

    private static void sync() {

        for (int i = 1; i <= 255; i++) {
            String host = SUBNET + "." + i;
            try {
                InetAddress inetAddress = InetAddress.getByName(host);
                if (inetAddress.isReachable(150)) {
                    String hostName = inetAddress.getHostName();
                    System.out.println("IP:" + host + " --- Host: " + hostName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 多线程异步扫描
     */
    private static void async() {
        List<HostInfo> list = new ArrayList<>();

        for (int i = 1; i <= 255; i++) {
            String host = SUBNET + "." + i;
            list.add(new HostInfo(host));
        }

        // 这里保持多线程有序的方法就是对list进行parallelStream操作
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        list.parallelStream().forEach(hostInfo -> {
            try {
                String ip = hostInfo.ip;
                InetAddress inetAddress = InetAddress.getByName(ip);
                if (inetAddress.isReachable(300)) {
                    hostInfo.isOK = true;
                    hostInfo.hostName = inetAddress.getHostName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        list.forEach(hostInfo -> {
            if (hostInfo.isOK) {
                System.out.println("IP:" + hostInfo.ip + " --- Host: " + hostInfo.hostName);
            }
        });


    }

    /**
     * 主机信息
     */
    private static class HostInfo{
        public HostInfo(String ip) {
            this.ip = ip;
        }

        private String ip;

        private String hostName;

        private boolean isOK = false;
    }
}
