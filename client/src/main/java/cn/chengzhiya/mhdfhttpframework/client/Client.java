package cn.chengzhiya.mhdfhttpframework.client;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.client.exception.ConnectionException;
import cn.chengzhiya.mhdfhttpframework.client.exception.RequestException;
import cn.chengzhiya.mhdfhttpframework.client.exception.URLException;

import java.net.HttpURLConnection;

public interface Client {
    /**
     * 格式化请求地址
     *
     * @param urlString 请求地址
     * @return 格式化后的地址
     */
    String formatUrl(String urlString);

    /**
     * 获取连接实例
     *
     * @param urlString 请求地址
     * @return 连接地址
     */
    HttpURLConnection getConnection(String urlString) throws URLException, ConnectionException;

    /**
     * 获取指定连接实例的数据
     *
     * @param connection 连接实例
     * @return 数据
     */
    String getData(HttpURLConnection connection) throws RequestException;


    /**
     * 发送请求并尝试获取数据
     *
     * @param urlString   请求地址
     * @param requestType 请求方式
     * @param data        负载数据
     * @return 数据
     */
    String request(String urlString, RequestTypes requestType, String data) throws RequestException, ConnectionException, URLException;

    /**
     * 发送GET请求并尝试获取数据
     *
     * @param urlString 请求地址
     * @return 数据
     */
    default String get(String urlString) throws RequestException, ConnectionException, URLException {
        return this.request(urlString, RequestTypes.GET, null);
    }

    /**
     * 发送POST请求并尝试获取数据
     *
     * @param urlString 请求地址
     * @param data      负载数据
     * @return 数据
     */
    default String post(String urlString, String data) throws RequestException, ConnectionException, URLException {
        return this.request(urlString, RequestTypes.GET, data);
    }
}
