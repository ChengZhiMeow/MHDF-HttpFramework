package cn.chengzhiya.mhdfhttpframework.server.entity;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Path {
    private String path;
    private RequestTypes type;

    public Path() {
    }

    public Path(String path, RequestTypes type) {
        this.path = path;
        this.type = type;
    }
}
