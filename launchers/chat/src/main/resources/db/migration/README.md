# SuperSonic 数据库迁移指南

## 目录结构

```
db/migration/
├── README.md
├── mysql/                      # MySQL 专用迁移脚本
│   ├── V0__baseline_schema.sql
│   ├── V1__multi_tenant.sql
│   ├── V2__rbac_permission.sql
│   ├── V3__oauth_session.sql
│   ├── V4__init_data.sql
│   └── V5__dataset_permission.sql
└── postgresql/                 # PostgreSQL 专用迁移脚本
    ├── V0__baseline_schema.sql
    ├── V1__multi_tenant.sql
    ├── V2__rbac_permission.sql
    ├── V3__oauth_session.sql
    ├── V4__init_data.sql
    └── V5__dataset_permission.sql
```

## 迁移脚本说明

| 版本 | 文件名 | 说明 |
|-----|--------|------|
| V0 | V0__baseline_schema.sql | 基础表结构（核心业务表） |
| V1 | V1__multi_tenant.sql | 多租户支持（租户表、订阅表、tenant_id字段） |
| V2 | V2__rbac_permission.sql | RBAC权限管理（角色、权限、角色权限映射表） |
| V3 | V3__oauth_session.sql | OAuth认证与会话管理（OAuth提供者、状态、令牌、会话表） |
| V4 | V4__init_data.sql | 初始化数据（默认租户、角色、权限等基础数据） |
| V5 | V5__dataset_permission.sql | 数据集权限（viewer/view_org/is_open字段、权限组表） |

## MySQL vs PostgreSQL 语法差异

| 特性 | MySQL | PostgreSQL |
|-----|-------|------------|
| 自增主键 | `AUTO_INCREMENT` | `SERIAL` / `BIGSERIAL` |
| 布尔类型 | `TINYINT` | `SMALLINT` / `BOOLEAN` |
| 大文本 | `MEDIUMTEXT` | `TEXT` |
| 冲突处理 | `ON DUPLICATE KEY UPDATE` | `ON CONFLICT ... DO NOTHING/UPDATE` |
| 条件DDL | `IF NOT EXISTS` (部分支持) | `DO $$ BEGIN ... END $$` |

## 部署场景

### 场景1：全新部署（空数据库）

```bash
# 默认配置，Flyway 从 V0 开始执行所有迁移
export S2_DB_TYPE=mysql
export S2_DB_HOST=localhost
export S2_DB_PORT=3306
export S2_DB_DATABASE=supersonic
export S2_DB_USER=root
export S2_DB_PASSWORD=your_password

java -jar supersonic.jar
```

执行顺序：V0 → V1 → V2 → V3 → V4 → V5

### 场景2：已有数据库升级

对于已通过 `schema-mysql.sql` 初始化的旧数据库：

```bash
# 设置基线版本为 1（跳过 V0 和 V1，因为已存在）
export FLYWAY_BASELINE_VERSION=1

# 或者如果只是升级到 RBAC
export FLYWAY_BASELINE_VERSION=2

java -jar supersonic.jar
```

### 场景3：H2 开发环境

H2 使用内存数据库，Flyway 已禁用。每次启动时通过以下文件初始化：
- `schema-h2.sql` - 表结构
- `data-h2.sql` - 初始数据

## 迁移脚本编写规范

1. **命名规范**：`V{版本号}__{描述}.sql`
   - 版本号：整数，递增
   - 描述：下划线分隔的英文描述
   - 示例：`V6__add_audit_log.sql`

2. **幂等性**：使用 `IF NOT EXISTS` / `IF EXISTS` 确保可重复执行

3. **向后兼容**：
   - 添加字段时使用 `DEFAULT` 值
   - 不要删除正在使用的字段
   - 重命名字段时先添加新字段，迁移数据，再删除旧字段

4. **数据迁移**：
   - 大数据量迁移考虑分批处理
   - 使用 `ON DUPLICATE KEY UPDATE`（MySQL）确保幂等

## 常用命令

```bash
# 查看迁移状态
mvn flyway:info

# 手动执行迁移
mvn flyway:migrate

# 修复迁移历史（慎用）
mvn flyway:repair

# 清空数据库（仅开发环境！）
mvn flyway:clean
```

## 回滚策略

Flyway 社区版不支持自动回滚。回滚方案：

1. **手动回滚脚本**：为每个迁移编写对应的回滚 SQL
2. **数据库备份**：生产环境迁移前务必备份
3. **版本回退**：部署旧版本应用 + 手动执行回滚 SQL
