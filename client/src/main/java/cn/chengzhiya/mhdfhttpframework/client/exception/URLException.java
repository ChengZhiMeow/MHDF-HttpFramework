package cn.chengzhiya.mhdfhttpframework.client.exception;

public class URLException extends Exception {
    public URLException(Throwable e) {
        super("处理网页地址的时候发生了问题", e);
    }
}
