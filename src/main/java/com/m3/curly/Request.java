/*
 * Copyright 2011-2012 M3, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.m3.curly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Request
 */
public class Request {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String DEFAULT_USER_AGENT = "Curly HTTP Client (https://github.com/m3dev/curly)";
    public static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    private boolean enableThrowingIOException = false;
    private boolean followRedirects = HttpURLConnection.getFollowRedirects();
    private String url;
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 10000;
    private String referer;
    private String userAgent = DEFAULT_USER_AGENT;
    private String charset = DEFAULT_CHARSET;
    private Map<String, String> headers = new HashMap<String, String>();
    private List<QueryParam> queryParams = new ArrayList<QueryParam>();
    private RequestBody requestBody = new RequestBody(this);
    private Map<String, ?> formParams = new HashMap<String, Object>();
    private List<? extends FormData> multipartFormData = new ArrayList<FormData>();

    public Request(String url) {
        setUrl(url);
    }

    public Request(String url, String charset) {
        setUrl(url);
        setCharset(charset);
    }

    public Request(String url, Map<String, ?> formParams) {
        setUrl(url);
        setFormParams(formParams);
    }

    public HttpURLConnection toHttpURLConnection(Method method) throws IOException {


        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

// Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }


        // set additional query parameters
        if (getQueryParams() != null && getQueryParams().size() > 0) {
            for (QueryParam queryParam : getQueryParams()) {
                if (queryParam != null && queryParam.getValue() != null) {
                    String name = queryParam.getName();
                    String value = String.valueOf(queryParam.getValue());
                    String newParam = HTTP.urlEncode(name) + "=" + HTTP.urlEncode(value);
                    url += (url.contains("?") ? "&" : "?") + newParam;
                }
            }
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method.toString());
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setInstanceFollowRedirects(followRedirects);

        // HTTP header injection is checked by HttpURLConnection
        conn.setRequestProperty("User-Agent", getUserAgent());
        for (String headerKey : headers.keySet()) {
            conn.setRequestProperty(headerKey, headers.get(headerKey));
        }

        return conn;
    }

    public boolean isEnableThrowingIOException() {
        return enableThrowingIOException;
    }

    public Request setEnableThrowingIOException(boolean enableThrowingIOException) {
        this.enableThrowingIOException = enableThrowingIOException;
        return this;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public Request setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Request setUrl(String url) {
        this.url = url;
        return this;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public Request setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public Request setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
        return this;
    }

    public String getReferer() {
        return referer;
    }

    public Request setReferer(String referer) {
        this.referer = referer;
        return this;
    }

    public String getUserAgent() {
        if (headers.get("User-Agent") != null) {
            return headers.get("User-Agent");
        } else {
            return userAgent;
        }
    }

    public Request setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public String getCharset() {
        return charset;
    }

    public Request setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    public String getHeader(String name) {
        String specifiedHeaderValue = headers.get(name);
        if (specifiedHeaderValue != null) {
            return specifiedHeaderValue;
        } else {
            try {
                return toHttpURLConnection(Method.GET).getHeaderField(name);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public Request setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Request addQueryParam(String name, Object value) {
        return addQueryParam(new QueryParam(name, value));
    }

    public Request addQueryParam(QueryParam queryParam) {
        getQueryParams().add(queryParam);
        return this;
    }

    public List<QueryParam> getQueryParams() {
        return queryParams;
    }

    public Request setQueryParams(List<QueryParam> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public RequestBody getRequestBody() {
        return requestBody;
    }

    public Request setBody(byte[] body, String contentType) {
        this.getRequestBody().setBody(body, contentType);
        return this;
    }

    public byte[] getBytes() {
        return getRequestBody().getBytes();
    }

    public String getContentType() {
        return getRequestBody().getContentType();
    }

    public Map<String, ?> getFormParams() {
        return formParams;
    }

    public Request setFormParams(Map<String, ?> formParams) {
        this.formParams = formParams;
        return this;
    }

    public List<? extends FormData> getMultipartFormData() {
        return multipartFormData;
    }

    public Request setMultipartFormData(List<? extends FormData> multipartFormData) {
        this.multipartFormData = multipartFormData;
        return this;
    }

}
