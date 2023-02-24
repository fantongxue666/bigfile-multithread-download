package com.example.demo.web;

import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class FileDownloader {

    /**
     * 单线程基于内存下载
     *
     * @param fileURL
     * @param filePath
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public void downloadToMemory(String fileURL, String filePath) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        long start = System.currentTimeMillis();
        RestTemplate restTemplate = RestTemplateBuilder.builder().build();

        byte[] bytes = restTemplate.execute(fileURL, HttpMethod.GET, null, new ByteArrayResponseExtractor());
        try {
            Files.write(Paths.get(filePath), Objects.requireNonNull(bytes));
            System.out.println("总共文件下载耗时：" + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 单线程基于文件下载
     *
     * @param fileURL
     * @param filePath
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public void downloadToFile(String fileURL, String filePath) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        long start = System.currentTimeMillis();
        RestTemplate restTemplate = RestTemplateBuilder.builder().build();
        FileResponseExtractor fileResponseExtractor = new FileResponseExtractor(filePath + ".download");
        File tempFile = restTemplate.execute(fileURL, HttpMethod.GET, null, fileResponseExtractor);
        tempFile.renameTo(new File(filePath));
        System.out.println("总共文件下载耗时：" + (System.currentTimeMillis() - start) / 1000 + "s");
    }

    /**
     * 多线程基于文件下载
     *
     * @param fileURL
     * @param filePath
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws IOException
     */
    public void downloadToFileMultiThread(String fileURL, String filePath, int threadNumber) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, InterruptedException {
        long start = System.currentTimeMillis();
        //先请求文件，得到文件总大小
        URL url = new URL(fileURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setRequestMethod("GET");
        // 得到需要下载的文件大小
        long fileLength = conn.getContentLengthLong();
        conn.disconnect();

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
            Thread t = new Thread(new Task(startPosition, endPosition, countDownLatch, filePath, fileURL, "download" + i));
            t.start();
        }
        countDownLatch.await();
        mergeTempFiles(filePath, threadNumber);
        System.out.println("总共文件下载耗时：" + (System.currentTimeMillis() - start) / 1000 + "s");
    }

    public void mergeTempFiles(String filePath, int threadNumber) throws IOException, InterruptedException {
        System.out.println("开始合并");
        //开始合并
        OutputStream os = new BufferedOutputStream(new FileOutputStream(filePath));
        //对临时目录的所有文件分片进行遍历，进行合并
        for (int i = 0; i < threadNumber; i++) {
            File tempFile = new File(filePath + ".download" + String.valueOf(i));
            System.out.println("文件名称：" + tempFile.getAbsolutePath());
            while (!tempFile.exists()) {
                Thread.sleep(100);
            }
            byte[] bytes = FileUtils.readFileToByteArray(tempFile);
            os.write(bytes);
            os.flush();
            tempFile.delete();
        }
        os.close();
    }

    class Task implements Runnable {
        private long startPosition;
        private long endPosition;
        private CountDownLatch countDownLatch;
//        private RestTemplate restTemplate;
        private String filePath;
        private String fileURL;
        private String tempName;

        Task(long startPosition,
             long endPosition,
             CountDownLatch countDownLatch,
             String filePath,
             String fileURL,
             String tempName) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.countDownLatch = countDownLatch;
            this.filePath = filePath;
            this.fileURL = fileURL;
            this.tempName = tempName;
        }

        @Override
        public void run() {
            try {
                RestTemplate restTemplate = RestTemplateBuilder.builder().build();
                FileResponseExtractor fileResponseExtractor = new FileResponseExtractor(filePath + "." + tempName);
                // 借助拦截器的方式来实现塞统一的请求头
                ClientHttpRequestInterceptor interceptor = (httpRequest, bytes, execution) -> {
                    httpRequest.getHeaders().set("Range", "bytes=" + startPosition + "-" + endPosition);
                    return execution.execute(httpRequest, bytes);
                };
                restTemplate.getInterceptors().add(interceptor);
                File tempFile = restTemplate.execute(fileURL, HttpMethod.GET, null, fileResponseExtractor);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }


    /**
     * 启动入口
     *
     * @param args
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, InterruptedException {
        FileDownloader fileDownloader = new FileDownloader();
//        String fileURL = "https://img-blog.csdnimg.cn/68aa9807555949a38461d2abff82b22c.png";
        String fileURL = "https://mirrors.tuna.tsinghua.edu.cn/eclipse/4diac/releases/2.0/4diac-ide/4diac-ide_2.0.0-linux.gtk.x86_64.tar.gz";
//        fileDownloader.downloadToMemory(fileURL,"C:\\Users\\fanto\\Downloads\test.tar.gz");
//        fileDownloader.downloadToFile(fileURL, "C:\\Users\\fanto\\Downloads\\test.tar.gz");
        fileDownloader.downloadToFileMultiThread(fileURL, "C:\\Users\\fanto\\Downloads\\test.tar.gz", 10);
    }
}
