package cn.chengzhiya.mhdfhttpframework.server;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;
import cn.chengzhiya.mhdfhttpframework.server.annotation.*;
import cn.chengzhiya.mhdfhttpframework.server.entity.FilterConfig;
import cn.chengzhiya.mhdfhttpframework.server.entity.Path;
import cn.chengzhiya.mhdfhttpframework.server.entity.SSLConfig;
import cn.chengzhiya.mhdfhttpframework.server.enums.ServerStatus;
import cn.chengzhiya.mhdfhttpframework.server.filter.CorsFilter;
import cn.chengzhiya.mhdfhttpframework.server.util.HttpServerUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class HttpServer extends HttpServlet implements Server {
    private final SSLConfig sslConfig;
    private final int port;

    private final Map<String, Map<Path, Method>> controllerlHashMap = new HashMap<>();
    private final List<FilterConfig> filterConfigList = new ArrayList<>();
    private Tomcat server;

    @Setter
    private ServerStatus status = ServerStatus.STOPPED;

    public HttpServer(int port, SSLConfig sslConfig) {
        this.port = port;
        this.sslConfig = sslConfig;

        getFilterConfigList().add(
                new FilterConfig("corsFilter", new CorsFilter())
        );
        getFilterConfigList().add(
                new FilterConfig("corsFilter", "/*")
        );

        reloadController();
    }

    /**
     * 重载所有接口
     */
    public void reloadController() {
        getControllerlHashMap().clear();
        try {
            Reflections reflections = new Reflections(ConfigurationBuilder.build().forPackages(""));

            for (Class<?> clazz : reflections.getTypesAnnotatedWith(RequestPath.class)) {
                String path = clazz.getAnnotation(RequestPath.class).value();
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }

                Map<Path, Method> map = getControllerlHashMap().getOrDefault(path, new HashMap<>());
                for (Method method : clazz.getMethods()) {
                    RequestPath methodPathAnnotation = method.getAnnotation(RequestPath.class);
                    RequestType methodRequestTypeAnnotation = method.getAnnotation(RequestType.class);
                    if (methodPathAnnotation == null || methodRequestTypeAnnotation == null) {
                        continue;
                    }

                    String methodPath = methodPathAnnotation.value();
                    if (methodPath.isEmpty()) {
                        continue;
                    }
                    if (!methodPath.startsWith("/")) {
                        methodPath = "/" + methodPath;
                    }

                    RequestTypes methodRequestType = methodRequestTypeAnnotation.value();
                    map.put(new Path(methodPath, methodRequestType), method);
                }

                getControllerlHashMap().put(path, map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connector getConnector() {
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(getPort());

        if (getSslConfig().isEnable()) {
            connector.setSecure(true);
            connector.setScheme("https");
            connector.setAttribute("keyAlias", getSslConfig().getAlias());
            connector.setAttribute("keystorePass", getSslConfig().getKey());
            connector.setAttribute("keystoreFile", getSslConfig().getFile());
            connector.setAttribute("clientAuth", "false");
            connector.setAttribute("sslProtocol", "TLS");
            connector.setAttribute("SSLEnabled", "true");
        }

        return connector;
    }

    @Override
    public void start() {
        this.setStatus(ServerStatus.STARTING);
        new Thread(() -> {
            try {
                this.server = new Tomcat();

                Connector connector = this.getConnector();
                this.server.getService().addConnector(connector);

                Context context = this.server.addContext("", null);
                Tomcat.addServlet(context, "dispatcherServlet", this);
                context.addServletMappingDecoded("/", "dispatcherServlet");

                for (FilterConfig filterConfig : getFilterConfigList()) {
                    switch (filterConfig.getType()) {
                        case URL -> context.addFilterMap(filterConfig.toFilterMap());
                        case ENTITY -> context.addFilterDef(filterConfig.toFilterDef());
                    }
                }

                this.server.start();
                this.setStatus(ServerStatus.RUNNING);

                this.server.getServer().await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 获取接口方法实例
     *
     * @param path 路径
     * @param type 请求类型
     * @return 接口方法实例
     */
    private Method getRequestMethod(String path, String methodPath, RequestTypes type) {
        if (path.isEmpty()) {
            path = methodPath;
        }
        String controllerKey = getControllerlHashMap().containsKey(path) ? path : "/default";
        Map<Path, Method> controllerMap = getControllerlHashMap().getOrDefault(controllerKey, new HashMap<>());

        Map<RequestTypes, Method> methodMap = new HashMap<>();
        controllerMap.entrySet().stream()
                .filter(e -> e.getKey().getPath().equals(methodPath))
                .forEach(e -> methodMap.put(e.getKey().getType(), e.getValue()));

        if (methodMap.isEmpty()) {
            controllerMap.entrySet().stream()
                    .filter(e -> e.getKey().getPath().equals("/default"))
                    .forEach(e -> methodMap.put(e.getKey().getType(), e.getValue()));
        }

        return methodMap.getOrDefault(type, methodMap.get(RequestTypes.ALL));
    }

    /**
     * 处理请求方法
     *
     * @param request  请求实例
     * @param response 回应实例
     * @param method   请求方法实例
     */
    private void handleRequestMethod(HttpServletRequest request, HttpServletResponse response, Method method) {
        JSONObject body = HttpServerUtil.getRequestBody(request);
        assert body != null;

        List<Object> parameterList = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            // 获取基础HTTP参数
            {
                if (parameter.getType().equals(HttpServletRequest.class)) {
                    parameterList.add(request);
                    continue;
                }
                if (parameter.getType().equals(HttpServletResponse.class)) {
                    parameterList.add(response);
                    continue;
                }
            }

            // 获取请求数据中的数据
            {
                BodyData annotation = method.getAnnotation(BodyData.class);
                if (annotation != null) {
                    String key = annotation.value();

                    if (key.equals("body")) {
                        parameterList.add(body);
                        continue;
                    }

                    Object value = body.getObject(key, parameter.getType());
                    if (value == null) {
                        parameterList.add(null);
                        continue;
                    }

                    parameterList.add(value);
                    continue;
                }
            }

            String paramData = null;

            // 设定默认值
            {
                DefaultValue annotation = parameter.getAnnotation(DefaultValue.class);
                if (annotation != null) {
                    paramData = annotation.value();
                }
            }

            // 获取cookie中的数据
            {
                if (request.getCookies() != null) {
                    CookieData annotation = parameter.getAnnotation(CookieData.class);
                    if (annotation != null) {
                        for (Cookie cookie : request.getCookies()) {
                            if (!cookie.getName().equals(annotation.value())) {
                                continue;
                            }

                            if (cookie.getValue() == null) {
                                continue;
                            }

                            paramData = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            // 获取请求参数中的数据
            {
                RequestParam annotation = method.getAnnotation(RequestParam.class);
                if (annotation != null) {
                    String value = request.getParameter(annotation.value());
                    if (value != null) {
                        paramData = value;
                    }
                }
            }

            parameterList.add(HttpServerUtil.converter(
                    parameter.getType(),
                    paramData
            ));
        }

        try {
            method.invoke(null, parameterList.toArray());
        } catch (Exception e) {
            throw new RuntimeException("接口方法必须为 static", e);
        }
    }

    /**
     * 处理请求
     *
     * @param request  请求实例
     * @param response 回应实例
     * @param type     请求类型实例
     */
    private void handleRequest(HttpServletRequest request, HttpServletResponse response, RequestTypes type) {
        String uri = request.getRequestURI();
        String path = uri.substring(0, uri.lastIndexOf("/"));
        String methodPath = uri.substring(path.length());

        Method method = getRequestMethod(path, methodPath, type);
        if (method == null) {
            return;
        }

        handleRequestMethod(request, response, method);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.GET);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.POST);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.HEAD);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.OPTIONS);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.PUT);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.DELETE);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, RequestTypes.TRACE);
    }
}
