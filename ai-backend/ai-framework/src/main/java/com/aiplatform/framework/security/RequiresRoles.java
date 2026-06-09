package com.aiplatform.framework.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统角色要求(用户必须拥有任一角色才能访问)。
 * 校验由 AuthorizationAspect 完成。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRoles {

    String[] value();

    Logical logical() default Logical.OR;
}
