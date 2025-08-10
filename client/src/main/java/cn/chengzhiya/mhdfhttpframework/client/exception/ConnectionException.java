package cn.chengzhiya.mhdfhttpframework.client.exception;

public final class ConnectionException extends Exception {
    public ConnectionException(Throwable e) {
        super("打开网络连接的时候发生了问题", e);
    }
}
