package cn.chengzhiya.mhdfhttpframework.client;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.client.exception.ConnectionException;
import cn.chengzhiya.mhdfhttpframework.client.exception.DownloadException;
import cn.chengzhiya.mhdfhttpframework.client.exception.RequestException;
import cn.chengzhiya.mhdfhttpframework.client.exception.URLException;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class HttpClient implements Client {
    private final Map<String, String> headerHashMap = new ConcurrentHashMap<>();
    private final Map<String, String> cookieHashMap = new ConcurrentHashMap<>();
    private int timeout = 5000;
    private boolean ignoreSSL = false;

    public HttpClient() {
        this.getHeaderHashMap().put("Content-Type", "application/json");
        this.getHeaderHashMap().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
    }

    @Override
    public String formatUrl(String urlString) {
        return urlString;
    }

    @Override
    public HttpURLConnection getConnection(String urlString) throws URLException, ConnectionException {
        try {
            URL url = URI.create(this.formatUrl(urlString)).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            this.getHeaderHashMap().forEach(connection::setRequestProperty);
            if (!this.getCookieHashMap().isEmpty()) {
                connection.setRequestProperty("Cookie", this.getCookieHeader());
            }
            connection.setConnectTimeout(this.getTimeout());
            connection.setReadTimeout(this.getTimeout());

            if (connection instanceof HttpsURLConnection httpsConnection) {
                if (this.isIgnoreSSL()) {
                    TrustManager[] trustManagers = new TrustManager[]{
                            new X509TrustManager() {
                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                }

                                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                }
                            }
                    };

                    SSLContext sslContext = SSLContext.getInstance("SSL");
                    sslContext.init(null, trustManagers, new java.security.SecureRandom());

                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }
            }

            return connection;
        } catch (MalformedURLException e) {
            throw new URLException(e);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public String getData(HttpURLConnection connection) throws RequestException {
        try {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return in.readLine();
                }
            }

            throw new RequestException("在网络连接中未检索到数据");
        } catch (IOException e) {
            throw new RequestException(e);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public String request(String urlString, RequestTypes requestType, String data) throws RequestException, ConnectionException, URLException {
        try {
            HttpURLConnection connection = this.getConnection(urlString);
            Objects.requireNonNull(connection).setRequestMethod(requestType.name());

            if (data != null && !data.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(data.getBytes());
                    out.flush();
                }
            }

            List<String> cookiesHeader = connection.getHeaderFields().get("Set-Cookie");
            if (cookiesHeader != null) {
                for (String s : cookiesHeader) {
                    String[] cookie = s.split("=");

                    if (cookie.length != 2) {
                        continue;
                    }

                    this.getCookieHashMap().put(cookie[0], cookie[1]);
                }
            }

            return this.getData(connection);
        } catch (RequestException e) {
            return null;
        } catch (IOException e) {
            throw new RequestException("找不到请求类型: " + requestType.name());
        }
    }

    /**
     * 获取Cookie头字符串
     *
     * @return Cookie头字符串
     */
    public String getCookieHeader() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : this.getCookieHashMap().entrySet()) {
            builder
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append(";");
        }

        return builder.toString();
    }


    /**
     * 发送POST请求获取数据
     *
     * @param urlString 请求地址
     * @return 数据
     */
    public String post(String urlString) throws URLException, RequestException, ConnectionException {
        return this.post(urlString, null);
    }

    /**
     * 通过URL连接下载文件
     *
     * @param connection URL连接
     * @return 文件数据
     */
    public byte[] downloadFile(URLConnection connection) throws DownloadException {
        try (InputStream in = connection.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                throw new DownloadException("无可下载文件");
            }
            return bytes;
        } catch (IOException e) {
            throw new DownloadException(e);
        }
    }

    /**
     * 通过URL地址下载文件
     *
     * @param url URL地址
     * @return 文件数据
     */
    public byte[] downloadFile(String url) throws DownloadException, URLException, ConnectionException {
        return this.downloadFile(this.getConnection(url));
    }

    /**
     * 通过URL连接下载并保存文件
     *
     * @param connection URL连接
     * @param savePath   保存目录
     */
    public void downloadFile(URLConnection connection, Path savePath) throws DownloadException {
        try {
            Files.write(savePath, this.downloadFile(connection));
        } catch (IOException e) {
            throw new DownloadException(e);
        }
    }

    /**
     * 通过URL地址下载并保存文件
     *
     * @param url      URL地址
     * @param savePath 保存目录
     */
    public void downloadFile(String url, Path savePath) throws DownloadException, URLException, ConnectionException {
        this.downloadFile(this.getConnection(url), savePath);
    }
}
