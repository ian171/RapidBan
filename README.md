# RapidBan - 企业级 Minecraft 惩罚管理系统

适用于 Paper 1.21+ 的高性能、分布式惩罚管理插件。

## 核心特性

### 🚀 高性能架构
- **异步数据库操作** - 使用 HikariCP 连接池，所有 I/O 操作异步执行
- **线程安全设计** - 独立线程池处理数据库请求，不影响服务器 TPS
- **本地缓存** - 活跃封禁缓存，减少数据库查询

### 🌐 分布式同步
- **多服务器支持** - 多个服务器共享同一数据库
- **实时同步** - 封禁/解封操作在所有服务器即时生效
- **Redis 支持** - 可选 Redis Pub/Sub 或数据库轮询

### 🎯 UUID 封禁系统
- 基于 UUID 而非玩家名
- 支持永久封禁、临时封禁、静默封禁
- 自动到期解封
- 完整的撤销系统

### 🔍 IP 关联检测
- 记录玩家历史登录 IP
- 自动检测关联账号
- 通知管理员可疑登录
- 可选自动连带封禁

### 🎨 自定义踢出界面
- 支持 MiniMessage 格式
- 变量占位符（玩家名、原因、剩余时间等）
- 不同处罚类型显示不同模板

### 📊 Web 管理面板
- REST API 接口
- JWT 身份验证
- 查看处罚历史
- 执行封禁/解封操作
- 搜索玩家和关联账号

## 安装

1. 下载 `RapidBan-1.0-Beta1.jar`
2. 放入服务器 `plugins` 目录
3. 启动服务器生成配置文件
4. 编辑 `config.yml` 选择数据库类型：
   - **SQLite** - 适合单服务器，无需额外配置
   - **MySQL** - 适合多服务器网络，需配置数据库连接
5. 重启服务器

### 数据库选择

#### SQLite（推荐单服务器）
- ✅ 无需额外安装数据库
- ✅ 配置简单，开箱即用
- ✅ 适合小型服务器
- ❌ 不支持多服务器同步

配置示例：
```yaml
database:
  type: "SQLITE"
  sqlite:
    file: "rapidban.db"
```

#### MySQL/MariaDB（推荐多服务器）
- ✅ 支持多服务器分布式同步
- ✅ 高性能，适合大型网络
- ✅ 数据集中管理
- ❌ 需要独立数据库服务器

配置示例：
```yaml
database:
  type: "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "rapidban"
    username: "root"
    password: "password"
```

## 配置

### config.yml

```yaml
# 服务器标识（用于分布式同步）
server-id: "server-1"

# 数据库配置
database:
  # 数据库类型: MYSQL 或 SQLITE
  type: "MYSQL"

  # MySQL/MariaDB 配置
  mysql:
    host: "localhost"
    port: 3306
    database: "rapidban"
    username: "root"
    password: "password"

  # SQLite 配置（文件路径相对于插件数据文件夹）
  sqlite:
    file: "rapidban.db"

# 同步系统配置
sync:
  interval-seconds: 5
  use-redis: false
  redis:
    host: "localhost"
    port: 6379
    password: ""

# IP 关联检测
ip-check:
  enabled: true
  auto-ban-alts: false
  notify-staff: true

# Web 管理面板
web:
  enabled: true
  host: "0.0.0.0"
  port: 8080
  jwt-secret: "change-this-to-a-random-secret-key"
  admin-username: "admin"
  admin-password: "admin123"
```

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/ban <玩家> <原因> [-t <时长>] [-s]` | 封禁玩家 | `rapidban.ban` |
| `/unban <玩家>` | 解封玩家 | `rapidban.unban` |
| `/history <玩家>` | 查看处罚历史 | `rapidban.history` |
| `/punishundo <玩家> [原因]` | 撤销所有处罚 | `rapidban.undo` |

### 时长格式
- `s` - 秒
- `m` - 分钟
- `h` - 小时
- `d` - 天
- `w` - 周
- `M` - 月
- `y` - 年

示例：
```
/ban Player123 作弊 -t 7d
/ban Player456 辱骂 -t 3h -s
/ban Hacker999 使用外挂
```

## 权限

| 权限 | 描述 |
|------|------|
| `rapidban.*` | 所有权限 |
| `rapidban.ban` | 封禁玩家 |
| `rapidban.unban` | 解封玩家 |
| `rapidban.history` | 查看历史 |
| `rapidban.undo` | 撤销处罚 |
| `rapidban.notify` | 接收处罚通知 |
| `rapidban.notify.alt` | 接收关联账号通知 |

## Web API

### 认证

```bash
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

返回：
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

### API 端点

所有 API 请求需要在 Header 中包含：
```
Authorization: Bearer <token>
```

#### 查看处罚历史
```bash
GET /api/punishments/history/:player
```

#### 封禁玩家
```bash
POST /api/punishments/ban
Content-Type: application/json

{
  "player": "Player123",
  "reason": "作弊",
  "duration": 604800000,
  "silent": false
}
```

#### 解封玩家
```bash
POST /api/punishments/unban
Content-Type: application/json

{
  "player": "Player123"
}
```

#### 撤销处罚
```bash
POST /api/punishments/revoke
Content-Type: application/json

{
  "player": "Player123",
  "reason": "误封"
}
```

## 数据库

插件支持两种数据库类型：

### SQLite
- 自动创建 `rapidban.db` 文件
- 适合单服务器部署
- 无需额外配置

### MySQL/MariaDB
- 自动创建以下表：
  - `rb_players` - 玩家信息
  - `rb_punishments` - 处罚记录
  - `rb_ip_history` - IP 登录历史
  - `rb_sync_events` - 同步事件
  - `rb_web_tokens` - Web 用户
  - `rb_audit_log` - 审计日志
- 支持多服务器分布式同步

## 性能优化

- ✅ 所有数据库操作异步执行
- ✅ HikariCP 连接池优化
- ✅ 活跃封禁本地缓存
- ✅ 独立线程池处理 I/O
- ✅ 批量查询优化
- ✅ 自动清理过期数据

## 技术栈

- **Paper API** 1.21+
- **HikariCP** - 数据库连接池
- **MariaDB/MySQL** - 关系型数据库（可选）
- **SQLite** - 嵌入式数据库（可选）
- **Jedis** - Redis 客户端
- **Javalin** - Web 框架
- **JWT** - 身份验证
- **Gson** - JSON 处理

## 开发

```bash
# 克隆项目
git clone <repository>

# 构建
./gradlew shadowJar

# 输出位置
build/libs/RapidBan-1.0-Beta1.jar
```

## 许可证

本项目仅供学习和研究使用。

## 支持

如有问题或建议，请提交 Issue。
