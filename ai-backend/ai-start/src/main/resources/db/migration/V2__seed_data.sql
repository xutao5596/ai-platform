-- ============================================================
-- V2: 种子数据(管理员、角色、字典、菜单)
-- ============================================================

-- 超级管理员(密码:admin123)
-- PasswordUtil.hash("admin123") = SHA-256("1admin1230")
-- 计算结果:98205e0566c92e169841ecd40244ab222eb22f72ecd8e1c00445910c25a4a9c4

INSERT INTO sys_user (username, password, real_name, admin, status, create_time)
VALUES ('admin', '98205e0566c92e169841ecd40244ab222eb22f72ecd8e1c00445910c25a4a9c4', '超级管理员', 1, 1, NOW());

INSERT INTO sys_role (name, code, description, status, sort_order, create_time) VALUES
('超级管理员', 'admin', '系统超级管理员', 1, 0, NOW()),
('普通用户', 'user', '普通用户', 1, 1, NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 一级菜单(侧边栏 6 个)
INSERT INTO sys_menu (id, parent_id, name, title, path, component, icon, type, sort_order, visible, status) VALUES
(1,  0, 'Dashboard',   '工作台',   '/dashboard',   'dashboard/index',          'HomeFilled',     1, 10, 1, 1),
(2,  0, 'Project',     '我的项目', '/project',     'project/list',             'Folder',         1, 20, 1, 1),
(3,  0, 'Ai',          'AI 资源',  '/ai',          'layout',                   'MagicStick',     1, 30, 1, 1),
(4,  0, 'Monitor',     '监控',     '/monitor',     'monitor/index',            'Monitor',        1, 40, 1, 1),
(5,  0, 'System',      '系统管理', '/system',      'system/index',             'Setting',        1, 50, 1, 1),
(6,  0, 'Assistant',   'AI 助手',  '/assistant',   'assistant/index',          'ChatDotRound',   1, 60, 1, 1);

-- 系统管理子菜单
INSERT INTO sys_menu (parent_id, name, title, path, component, perm_code, type, sort_order) VALUES
(5, 'SysUser',  '用户管理', 'user',  'system/user',  'system:user:view', 2, 1),
(5, 'SysRole',  '角色管理', 'role',  'system/role',  'system:role:view', 2, 2),
(5, 'SysMenu',  '菜单管理', 'menu',  'system/menu',  'system:menu:view', 2, 3),
(5, 'SysDept',  '部门管理', 'dept',  'system/dept',  'system:dept:view', 2, 4),
(5, 'SysDict',  '字典管理', 'dict',  'system/dict',  'system:dict:view', 2, 5),
(5, 'SysLog',   '操作日志', 'log',   'system/log',   'system:log:view',  2, 6);

-- AI 资源子菜单
INSERT INTO sys_menu (parent_id, name, title, path, perm_code, type, sort_order) VALUES
(3, 'ModelList',    '模型管理',  'model',     'ai:model:view',     2, 1),
(3, 'McpList',      'MCP 服务',  'mcp',       'ai:mcp:view',       2, 2),
(3, 'KnowledgeList','知识库',    'knowledge', 'ai:knowledge:view', 2, 3),
(3, 'PromptList',   '提示词',    'prompt',    'ai:prompt:view',    2, 4);

-- 权限按钮(操作类,绑定 perm_code)
INSERT INTO sys_menu (parent_id, name, title, perm_code, type, sort_order) VALUES
((SELECT id FROM (SELECT id FROM sys_menu WHERE name='SysUser' LIMIT 1) t), 'SysUserAdd', '新增', 'system:user:add', 3, 1),
((SELECT id FROM (SELECT id FROM sys_menu WHERE name='SysUser' LIMIT 1) t), 'SysUserEdit', '编辑', 'system:user:edit', 3, 2),
((SELECT id FROM (SELECT id FROM sys_menu WHERE name='SysUser' LIMIT 1) t), 'SysUserDelete', '删除', 'system:user:delete', 3, 3),
((SELECT id FROM (SELECT id FROM sys_menu WHERE name='SysUser' LIMIT 1) t), 'SysUserResetPwd', '重置密码', 'system:user:reset-password', 3, 4);

-- 角色绑定所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission)
SELECT 1, CONCAT('system:', p) FROM (
    SELECT 'user:view' AS p UNION ALL SELECT 'user:add' UNION ALL SELECT 'user:edit' UNION ALL SELECT 'user:delete' UNION ALL SELECT 'user:reset-password'
    UNION ALL SELECT 'role:view' UNION ALL SELECT 'role:add' UNION ALL SELECT 'role:edit' UNION ALL SELECT 'role:delete'
    UNION ALL SELECT 'menu:view' UNION ALL SELECT 'menu:add' UNION ALL SELECT 'menu:edit' UNION ALL SELECT 'menu:delete'
    UNION ALL SELECT 'dept:view' UNION ALL SELECT 'dept:add' UNION ALL SELECT 'dept:edit' UNION ALL SELECT 'dept:delete'
    UNION ALL SELECT 'dict:view' UNION ALL SELECT 'dict:add' UNION ALL SELECT 'dict:edit' UNION ALL SELECT 'dict:delete'
    UNION ALL SELECT 'log:view' UNION ALL SELECT 'log:delete'
) t;

INSERT INTO sys_role_permission (role_id, permission) VALUES (1, 'project:create');

-- 字典:用户状态
INSERT INTO sys_dict (type_code, type_name, description, status, create_time) VALUES
('user_status', '用户状态', '用户启用/禁用', 1, NOW()),
('user_gender', '用户性别', '0未知 1男 2女', 1, NOW()),
('project_role', '项目角色', 'owner/admin/developer/viewer', 1, NOW()),
('flow_status', '流程状态', 'draft/published/archived', 1, NOW()),
('node_type', '节点类型', 'start/end/llm/...', 1, NOW()),
('trigger_type', '触发器类型', 'manual/cron/webhook/event/chained', 1, NOW()),
('model_provider', '模型厂商', 'openai/deepseek/claude/...', 1, NOW());

INSERT INTO sys_dict_item (type_code, item_key, item_value, label, sort_order, status) VALUES
('user_status', '1', '1', '启用', 1, 1),
('user_status', '0', '0', '禁用', 2, 1),
('user_gender', '0', '0', '未知', 1, 1),
('user_gender', '1', '1', '男',   2, 1),
('user_gender', '2', '2', '女',   3, 1),
('project_role', 'owner',     'owner',     '所有者',   4, 1),
('project_role', 'admin',     'admin',     '管理员',   3, 1),
('project_role', 'developer', 'developer', '开发者',   2, 1),
('project_role', 'viewer',    'viewer',    '观察者',   1, 1),
('flow_status', 'draft',     'draft',     '草稿',     1, 1),
('flow_status', 'published', 'published', '已发布',  2, 1),
('flow_status', 'archived',  'archived',  '已归档',  3, 1),
('trigger_type', 'manual',  'manual',  '手动',  1, 1),
('trigger_type', 'cron',    'cron',    '定时',  2, 1),
('trigger_type', 'webhook', 'webhook', '回调',  3, 1),
('trigger_type', 'event',   'event',   '事件',  4, 1),
('trigger_type', 'chained', 'chained', '链式',  5, 1),
('model_provider', 'openai',   'openai',   'OpenAI',     1, 1),
('model_provider', 'deepseek', 'deepseek', 'DeepSeek',   2, 1),
('model_provider', 'claude',   'claude',   'Claude',     3, 1),
('model_provider', 'qwen',     'qwen',     '通义千问',   4, 1),
('model_provider', 'glm',      'glm',      '智谱 GLM',   5, 1),
('model_provider', 'ollama',   'ollama',   'Ollama',     6, 1);
