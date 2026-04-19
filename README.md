# AI Code Mother - 零代码应用生成平台

> 通过自然语言描述，快速生成可部署的网站应用

## 项目简介

AI Code Mother 是一个全栈零代码应用生成平台，灵感来源于 [base44](https://base44.com/) 等低代码平台。用户只需通过自然语言描述需求，系统即可自动生成完整的网站应用。

**当前状态**：项目处于骨架搭建阶段，基础架构正在完善中。

## 技术栈

### 后端
- **框架**：Spring Boot 3.5.13
- **语言**：Java 21
- **数据库**：MySQL
- **API 文档**：Knife4j (OpenAPI 3)
- **工具库**：Hutool

### 前端
- **框架**：Vue 3.5
- **语言**：TypeScript
- **构建工具**：Vite 7
- **UI 组件库**：Ant Design Vue 4
- **状态管理**：Pinia 3
- **路由**：Vue Router 4

## 项目结构

```
ai-code-mother/
├── src/                          # 后端源码
│   └── main/java/com/ai/...
├── ai-code-mother-frontend/      # 前端项目
│   ├── src/
│   │   ├── api/                  # API 接口
│   │   ├── components/           # 公共组件
│   │   ├── layouts/              # 布局组件
│   │   ├── pages/                # 页面
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # Pinia 状态管理
│   │   └── views/                # 视图组件
│   └── package.json
├── pom.xml                       # Maven 配置
└── README.md
```

## 开发环境

### 要求
- JDK 21+
- Node.js 18+
- MySQL 8.0+

### 启动后端
```bash
./mvnw spring-boot:run
```

### 启动前端
```bash
cd ai-code-mother-frontend
npm install
npm run dev
```

## 路线图

- [x] 项目初始化
- [x] 前端基础布局搭建
- [ ] AI 代码生成核心功能
- [ ] 可视化编辑器
- [ ] 应用部署与管理

## 贡献

项目处于早期开发阶段，欢迎提出建议和需求。

---

*更多详细文档将在项目迭代过程中补充完善。*
