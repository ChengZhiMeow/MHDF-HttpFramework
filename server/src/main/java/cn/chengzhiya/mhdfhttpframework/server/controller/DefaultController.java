package cn.chengzhiya.mhdfhttpframework.server.controller;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestPath;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestType;
import cn.chengzhiya.mhdfhttpframework.server.util.HttpServerUtil;

import javax.servlet.http.HttpServletResponse;

@RequestPath("default")
public final class DefaultController {
    @RequestPath("default")
    @RequestType(RequestTypes.ALL)
    public static void handleDefault(HttpServletResponse response) {
        HttpServerUtil.returnStringData(response, "找不到接口!");
    }
}
