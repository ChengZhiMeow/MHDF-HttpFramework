package cn.chengzhiya.mhdfhttpframework.example.controller;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestPath;
import cn.chengzhiya.mhdfhttpframework.server.annotation.RequestType;
import cn.chengzhiya.mhdfhttpframework.server.util.HttpServerUtil;

import javax.servlet.http.HttpServletResponse;

@RequestPath("/hello")
public final class HelloController {
    @RequestPath("default")
    @RequestType(RequestTypes.GET)
    public static void world(HttpServletResponse response) {
        HttpServerUtil.returnStringData(response, "world");
    }
}
