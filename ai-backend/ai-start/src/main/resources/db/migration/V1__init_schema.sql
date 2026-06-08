-- ============================================================
-- V1: 初始化系统表 + 项目表(共 16 张)
-- 数据库:MariaDB 12.2
-- 字符集:utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 系统管理 (10 张)
-- ============================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    username          VARCHAR(50)     NOT NULL COMMENT '用户名',
    password          VARCHAR(128)    NOT NULL COMMENT '密码(SHA-256)',
    real_name         VARCHAR(50)              COMMENT '真实姓名',
    nickname          VARCHAR(50)              COMMENT '昵称',
    avatar            VARCHAR(255)             COMMENT '头像',
    email             VARCHAR(100)             COMMENT '邮箱',
    phone             VARCHAR(20)              COMMENT '手机号',
    gender            TINYINT                  COMMENT '性别 0未知 1男 2女',
    dept_id           BIGINT                   COMMENT '部门ID',
    dept_name         VARCHAR(100)             COMMENT '部门名(冗余)',
    status            TINYINT       DEFAULT 1  COMMENT '状态 1启用 0停用',
    admin             TINYINT       DEFAULT 0  COMMENT '是否超管 1是 0否',
    last_login_time   DATETIME                 COMMENT '最后登录时间',
    last_login_ip     VARCHAR(50)              COMMENT '最后登录IP',
    remark            VARCHAR(500)             COMMENT '备注',
    create_by         VARCHAR(50)              COMMENT '创建人',
    create_time       DATETIME                 COMMENT '创建时间',
    update_by         VARCHAR(50)              COMMENT '更新人',
    update_time       DATETIME                 COMMENT '更新时间',
    deleted           TINYINT       DEFAULT 0  COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(50)     NOT NULL COMMENT '角色名',
    code          VARCHAR(50)     NOT NULL COMMENT '角色编码',
    description   VARCHAR(255)             COMMENT '描述',
    status        TINYINT        DEFAULT 1 COMMENT '状态',
    data_scope    TINYINT        DEFAULT 1 COMMENT '数据权限 1全部 2本部门 3本人',
    sort_order    INT            DEFAULT 0 COMMENT '排序',
    create_by     VARCHAR(50)             COMMENT '创建人',
    create_time   DATETIME                COMMENT '创建时间',
    update_by     VARCHAR(50)             COMMENT '更新人',
    update_time   DATETIME                COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id   BIGINT NOT NULL COMMENT '用户ID',
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user (user_id),
    KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id     BIGINT          DEFAULT 0  COMMENT '父菜单ID,0为根',
    name          VARCHAR(50)     NOT NULL   COMMENT '路由名',
    title         VARCHAR(50)     NOT NULL   COMMENT '菜单标题',
    path          VARCHAR(200)                COMMENT '路由路径',
    component     VARCHAR(200)                COMMENT '组件路径',
    icon          VARCHAR(50)                 COMMENT '图标',
    perm_code     VARCHAR(100)                COMMENT '权限标识',
    type          TINYINT          DEFAULT 1  COMMENT '类型 1目录 2菜单 3按钮',
    sort_order    INT              DEFAULT 0  COMMENT '排序',
    visible       TINYINT          DEFAULT 1  COMMENT '是否可见',
    status        TINYINT          DEFAULT 1  COMMENT '状态',
    redirect      VARCHAR(200)                COMMENT '重定向',
    remark        VARCHAR(500)                COMMENT '备注',
    create_by     VARCHAR(50)                 COMMENT '创建人',
    create_time   DATETIME                    COMMENT '创建时间',
    update_by     VARCHAR(50)                 COMMENT '更新人',
    update_time   DATETIME                    COMMENT '更新时间',
    deleted       TINYINT          DEFAULT 0  COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单';

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    menu_id   BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    permission  VARCHAR(100) NOT NULL COMMENT '权限标识',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限(细粒度)';

DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT          DEFAULT 0  COMMENT '父部门ID',
    name        VARCHAR(50)     NOT NULL   COMMENT '部门名',
    code        VARCHAR(50)                 COMMENT '部门编码',
    path        VARCHAR(255)                COMMENT '祖先路径',
    leader      VARCHAR(50)                 COMMENT '负责人',
    phone       VARCHAR(20)                 COMMENT '联系电话',
    email       VARCHAR(100)                COMMENT '邮箱',
    sort_order  INT              DEFAULT 0  COMMENT '排序',
    status      TINYINT          DEFAULT 1  COMMENT '状态',
    create_by   VARCHAR(50)                 COMMENT '创建人',
    create_time DATETIME                    COMMENT '创建时间',
    update_by   VARCHAR(50)                 COMMENT '更新人',
    update_time DATETIME                    COMMENT '更新时间',
    deleted     TINYINT          DEFAULT 0  COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

DROP TABLE IF EXISTS sys_dict;
CREATE TABLE sys_dict (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_code   VARCHAR(50)     NOT NULL COMMENT '字典类型编码',
    type_name   VARCHAR(50)     NOT NULL COMMENT '字典类型名称',
    description VARCHAR(255)             COMMENT '描述',
    status      TINYINT        DEFAULT 1 COMMENT '状态',
    create_by   VARCHAR(50)             COMMENT '创建人',
    create_time DATETIME                COMMENT '创建时间',
    update_by   VARCHAR(50)             COMMENT '更新人',
    update_time DATETIME                COMMENT '更新时间',
    deleted     TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

DROP TABLE IF EXISTS sys_dict_item;
CREATE TABLE sys_dict_item (
    id          BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    type_code   VARCHAR(50)     NOT NULL   COMMENT '字典类型编码',
    item_key    VARCHAR(50)     NOT NULL   COMMENT '字典项 key',
    item_value  VARCHAR(100)    NOT NULL   COMMENT '字典项 value',
    label       VARCHAR(100)                COMMENT '展示标签',
    color       VARCHAR(20)                 COMMENT '标签颜色',
    sort_order  INT              DEFAULT 0  COMMENT '排序',
    status      TINYINT          DEFAULT 1  COMMENT '状态',
    remark      VARCHAR(500)                COMMENT '备注',
    create_by   VARCHAR(50)                 COMMENT '创建人',
    create_time DATETIME                    COMMENT '创建时间',
    update_by   VARCHAR(50)                 COMMENT '更新人',
    update_time DATETIME                    COMMENT '更新时间',
    deleted     TINYINT          DEFAULT 0  COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项';

DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    module          VARCHAR(50)              COMMENT '模块',
    action          VARCHAR(50)              COMMENT '操作',
    method          VARCHAR(200)             COMMENT '方法',
    request_url     VARCHAR(500)             COMMENT '请求 URL',
    request_method  VARCHAR(10)              COMMENT 'HTTP 方法',
    request_params  TEXT                     COMMENT '请求参数',
    response_data   TEXT                     COMMENT '响应数据',
    user_id         BIGINT                   COMMENT '用户ID',
    username        VARCHAR(50)              COMMENT '用户名',
    ip              VARCHAR(50)              COMMENT 'IP',
    user_agent      VARCHAR(500)             COMMENT 'UA',
    cost_ms         BIGINT                   COMMENT '耗时 ms',
    status          TINYINT                  COMMENT '状态 0失败 1成功',
    error_msg       TEXT                     COMMENT '错误信息',
    create_time     DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================================
-- 项目域 (6 张)
-- ============================================================

DROP TABLE IF EXISTS ai_project;
CREATE TABLE ai_project (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name          VARCHAR(100)    NOT NULL COMMENT '项目名',
    code          VARCHAR(50)              COMMENT '项目编码(唯一)',
    description   VARCHAR(500)             COMMENT '描述',
    icon          VARCHAR(255)             COMMENT '图标',
    status        TINYINT        DEFAULT 1 COMMENT '状态',
    owner_id      BIGINT                   COMMENT '所有者用户ID',
    settings      LONGTEXT                 COMMENT '项目设置 JSON',
    create_by     VARCHAR(50)              COMMENT '创建人',
    create_time   DATETIME                 COMMENT '创建时间',
    update_by     VARCHAR(50)              COMMENT '更新人',
    update_time   DATETIME                 COMMENT '更新时间',
    deleted       TINYINT        DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

DROP TABLE IF EXISTS ai_project_member;
CREATE TABLE ai_project_member (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id  BIGINT      NOT NULL COMMENT '项目ID',
    user_id     BIGINT      NOT NULL COMMENT '用户ID',
    username    VARCHAR(50) NOT NULL COMMENT '用户名(冗余)',
    role_code   VARCHAR(20) NOT NULL COMMENT '项目角色 owner/admin/developer/viewer',
    join_time   DATETIME    NOT NULL COMMENT '加入时间',
    create_by   VARCHAR(50)          COMMENT '创建人',
    create_time DATETIME             COMMENT '创建时间',
    update_by   VARCHAR(50)          COMMENT '更新人',
    update_time DATETIME             COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_user (project_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员';

DROP TABLE IF EXISTS ai_project_api_key;
CREATE TABLE ai_project_api_key (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id      BIGINT          NOT NULL COMMENT '项目ID',
    name            VARCHAR(100)    NOT NULL COMMENT '名称',
    api_key         VARCHAR(64)     NOT NULL COMMENT 'API Key',
    api_secret      VARCHAR(128)    NOT NULL COMMENT 'API Secret',
    scopes          VARCHAR(500)             COMMENT '权限范围(JSON 数组)',
    rate_limit      INT             DEFAULT 60 COMMENT '每分钟限流',
    expires_at      BIGINT                   COMMENT '过期时间(epoch ms)',
    status          TINYINT         DEFAULT 1 COMMENT '状态 1启用 0禁用',
    last_used_time  DATETIME                 COMMENT '最后使用时间',
    last_used_ip    VARCHAR(50)              COMMENT '最后使用IP',
    create_by       VARCHAR(50)              COMMENT '创建人',
    create_time     DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_key (api_key),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目 API Key';

DROP TABLE IF EXISTS ai_project_webhook;
CREATE TABLE ai_project_webhook (
    id            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id    BIGINT          NOT NULL COMMENT '项目ID',
    name          VARCHAR(100)    NOT NULL COMMENT '名称',
    url           VARCHAR(500)    NOT NULL COMMENT '回调 URL',
    secret        VARCHAR(128)             COMMENT '签名密钥',
    events        VARCHAR(500)             COMMENT '订阅事件(逗号分隔)',
    status        TINYINT         DEFAULT 1 COMMENT '状态',
    description   VARCHAR(500)             COMMENT '描述',
    create_by     VARCHAR(50)              COMMENT '创建人',
    create_time   DATETIME                 COMMENT '创建时间',
    update_by     VARCHAR(50)              COMMENT '更新人',
    update_time   DATETIME                 COMMENT '更新时间',
    deleted       TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目 Webhook';

DROP TABLE IF EXISTS ai_project_webhook_log;
CREATE TABLE ai_project_webhook_log (
    id               BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    webhook_id       BIGINT          NOT NULL COMMENT 'Webhook ID',
    project_id       BIGINT          NOT NULL COMMENT '项目ID',
    event            VARCHAR(50)     NOT NULL COMMENT '事件',
    request_url      VARCHAR(500)             COMMENT '请求 URL',
    response_status  INT                      COMMENT '响应状态码',
    response_body    TEXT                     COMMENT '响应内容',
    request_payload  TEXT                     COMMENT '请求负载',
    retry_count      INT             DEFAULT 0 COMMENT '重试次数',
    cost_ms          BIGINT                   COMMENT '耗时 ms',
    create_time      DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_webhook (webhook_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook 投递日志';

DROP TABLE IF EXISTS ai_file;
CREATE TABLE ai_file (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(255)    NOT NULL COMMENT '存储文件名',
    original_name   VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    path            VARCHAR(500)    NOT NULL COMMENT '存储路径',
    url             VARCHAR(500)             COMMENT '访问 URL',
    content_type    VARCHAR(100)             COMMENT 'MIME',
    size            BIGINT                   COMMENT '大小(字节)',
    biz_type        VARCHAR(50)              COMMENT '业务类型(knowledge/avatar/...)',
    biz_id          BIGINT                   COMMENT '业务ID',
    project_id      BIGINT                   COMMENT '项目ID',
    uploader_id     BIGINT                   COMMENT '上传者ID',
    uploader_name   VARCHAR(50)              COMMENT '上传者',
    storage_type    VARCHAR(20)   DEFAULT 'local' COMMENT '存储类型 local/s3/oss',
    create_by       VARCHAR(50)              COMMENT '创建人',
    create_time     DATETIME                 COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件';

SET FOREIGN_KEY_CHECKS = 1;
