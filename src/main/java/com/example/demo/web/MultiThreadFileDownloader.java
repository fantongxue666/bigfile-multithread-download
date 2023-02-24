package com.example.demo.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

@Deprecated
public class MultiThreadFileDownloader {

    private String str_url;
    private String storagePath;
    private int threadNumber;
    private static long downloadByteCount;

    MultiThreadFileDownloader(String str_url, String storagePath, int threadNumber) {
        this.str_url = str_url;
        this.storagePath = storagePath;
        this.threadNumber = threadNumber;
    }

    /**
     * 多线程基于文件下载
     */
    public void download() throws NoSuchAlgorithmException,
            KeyStoreException,
            KeyManagementException,
            IOException,
            InterruptedException {
        System.out.println("开始下载。。。");
        long startTime = System.currentTimeMillis();
        //先请求文件，得到文件总大小
        URL url = new URL(str_url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setRequestMethod("GET");
        // 得到需要下载的文件大小
        long fileLength = conn.getContentLengthLong();
        conn.disconnect();
        RandomAccessFile file = new RandomAccessFile(storagePath, "rwd");
        // 设置本地文件长度
        file.setLength(fileLength);
        file.close();
        CompletableFuture.runAsync(()->{
            long temp = 0;
            long speed;
            while (downloadByteCount < fileLength){
                speed = downloadByteCount - temp;
                temp = downloadByteCount;
                System.out.println("文件总大小： " + fileLength/1000 + " KB，已下载：" + (downloadByteCount / 1024) + "KB，下载速度："+ (speed/1000)+"KB/S");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        /*
         *  计算每条线程下载的字节数，以及每条线程起始下载位置与结束的下载位置，
         *  因为不一定平均分，所以最后一条线程下载剩余的字节
         *  然后创建线程任务并启动
         *  Main线程等待每条线程结束(join()方法)
         */
        long oneThreadReadByteLength = fileLength / threadNumber;
        CountDownLatch countDownLatch = new CountDownLatch(threadNumber);
        for (int i = 0; i < threadNumber; i++) {
            long startPosition = i * oneThreadReadByteLength;
            long endPosition = i == threadNumber - 1 ? fileLength : (i + 1) * oneThreadReadByteLength - 1;
            Thread t = new Thread(new Task(startPosition, endPosition,countDownLatch));
            t.start();
        }
        countDownLatch.await();
        /*
         *  检查文件是否下载完整，不完整则删除
         */
        if (downloadByteCount == fileLength) {
            System.out.println("下载完毕！总耗时长" + (System.currentTimeMillis() - startTime)/1000 + "s！");
        } else {
            System.out.println("下载错误！");
            new File(storagePath).delete();
        }
    }

    class Task implements Runnable {
        private long startPosition;
        private long endPosition;
        private CountDownLatch countDownLatch;

        Task(long startPosition, long endPosition,CountDownLatch countDownLatch) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.countDownLatch=countDownLatch;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(str_url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition); // 关键方法: 每条线程请求的字节范围
                if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) { // 关键响应码 ：206，请求成功 + 请求数据字节范围成功
                    RandomAccessFile file = new RandomAccessFile(storagePath, "rwd");
                    file.seek(startPosition); // 关键方法 ：每条线程起始写入文件的位置
                    InputStream in = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        file.write(buf, 0, len);
                        downloadByteCount += len;
                    }
                    // 关闭网络连接及本地流
                    in.close();
                    file.close();
                    conn.disconnect();
                    System.out.println(Thread.currentThread().getName() + ": download OK");
                }
            } catch (IOException e) {
                System.out.println(Thread.currentThread().getName() + "_Error : " + e);
            }finally {
                countDownLatch.countDown();
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, InterruptedException {
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU核数量: " + processors + "将开启"+processors+"个线程进行下载。。");
        String fileURL = "https://mirrors.tuna.tsinghua.edu.cn/eclipse/4diac/releases/2.0/4diac-ide/4diac-ide_2.0.0-linux.gtk.x86_64.tar.gz";
        MultiThreadFileDownloader multiThreadFileDownloader = new MultiThreadFileDownloader
                (fileURL, "C:\\Users\\fanto\\Downloads\\test.tar.gz", processors);
        multiThreadFileDownloader.download();

    }

}
