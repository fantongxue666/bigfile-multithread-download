package com.example.demo.web;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractDisplayDownloadSpeedResponseExtractor<T> implements ResponseExtractor<T>,DisplayDownloadSpeed {
    /**
     * 显示下载速度
     */
    @Override
    public void displaySpeed(String task, long contentLength) {
        long totalSize = contentLength / 1024;
        CompletableFuture.runAsync(()->{
            long temp = 0;
            long speed;
            StringBuilder stringBuilder = new StringBuilder();
            while (contentLength - temp > 0){
                speed = getAlreadyDownloadLength() - temp;
                temp = getAlreadyDownloadLength();
                stringBuilder.append("\r");//不换行进行覆盖
                stringBuilder.append(task + " 文件总大小： " + totalSize + " KB，已下载：" + (temp / 1024) + "KB，下载速度："+ (speed/1000)+"KB/S");
                System.out.print(stringBuilder.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public T extractData(ClientHttpResponse clientHttpResponse) throws IOException {
        long contentLength = clientHttpResponse.getHeaders().getContentLength();
        this.displaySpeed(Thread.currentThread().getName(),contentLength);
        return this.doExtractData(clientHttpResponse);
    }

    public abstract T doExtractData(ClientHttpResponse clientHttpResponse) throws IOException;

    /**
     * 获取已经下载了多少字节
     */
    protected abstract long getAlreadyDownloadLength();

}
