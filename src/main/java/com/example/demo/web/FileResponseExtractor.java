package com.example.demo.web;

import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileResponseExtractor extends AbstractDisplayDownloadSpeedResponseExtractor<File> {
    //已下载的字节数
    private long byteCount;
    //文件的路径
    private String filePath;

    public FileResponseExtractor(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public File doExtractData(ClientHttpResponse clientHttpResponse) throws IOException {
        long contentLength = clientHttpResponse.getHeaders().getContentLength();
        InputStream in = clientHttpResponse.getBody();
        File file = new File(filePath);
        FileOutputStream out = new FileOutputStream(file);
        int byteRead;
        for (byte[] buffer = new byte[4096]; (byteRead = in.read(buffer)) != -1; byteCount += byteRead) {
            out.write(buffer,0,byteRead);
        }
        out.flush();
        out.close();
        return file;
    }

    @Override
    protected long getAlreadyDownloadLength() {
        return byteCount;
    }
}
