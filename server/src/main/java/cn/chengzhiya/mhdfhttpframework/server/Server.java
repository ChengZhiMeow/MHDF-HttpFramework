package cn.chengzhiya.mhdfhttpframework.server;

import org.apache.catalina.connector.Connector;

public interface Server {
    /**
     * 获取服务器实例
     *
     * @return 服务器实例
     */
    Connector getConnector();

    /**
     * 启动服务器
     */
    void start();
}
