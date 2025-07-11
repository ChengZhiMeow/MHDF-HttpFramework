package cn.chengzhiya.mhdfhttpframework.client.exception;

public class RequestException extends Exception {
    public RequestException(String s) {
        super(s);
    }

    public RequestException(Throwable e) {
        super("发送网络请求的时候发生了错误", e);
    }
}
