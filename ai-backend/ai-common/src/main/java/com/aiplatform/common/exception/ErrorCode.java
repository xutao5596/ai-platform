package com.aiplatform.common.exception;

import lombok.Getter;

/**
 * 错误码枚举。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),

    INTERNAL_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),
    USER_EXISTS(1004, "用户名已存在"),

    ROLE_NOT_FOUND(2001, "角色不存在"),
    ROLE_IN_USE(2002, "角色已被使用,无法删除"),

    MENU_NOT_FOUND(3001, "菜单不存在"),

    DEPT_NOT_FOUND(4001, "部门不存在"),

    PROJECT_NOT_FOUND(5001, "项目不存在"),
    PROJECT_NO_PERMISSION(5002, "无项目访问权限"),
    PROJECT_MEMBER_EXISTS(5003, "用户已是项目成员"),

    FLOW_NOT_FOUND(6001, "流程不存在"),

    KNOWLEDGE_NOT_FOUND(7001, "知识库不存在"),

    FILE_UPLOAD_ERROR(8001, "文件上传失败"),
    FILE_TYPE_INVALID(8002, "不支持的文件类型");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
