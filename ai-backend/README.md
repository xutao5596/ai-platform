# AI-Platform Backend

> Spring Boot 3.5 + Java 21 后端工程

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 21 LTS |
| Spring Boot | 3.5.5 |
| MyBatis Plus | 3.5.9 |
| MariaDB Driver | 3.x |
| Apache Shiro | 2.0.2 |
| JWT | 0.12.x |
| Caffeine | 3.1.8 |
| LangChain4j | 1.9.1 |
| LiteFlow | 2.15.0 |
| Apache Tika | 3.2.3 |
| Hnswlib | 0.7.1 |

## 模块结构

```
ai-backend/
├── ai-common/                # 公共工具
├── ai-framework/             # 基础框架
│   ├── shiro/                # 权限认证
│   ├── jwt/                  # JWT
│   ├── web/                  # Web 配置
│   ├── cache/                # Caffeine
│   └── log/                  # 日志切面
├── ai-system/                # 系统管理
│   └── controller/service/entity/mapper
├── ai-project/               # 项目域
│   ├── controller/
│   ├── interceptor/          # @PreProjectRole
│   └── entity/
├── ai-flow/                  # 流程引擎
│   ├── spi/                  # FlowNode 接口
│   ├── registry/             # NodeRegistry
│   ├── executor/             # LiteFlow 执行
│   ├── trigger/              # 5 种触发器
│   └── nodes/                # 12 个内置节点
├── ai-ai/                    # AI 业务
│   ├── model/                # 大模型
│   ├── mcp/                  # MCP
│   ├── knowledge/            # 知识库
│   ├── prompt/               # 提示词
│   └── chat/                 # AI 对话
├── ai-assistant/             # AI 助手
├── ai-start/                 # 启动模块
│   ├── Application.java
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/migration/     # Flyway
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- MariaDB 12.2

### 数据库初始化

```bash
mysql -u root -p < sql/00_init.sql
```

### 构建

```bash
mvn clean install -DskipTests
```

### 运行

```bash
java -jar ai-start/target/ai-start-1.0.0.jar
```

### 访问

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Druid 监控: http://localhost:8080/druid

## 配置

### application-dev.yml

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/ai_platform
    username: root
    password: root

ai:
  data:
    upload-path: /data/upload
    hnsw-path: /data/hnsw
    log-path: /data/logs
```

## 关键设计

详见 [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)

## 开发规范

- 遵循《阿里巴巴 Java 开发手册》
- 单元测试覆盖率 ≥ 50%
- 关键路径必须测试
- Commit 规范遵循 Conventional Commits

## 状态

🟡 **待开发** - Sprint 0 启动前
