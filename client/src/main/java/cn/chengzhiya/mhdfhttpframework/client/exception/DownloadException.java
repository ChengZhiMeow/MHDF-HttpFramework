package cn.chengzhiya.mhdfhttpframework.client.exception;

public final class DownloadException extends Exception {
    public DownloadException(String s) {
        super(s);
    }

    public DownloadException(Throwable e) {
        super("下载文件的时候发生了问题", e);
    }
}
