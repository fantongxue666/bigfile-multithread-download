package com.example.demo.web;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import javax.xml.ws.spi.http.HttpContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;


public class RestTemplateBuilder {


    public static RestTemplateBuilder builder(){
        return new RestTemplateBuilder();
    }

    public RestTemplate build() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSL factory = new SSL();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(15000);
        return new RestTemplate(factory);
    }

    public static class SSL extends SimpleClientHttpRequestFactory {

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                throws IOException {
            if (connection instanceof HttpsURLConnection) {
                prepareHttpsConnection((HttpsURLConnection) connection);
            }
            super.prepareConnection(connection, httpMethod);
        }

        private void prepareHttpsConnection(HttpsURLConnection connection) {
            connection.setHostnameVerifier(new SkipHostnameVerifier());
            try {
                connection.setSSLSocketFactory(createSslSocketFactory());
            }
            catch (Exception ex) {
                // Ignore
            }
        }

        private SSLSocketFactory createSslSocketFactory() throws Exception {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { new SkipX509TrustManager() },
                    new SecureRandom());
            return context.getSocketFactory();
        }

        private class SkipHostnameVerifier implements HostnameVerifier {

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }

        }

        private static class SkipX509TrustManager implements X509TrustManager {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

        }

    }



}
