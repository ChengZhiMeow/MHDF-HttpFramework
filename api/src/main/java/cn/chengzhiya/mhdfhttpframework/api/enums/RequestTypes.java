package cn.chengzhiya.mhdfhttpframework.api.enums;

public enum RequestTypes {
    ALL,
    GET,
    POST,
    HEAD,
    OPTIONS,
    PUT,
    DELETE,
    TRACE;


    /**
     * 检查是否为目标请求类型
     *
     * @param type   当前类型
     * @param target 目标类型
     * @return 结果
     */
    public boolean isRequestType(RequestTypes target) {
        if (this == ALL) return true;
        return this == target;
    }
}
