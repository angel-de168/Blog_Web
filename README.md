# Blog Web（个人博客写作、随笔动态与成长可视化）

一个前后端分离的现代化个人博客平台，聚焦四件事：
- 持续写作（博客沉淀）
- 记录当下（随笔动态）
- 看见成长（趋势图谱）
- 低干扰交流（二期推进中）

## 当前状态

一期主链路已打通：
- 注册 / 登录 / 登出 / 当前用户
- 笔记列表、筛选、创建、编辑、删除
- 随笔列表、筛选、创建、编辑、删除
- 成长图谱（时间线）+ 技能雷达（标签聚合）+ 闭环分析（产出/反馈/迭代）
- 公开首页 + 登录门槛提示（未登录使用受保护功能会提示并引导登录）
- 前端视觉已完成两轮重构（暖色阅读流 + 首页海蓝沉浸式首屏）
- 首页首屏文案已居中，并移除“登录后发随笔 / 先看成长图谱”快捷按钮
- 全站页面风格已按首页收口（博客/随笔/成长/登录），导航改为“下滑隐藏、上滑恢复、顶部常显”
- 登录页沿用首页导航态（单行顶部入口保持一致）
- 登录页已改为沉浸式全屏背景 + 叠层双区结构，进入页面随机切换背景图
- 登录与注册态支持联动缩放（切换“立即注册 / 立即登录”时图片与表单同步放大 / 恢复）
- 成长图谱已完成二阶段扩维：主时间线、技能雷达、周内活跃分布、状态结构、闭环指标与闭环趋势线
- 闭环趋势线已升级为三序列折线图（产出/反馈/迭代），支持 30 / 90 / 120 天区间联动
- 成长页右侧信息卡已收口为“状态结构 + 个人成就 + 内容总量 + 创作历程（按年）”

二期社交模块当前进度：
- 已完成：随笔点赞/取消点赞、随笔评论（新增/删除）
- 已完成：随笔可见范围（公开 / 仅自己 / 圈层），并完成未登录/非主用户可见性验证
- 待开发：关注/取关、私信、低干扰提醒机制

## 技术栈

- 前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Axios + ECharts
- 后端：Spring Boot 3 + Spring Security + MyBatis-Plus
- 数据库：MySQL（开发联调支持 H2）
- 构建：npm / Maven

## 功能模块

### 1) 鉴权模块
- 用户注册
- 用户登录（Session/Cookie）
- 用户登出
- 获取当前登录用户

### 2) 博客模块
- 笔记 CRUD
- 标题/内容关键词筛选
- 按学习状态筛选（学习中 / 已掌握）
- 标签管理（逗号分隔输入）

### 3) 随笔模块
- 随笔 CRUD
- 内容/心情关键词筛选
- 随手记录感悟、心情、日记
- 互动：点赞、评论（二期已落地）

### 4) 成长图谱模块
- 成长主时间线（按日趋势）
- 技能雷达（标签聚合）
- 周内活跃分布（周一~周日）
- 状态结构（学习中 / 已掌握）
- 闭环指标（产出 / 反馈 / 迭代）
- 闭环趋势线（三序列折线图）
- 右侧信息卡：状态结构、个人成就、内容总量、创作历程（按年）

### 5) 公开首页模块
- 公开访问首页（无需登录）
- 顶部导航统一入口（首页、博客、随笔、成长图谱）
- 未登录访问受保护功能时提示“请登录后使用”并可跳转登录
- 首页“知行记录者”展示站点统计（文章、标签去重、访问量）

### 6) 站点信息模块
- 获取首页主用户资料（`GET /api/auth/main-user`）
- 获取并更新站点统计（`GET /api/site/stats`，返回文章数/标签去重数/访问量）

## 项目结构

```text
Blog_Web/
├─ frontend/                # Vue3 前端
│  ├─ src/
│  │  ├─ api/
│  │  ├─ components/
│  │  ├─ router/
│  │  ├─ stores/
│  │  ├─ views/
│  │  └─ style.css
│  └─ vite.config.ts
├─ backend/                 # Spring Boot 后端
│  ├─ src/main/java/com/blogweb/
│  │  ├─ common/
│  │  ├─ config/
│  │  └─ modules/
│  └─ src/main/resources/
│     ├─ application.yml
│     └─ db/migration/
├─ .mvn/maven.config        # Maven settings 与本地仓库配置
├─ CLAUDE.md                # 项目上下文与阶段信息
└─ TODO.md                  # 开发进度与后续计划
```

## 本地开发

### 环境要求
- Node.js 20+
- npm 10+
- JDK 21
- Maven 3.9+
- MySQL 8+

### 1) 数据库准备
先创建数据库：

```sql
CREATE DATABASE blog_web DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行初始化脚本：
- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__add_moment_table.sql`
- `backend/src/main/resources/db/migration/V3__add_moment_social_tables.sql`
- `backend/src/main/resources/db/migration/V4__add_site_counter.sql`

> 当前 `spring.sql.init.mode=never`，不会自动建表。

### 2) 启动后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

或先构建：

```bash
mvn -f backend/pom.xml clean package
```

说明：项目已在 `.mvn/maven.config` 中配置：
- `--settings=D:/Maven/apache-maven-3.9.6/conf/settings.xml`
- `-Dmaven.repo.local=D:/Maven/maven-repository`

### 3) 启动前端

```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

构建：

```bash
npm --prefix frontend run build
```

前端代理配置：`/api -> http://localhost:8080`（见 `frontend/vite.config.ts`）。

## 配置说明

后端主要配置文件：`backend/src/main/resources/application.yml`
- 数据库：
  - url: `jdbc:mysql://localhost:3306/blog_web?...`
  - username: `root`
  - password: `123456`

> 注意：`server.port` 需要是合法数字端口（例如 `8080`）。

## 开发约束（视觉）

遵循当前仓库规范：
- 禁用紫色系主视觉
- 避免对称三卡布局
- 文案口语化、短句
- 首页维持“海蓝沉浸首屏 + 波浪过渡 + 左右内容流”的结构风格

## 已知问题

- 命令行 `curl -d` 发送中文 JSON 在当前终端可能出现 UTF-8 编码异常；英文 payload 正常。
- 成长图谱页面打包后 ECharts 相关 chunk 仍偏大，后续可继续按图表模块拆包。
- Maven 全局 `settings.xml` 可能出现非标准 `profile` 标签告警，不影响构建运行。
- 成长页“个人成就”中的博客点赞当前固定展示为 0（后端暂未接入博客点赞数据源）。
- 全站视觉统一（首页 + 博客 + 随笔 + 成长 + 登录）已完成代码层改造并通过构建，仍建议做 375 / 768 / 1440 三档人工验收。

## 后续计划

- 二期社交模块：推进关注/私信
- 低干扰机制：默认弱提醒、安静时段、通知汇总
- 后台管理：用户、内容、权限
- 安全与性能加固：输入校验、权限边界、索引与慢查询
- 部署完善：MySQL 正式配置、容器化方案
