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
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("deprecation")
@Getter
public class HttpServer extends HttpServlet implements Server {
    private final SSLConfig sslConfig;
    private final int port;

    private final Map<String, Map<Path, List<Method>>> controllerlHashMap = new HashMap<>();
    private final List<FilterConfig> filterConfigList = new ArrayList<>();
    private Tomcat server;

    @Setter
    private ServerStatus status = ServerStatus.STOPPED;

    public HttpServer(int port, SSLConfig sslConfig) {
        this.port = port;
        this.sslConfig = sslConfig;

        this.getFilterConfigList().add(
                new FilterConfig("corsFilter", new CorsFilter())
        );
        this.getFilterConfigList().add(
                new FilterConfig("corsFilter", "/*")
        );

        this.reloadController();
    }

    /**
     * 重载所有接口
     */
    public void reloadController() {
        this.getControllerlHashMap().clear();
        try {
            Reflections reflections = new Reflections(ConfigurationBuilder.build().forPackages(""));

            for (Class<?> clazz : reflections.getTypesAnnotatedWith(RequestPath.class)) {
                String path = clazz.getAnnotation(RequestPath.class).value();
                if (!path.startsWith("/")) path = "/" + path;

                Map<Path, List<Method>> map = this.getControllerlHashMap().getOrDefault(path, new HashMap<>());
                for (Method method : clazz.getMethods()) {
                    RequestPath methodPathAnnotation = method.getAnnotation(RequestPath.class);
                    RequestType methodRequestTypeAnnotation = method.getAnnotation(RequestType.class);

                    if (methodPathAnnotation == null || methodRequestTypeAnnotation == null) continue;

                    String methodPath = methodPathAnnotation.value();
                    if (methodPath.isEmpty()) continue;
                    if (!methodPath.startsWith("/")) methodPath = "/" + methodPath;

                    RequestTypes methodRequestType = methodRequestTypeAnnotation.value();
                    Path p = new Path(methodPath, methodRequestType);

                    List<Method> list = map.getOrDefault(p, new CopyOnWriteArrayList<>());
                    list.add(method);
                    map.put(p, list);
                }

                this.getControllerlHashMap().put(path, map);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connector getConnector() {
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(this.getPort());

        if (this.getSslConfig().isEnable()) {
            connector.setSecure(true);
            connector.setScheme("https");
            connector.setAttribute("keyAlias", this.getSslConfig().getAlias());
            connector.setAttribute("keystorePass", this.getSslConfig().getKey());
            connector.setAttribute("keystoreFile", this.getSslConfig().getFile());
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

                for (FilterConfig filterConfig : this.getFilterConfigList()) {
                    switch (filterConfig.getType()) {
                        case URL -> context.addFilterMap(filterConfig.toFilterMap());
                        case ENTITY -> context.addFilterDef(filterConfig.toFilterDef());
                    }
                }

                this.server.start();
                this.setStatus(ServerStatus.RUNNING);

                this.server.getServer().await();
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
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
    private List<Method> getRequestMethodList(String path, String methodPath, RequestTypes type) {
        if (path.isEmpty()) path = methodPath;

        Map<Path, List<Method>> methodMap = this.getControllerlHashMap().getOrDefault(path, new HashMap<>());
        methodMap.putAll(this.getControllerlHashMap().getOrDefault("/default", new HashMap<>()));

        List<Method> list = new ArrayList<>();
        methodMap.entrySet().stream()
                .filter(e -> e.getKey().getType().isRequestType(type))
                .filter(e -> e.getKey().getPath().equals(methodPath) || e.getKey().getPath().equals("/default"))
                .forEach(e -> list.addAll(e.getValue()));

        list.sort((m1, m2) -> {
            Priority p1 = m1.getAnnotation(Priority.class);
            Priority p2 = m2.getAnnotation(Priority.class);
            int priority1 = p1 != null ? p1.value() : 0;
            int priority2 = p2 != null ? p2.value() : 0;

            return Integer.compare(priority2, priority1);
        });

        return list;
    }

    /**
     * 处理请求方法
     *
     * @param request  请求实例
     * @param response 回应实例
     * @param method   请求方法实例
     * @return 结束处理后续方法
     */
    private boolean handleRequestMethod(HttpServletRequest request, HttpServletResponse response, Method method) {
        List<Object> parameterList = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            // 获取基础HTTP参数
            {
                if (parameter.getType().equals(HttpServletRequest.class)) {
                    parameterList.add(request);
                    continue;
                } else if (parameter.getType().equals(HttpServletResponse.class)) {
                    parameterList.add(response);
                    continue;
                }
            }

            Object defaultValue = null;

            // 设定默认值
            {
                DefaultValue annotation = parameter.getAnnotation(DefaultValue.class);
                if (annotation != null) {
                    defaultValue = HttpServerUtil.converter(
                            parameter.getType(),
                            annotation.value()
                    );
                }
            }

            // 获取请求数据中的数据
            {
                BodyData annotation = method.getAnnotation(BodyData.class);
                if (annotation != null) {
                    Object value = defaultValue;

                    JSONObject body = HttpServerUtil.getRequestBody(request);
                    if (body != null) {
                        String key = annotation.value();
                        value = key.equals("body") ? body : body.getObject(key, parameter.getType());
                    }

                    parameterList.add(value);
                }
            }

            // 获取cookie中的数据
            {
                if (request.getCookies() != null) {
                    CookieData annotation = parameter.getAnnotation(CookieData.class);
                    if (annotation != null) {
                        Object value = defaultValue;

                        for (Cookie cookie : request.getCookies()) {
                            if (!cookie.getName().equals(annotation.value())) continue;
                            if (cookie.getValue() == null) continue;

                            value = HttpServerUtil.converter(
                                    parameter.getType(),
                                    cookie.getValue()
                            );
                            break;
                        }

                        parameterList.add(value);
                    }
                }
            }

            // 获取请求参数中的数据
            {
                RequestParam annotation = method.getAnnotation(RequestParam.class);
                if (annotation != null) {
                    Object value = defaultValue;

                    String param = request.getParameter(annotation.value());
                    if (param != null) {
                        value = HttpServerUtil.converter(
                                parameter.getType(),
                                param
                        );
                    }

                    parameterList.add(value);
                }
            }

        }

        boolean ignoreNullParam = method.getAnnotation(IgnoreNullParam.class) != null;
        if (ignoreNullParam) {
            if (parameterList.contains(null)) return false;
        }

        try {
            Object obj = method.invoke(null, parameterList.toArray());
            if (obj instanceof Boolean) return (Boolean) obj;
        } catch (Exception e) {
            throw new RuntimeException("接口方法必须为 static", e);
        }

        return true;
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

        for (Method method : this.getRequestMethodList(path, methodPath, type)) {
            if (this.handleRequestMethod(request, response, method)) {
                return;
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.GET);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.POST);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.HEAD);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.OPTIONS);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.PUT);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.DELETE);
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) {
        this.handleRequest(request, response, RequestTypes.TRACE);
    }
}
