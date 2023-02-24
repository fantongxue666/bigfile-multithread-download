package com.example.demo.web;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 这种方式会将文件的字节数组全部放入内存中，及其消耗资源，只适用于小文件的下载，如果下载几个G的文件，内存肯定是不够用的
 */
@Deprecated
public class ByteArrayResponseExtractor extends AbstractDisplayDownloadSpeedResponseExtractor<byte[]> {
    //保存已经下载的字节数
    private long byteCount;

    @Override
    public byte[] doExtractData(ClientHttpResponse clientHttpResponse) throws IOException {
        long contentLength = clientHttpResponse.getHeaders().getContentLength();
        ByteArrayOutputStream out = new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
        InputStream in = clientHttpResponse.getBody();
        int byteRead;
        for (byte[] buffer = new byte[4096]; (byteRead = in.read(buffer)) != -1; byteCount += byteRead) {
            out.write(buffer,0,byteRead);
        }
        out.flush();
        return out.toByteArray();
    }

    @Override
    protected long getAlreadyDownloadLength() {
        return byteCount;
    }
}
