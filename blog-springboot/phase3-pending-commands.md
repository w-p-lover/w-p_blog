# Phase 3 待执行命令清单

> 适用目录：`blog-springboot`

## 1) 基础检查

```bash
git branch --all
git worktree list
git status
```

## 2) （可选）切到本次开发分支

如果你当前不在 `worktree-phase3-ai-core`：

```bash
git switch worktree-phase3-ai-core
```

## 3) 编译与定向测试

```bash
mvn -q -DskipTests compile
mvn -Dtest=AiArticleServiceImplTest test
mvn -Dtest=AiRagServiceImplTest test
mvn -Dtest=AiControllerTest test
```

## 4) 全量测试与打包

```bash
mvn test
mvn -q -DskipTests package
```

## 5) 本地启动（后端）

```bash
mvn spring-boot:run
```

## 6) 接口验收

### 6.1 RAG 问答

```bash
curl -X POST "http://localhost:8080/api/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"Redis缓存击穿怎么解决"}'
```

### 6.2 SSE 写作助手

```bash
curl -N "http://localhost:8080/api/ai/write-assist?action=expand&content=缓存优化实践"
```

## 7) Qdrant 连通性

```bash
curl "http://121.41.87.40:6333/collections"
```

## 8) 若需提交本次改动

```bash
git add \
  src/main/java/com/ican/config/AiVectorStoreConfig.java \
  src/main/java/com/ican/controller/AiController.java \
  src/main/java/com/ican/service/AiWriteAssistService.java \
  src/main/java/com/ican/service/impl/AiWriteAssistServiceImpl.java \
  src/test/java/com/ican/controller/AiControllerTest.java \
  src/test/java/com/ican/SmokeTest.java \
  src/test/resources/application-test.yml \
  src/main/java/com/ican/service/impl/AiRagServiceImpl.java \
  src/test/java/com/ican/service/impl/AiRagServiceImplTest.java

git commit -m "feat: 完成 Phase3 AI 核心能力（RAG + SSE写作助手 + 向量健康检查）"
```
