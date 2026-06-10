# AI-Platform 用户手册

> 适用版本:v0.6.0(Sprint 4)
> 读者角色:管理员、开发者、最终用户
> 配套文档:[部署文档](./DEPLOY.md)、[API 契约](./api/sprint4-contracts.md)

## 1. 简介

AI-Platform 是一站式 AI 流程自动化平台,核心理念是**以"项目"为顶层容器**。在项目里你可以:

- 通过拖拽式可视化编辑器构建 AI 流程(LLM、知识库检索、HTTP、IF 条件分支……)
- 创建多个 AI 助手,让其自动使用工具、调用子流程、检索知识库
- 接入外部系统:API Key 鉴权(对外提供受控能力)、Webhook 接收外部事件
- 多人协作:4 级角色权限(Owner / Admin / Developer / Viewer)

平台后端采用 Spring Boot 3 + Java 21 + MariaDB + Hnswlib(向量库),前端使用 Vue 3 + Element Plus。部署只需 JDK 21 + MariaDB + Nginx,无需 Redis / PostgreSQL / 微服务框架。

本文档面向使用本平台完成日常工作的用户,从初始化到 API 对外开放,逐章讲解。

## 2. 快速开始

### 2.1 初始化 admin 账号

部署完成后,数据库 Flyway 迁移会注入一个默认超级管理员:

| 字段 | 值 |
| --- | --- |
| 用户名 | `admin` |
| 密码 | `admin123` |
| 角色 | 超管(`admin=1`),拥有所有权限 |

**首次登录步骤**:

1. 浏览器访问 `http://<服务器 IP>/`(由 nginx 反代)或 `http://<IP>:8080/` 直连后端
2. 使用 `admin / admin123` 登录
3. 登录后跳转到**工作台** Dashboard

**强烈建议**:登录后立即进入 **系统管理 → 用户管理** 修改 admin 的初始密码,并配置邮箱以便找回。

### 2.2 创建项目

1. 左侧导航点击 **我的项目** → 右上角 **新建项目**
2. 填写:
   - **项目名称**(必填,2-50 字符)
   - **项目编码**(英文字母数字,用于 URL,不可重复)
   - **描述**(选填)
3. 创建后默认角色为 **Owner**(最高权限),可管理项目所有资源

### 2.3 添加成员

1. 进入项目 → 顶部 **成员** Tab
2. 点击 **添加成员**
3. 输入用户名 → 选择角色(Owner/Admin/Developer/Viewer)→ 提交

**角色权限速查**:

| 角色 | 成员管理 | 流程 CRUD | 流程发布/运行 | API Key | Webhook | 删除项目 |
| --- | --- | --- | --- | --- | --- | --- |
| Owner(4) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Admin(3) | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Developer(2) | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Viewer(1) | ❌ | ❌ | ✅(只读+运行) | ❌ | ❌ | ❌ |

## 3. 流程编排

### 3.1 创建流程

1. 进入项目 → 左侧菜单 **流程**
2. 右上 **新建流程** → 输入名称 / 描述 / 触发器类型 → 提交
3. 自动跳转到 **流程编辑器**(5 个 Tab:**设计 / 调试 / 运行 / 监控 / 设置**)

### 3.2 节点类型说明

平台内置 **12 类节点**,按用途分 4 组:

#### 基础节点

- **开始(start)**:流程入口,所有流程必须以它开始。`input` 字段接收外部参数。
- **结束(end)**:流程出口,可配置返回结构。

#### AI 节点

- **LLM 调用(llm)**:调用大模型,支持 OpenAI 兼容协议(DeepSeek、通义、智谱、Claude、Ollama)。
  - 属性:模型 ID、提示词 ID、temperature、maxTokens、messages(支持引用变量)
- **知识库检索(knowledge_search)**:从项目知识库中检索相关 chunk。
  - 属性:知识库 ID、TopK、相似度阈值、Embedding 模型
- **提示词模板(prompt)**:将变量注入模板生成最终 prompt。
  - 属性:提示词 ID、版本号、输入变量映射
- **Agent 决策(agent)**:让 LLM 自主决定下一步动作(Function Calling)。
  - 属性:模型 ID、可用工具列表、最大迭代轮数

#### 控制节点

- **条件分支(if_else)**:根据表达式结果分叉。
  - 属性:条件表达式(支持 `{{var}}` 引用)
- **子流程调用(subflow)**:调用同一项目内其他已发布流程。

#### 工具节点

- **HTTP 请求(http)**:调用外部 HTTP 接口。
  - 属性:URL、方法、Headers、Body、超时、鉴权方式
- **MCP 工具(mcp_tool)**:调用已注册的 MCP 服务。
- **脚本执行(script)**:运行 JS / Python 脚本做轻量数据处理。
- **变量赋值(set_var)**:把表达式结果写入流程变量。

### 3.3 触发器配置

在流程编辑器的 **设置** Tab 中可启用 5 种触发器,任选其一或多选:

| 触发器 | 用途 | 配置项 |
| --- | --- | --- |
| **Manual** | 用户在前端点击"运行" | 无需配置 |
| **Cron** | 定时调度 | Cron 表达式(例:`0 0/5 * * * ?` 每 5 分钟) |
| **Webhook** | 外部系统 HTTP 触发 | 自动生成 token URL:`/api/v1/webhook/flow/{token}` |
| **Event** | 内部应用事件 | 事件类型(例:`flow.run.success`) |
| **Chained** | 被其他流程调用 | 暴露为子流程 ID |

### 3.4 调试与试运行

1. 切换到 **调试** Tab
2. 填入输入参数(JSON 格式)
3. 点击 **单步执行** / **全速执行**
4. 实时查看每个节点的输入、输出、耗时

### 3.5 发布版本

1. 切换到 **设置** Tab → 点击 **发布新版本**
2. 输入版本号(语义化版本,如 `1.0.0`)+ 发布说明
3. 提交后该版本不可再编辑;如需修改,请发布新版本

## 4. 助手使用

### 4.1 启用工具

助手默认是无工具的"纯聊天"状态,需要手动启用工具:

1. 进入项目 → **AI 资源 → 助手**
2. 选择/创建助手 → **工具** Tab
3. 勾选需要启用的工具(可多选)

平台内置 8 个工具:

| 工具 | 用途 |
| --- | --- |
| `http_request` | 调外部 HTTP |
| `run_flow` | 调用项目内子流程 |
| `search_kb` | 检索知识库 |
| `list_projects` | 列出当前用户可见项目 |
| `project_members` | 查看项目成员 |
| `current_time` | 获取当前时间(支持时区) |
| `calculator` | 数学表达式求值 |
| `code_run` | 在沙箱里执行 Python 代码 |

### 4.2 知识库关联

1. 在助手 **知识库** Tab 添加需要关联的知识库(可多个)
2. 助手在对话时会自动从这些知识库中检索上下文
3. 可调整 TopK、相似度阈值控制召回质量

### 4.3 对话入口

助手支持 3 种入口:

- **全局悬浮按钮**:任意页面右下角,可选择项目内任一助手
- **项目内助手列表**:`/project/:id/assistant`
- **全屏对话页**:`/assistant/:id`(适合深度任务)

## 5. API Key 使用

> 适用场景:把平台能力开放给外部系统或第三方应用,使用 API Key 而非用户名密码

### 5.1 创建 Key

1. 进入项目 → **API Key** Tab
2. 点击 **新建 API Key**:
   - **名称**:方便识别用途(例:"账单系统")
   - **权限范围(scopes)**:可多选(`flow:read` / `flow:run` / `assistant:chat`)
   - **速率限制**:每分钟请求数(默认 60)
   - **过期时间**:可选,留空表示永不过期
3. 创建后**仅此一次**返回明文 `apiKey` 和 `apiSecret`,请立即保存到安全的地方

### 5.2 curl 调用示例

```bash
# 1. 列出项目流程
curl -X GET "http://api.example.com/api/v1/ext/flow/list?projectId=1" \
  -H "X-API-Key: ak_live_xxxxxxxx" \
  -H "X-API-Secret: sk_live_yyyyyyyy"

# 2. 触发流程
curl -X POST "http://api.example.com/api/v1/ext/flow/1/run" \
  -H "X-API-Key: ak_live_xxxxxxxx" \
  -H "X-API-Secret: sk_live_yyyyyyyy" \
  -H "Content-Type: application/json" \
  -d '{"input": {"user": "alice"}}'

# 3. 助手对话
curl -X POST "http://api.example.com/api/v1/ext/assistant/1/chat" \
  -H "X-API-Key: ak_live_xxxxxxxx" \
  -H "X-API-Secret: sk_live_yyyyyyyy" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好,1+1=?", "sessionId": "session-1"}'
```

也可使用合并写法 `Authorization: ApiKey <key>:<secret>`。

### 5.3 重置与撤销

- **重置 Secret**:`PUT /apikeys/{id}/reset` → 生成新 secret,旧的立即失效
- **撤销 Key**:`DELETE /apikeys/{id}` → 软删除,后续请求 401

## 6. Webhook 配置

> 适用场景:订阅平台内部事件,事件触发时主动推送到你的服务端

### 6.1 创建 Webhook

1. 进入项目 → **Webhook** Tab
2. 点击 **新建 Webhook**:
   - **名称**:用于识别
   - **URL**:接收端点(必须 HTTPS)
   - **事件类型**:多选
   - **描述**:备注用途

### 6.2 事件类型

平台支持以下事件订阅:

| 事件 | 触发时机 |
| --- | --- |
| `flow.run.success` | 流程执行成功 |
| `flow.run.failed` | 流程执行失败 |
| `assistant.chat.completed` | 助手对话完成 |
| `kb.doc.indexed` | 知识库文档索引完成 |

### 6.3 签名验证

每次推送包含以下头:

| Header | 说明 |
| --- | --- |
| `X-Webhook-Signature` | `sha256=<HMAC-SHA256(secret, body)>` |
| `X-Webhook-Timestamp` | Unix 秒,5 分钟内有效(防重放) |
| `X-Webhook-Event` | 事件类型 |

**验证示例(Node.js)**:

```javascript
const crypto = require('crypto');

function verifyWebhook(req, secret) {
  const signature = req.headers['x-webhook-signature'].replace('sha256=', '');
  const timestamp  = req.headers['x-webhook-timestamp'];
  const body       = req.rawBody;

  // 1. 检查时间戳(5 分钟内)
  const now = Math.floor(Date.now() / 1000);
  if (Math.abs(now - Number(timestamp)) > 300) {
    throw new Error('时间戳过期');
  }

  // 2. 校验签名
  const expected = crypto
    .createHmac('sha256', secret)
    .update(timestamp + '.' + body)
    .digest('hex');

  if (signature !== expected) {
    throw new Error('签名错误');
  }

  return JSON.parse(body);
}
```

### 6.4 重试策略

平台推送失败时会自动重试,策略:

- 失败后:1s → 5s → 30s → 5min(最多 4 次)
- 成功或最终失败都会写入 `ai_project_webhook_log`
- 在 Webhook 详情页可查看每次投递的请求/响应/耗时

### 6.5 测试推送

创建后点击 **发送测试**,平台会立即用一条假数据向你的 URL 推送,方便联调签名验证代码。

## 7. 常见问题 FAQ

### 7.1 登录

**Q: 忘记 admin 密码怎么办?**
A: 直接重置数据库。SSH 到服务器执行:
```bash
mysql -uroot -p ai_platform
> UPDATE sys_user SET password = SHA2('newpassword', 256) WHERE username = 'admin';
```

**Q: 登录后 401 跳回登录页?**
A: 浏览器 localStorage 里的 token 过期,清掉重新登录;或检查服务端时间是否与客户端一致(JWT 校验依赖时间)。

### 7.2 流程执行

**Q: 流程运行报"找不到开始节点"?**
A: 编辑器里检查是否画了开始节点,以及是否有从开始节点到结束节点的边。

**Q: LLM 节点一直转圈?**
A: 检查 **AI 资源 → 模型** 中对应模型是否配置了正确的 `apiKey` 和 `apiBase`;外网不通时改用本地 Ollama。

**Q: 流程运行成功了,但返回 500?**
A: 看后端日志 `journalctl -u ai-platform -n 200`,通常能在异常堆栈里找到具体哪个节点报错。

### 7.3 知识库

**Q: 上传文档后检索不到?**
A: 知识库索引是异步的(默认 30s 一次),等一会儿再试;或检查 **AI 资源 → 知识库 → 文档状态**,应显示为 `indexed`。

**Q: 检索结果不相关?**
A: 1) 切换到更合适的 Embedding 模型;2) 调整 chunk 切分策略;3) 提高 TopK 然后再 rerank。

### 7.4 API Key / Webhook

**Q: API Key 调用返回 401?**
A: 1) 检查请求头是否带了 `X-API-Key` 和 `X-API-Secret`;2) 确认 Key 状态是 `enabled`;3) 确认 Key 的 `projectId` 与 URL 中的 `projectId` 一致。

**Q: API Key 调用返回 429?**
A: 触发限流。短期:等待 1 分钟;长期:联系管理员调高 `rate_limit`。

**Q: Webhook 收不到回调?**
A: 1) 你的服务端必须是 HTTPS;2) 公网可访问;3) 防火墙放行平台出口 IP;4) 查看 Webhook **投递日志** 找具体错误。

### 7.5 性能

**Q: 后端响应慢?**
A: 1) 看 `monitoring` 页面是否数据库连接打满;2) 检查 LLM 厂商是否限流;3) 看是否有大文件上传/解析任务占用资源。

**Q: 想看实时 QPS / P95?**
A: 进入 **监控** 页面,或使用 `curl http://<host>:8080/actuator/metrics`(需开启 metrics 端点)。

### 7.6 升级

**Q: 升级后前端白屏?**
A: Vite 产物名带 hash,强刷(`Ctrl+Shift+R`)或清浏览器缓存;确认 nginx 指向的 `frontend` 目录已被新版本覆盖。

**Q: 升级后 Flyway 报错?**
A: 不要手动改库,直接看错误信息里的 migration 脚本号,联系运维处理。
