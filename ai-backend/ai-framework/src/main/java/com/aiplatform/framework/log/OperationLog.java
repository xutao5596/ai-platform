package com.aiplatform.framework.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解(切面记录到 sys_log)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String module() default "";

    String action() default "";

    boolean saveRequest() default true;

    boolean saveResponse() default false;
}
