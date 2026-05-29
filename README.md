# Mint 聚合搜索平台

前后端分离可运行原型：

- `backend/`：Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT
- `frontend/`：Vue 3 + Element Plus + Pinia + Axios

## 快速启动

```sql
CREATE DATABASE IF NOT EXISTS mint_search DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

默认账号：

- 管理员：`admin / admin123456`
- 普通用户：`user / user123456`

NewAPI 未配置时会自动使用模拟数据兜底。
