package cn.chengzhiya.mhdfhttpframework.example.controller;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.server.annotation.Priority;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestPath;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestType;
import cn.chengzhiya.mhdfhttpframework.server.util.HttpServerUtil;

import javax.servlet.http.HttpServletResponse;

@RequestPath("/hello")
public final class NoHelloController {
    @Priority(-999)
    @RequestPath("default")
    @RequestType(RequestTypes.GET)
    public static void world(HttpServletResponse response) {
        HttpServerUtil.returnStringData(response, "server is gg");
    }
}
