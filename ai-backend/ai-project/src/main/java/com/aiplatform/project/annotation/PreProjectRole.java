package com.aiplatform.project.annotation;

import com.aiplatform.common.constant.CommonConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 项目角色要求(用户必须是项目成员,且角色达到要求)。
 * 校验由 ProjectRoleAspect 完成。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreProjectRole {

    String value() default CommonConstants.PROJECT_VIEWER;
}
