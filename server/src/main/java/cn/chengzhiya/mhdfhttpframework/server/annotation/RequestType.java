package cn.chengzhiya.mhdfhttpframework.server.annotation;

import cn.chengzhiya.mhdfhttpframework.api.enums.RequestTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    RequestTypes value();
}
