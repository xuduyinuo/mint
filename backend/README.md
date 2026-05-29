# Mint 聚合搜索后端

## 启动

1. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS mint_search DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 启动服务：

```bash
mvn spring-boot:run
```

默认管理员：`admin / admin123456`  
默认普通用户：`user / user123456`

## NewAPI 配置

未配置时自动使用模拟数据兜底。

```bash
export NEWAPI_BASE_URL=https://your-newapi-host
export NEWAPI_API_KEY=your-key
export NEWAPI_MODEL=gpt-4.1-mini
```
