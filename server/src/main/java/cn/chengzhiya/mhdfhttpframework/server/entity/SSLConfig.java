package cn.chengzhiya.mhdfhttpframework.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class SSLConfig {
    private boolean enable;
    private String alias;
    private String file;
    private String key;

    public SSLConfig() {
        this.enable = false;
    }

    public SSLConfig(boolean enable, String alias, String file, String key) {
        this.enable = enable;
        this.alias = alias;
        this.file = file;
        this.key = key;
    }
}
