# Docker Compose 网络与增量更新排查记录

本文记录 2026-05-18 在服务器更新 Idea Island 后端时遇到的 Compose 网络、容器冲突和数据卷提示问题，供后续部署排查使用。

## 背景

服务器上曾经在不同目录启动过两套 Compose 项目，例如：

- `/root/idea-island`
- `/root/idea-island-backend`

虽然容器名都叫 `idea-island-app`、`idea-island-mysql`、`idea-island-redis`，但 Compose 会根据项目名创建不同的默认网络：

- `idea-island_default`
- `idea-island-backend_default`

如果只更新 `app`，但 `app` 被拉到新网络，而 MySQL/Redis 仍在旧网络，后端就会连接数据库或 Redis 失败。

## 典型现象

### 容器名冲突

执行：

```bash
docker compose up -d app
```

可能报错：

```text
Conflict. The container name "/idea-island-redis" is already in use
```

原因是 Compose 会尝试创建依赖服务 `mysql`、`redis`，但同名容器已经由另一套 Compose 项目创建。

### 只更新 app 后连接失败

执行：

```bash
docker compose up -d --no-deps --build app
```

可以避免重建 MySQL/Redis，但如果 `app` 和 MySQL/Redis 不在同一个 Docker network，应用仍然无法通过 `mysql`、`redis` 主机名访问依赖。

## 关键排查命令

查看运行中的容器：

```bash
docker ps
```

查看容器所属网络：

```bash
docker inspect idea-island-app --format '{{json .NetworkSettings.Networks}}'
docker inspect idea-island-redis --format '{{json .NetworkSettings.Networks}}'
docker inspect idea-island-mysql --format '{{json .NetworkSettings.Networks}}'
```

如果看到类似：

```text
idea-island-app -> idea-island-backend_default
idea-island-redis -> idea-island_default
idea-island-mysql -> idea-island_default
```

说明后端应用和依赖服务不在同一个网络。

查看 Compose 项目生成的资源：

```bash
docker network ls
docker volume ls
```

查看某个 volume 是否有数据：

```bash
docker run --rm -v idea-island-backend_mysql_data:/data alpine sh -c "du -sh /data"
docker run --rm -v idea-island_mysql_data:/data alpine sh -c "du -sh /data"
```

## 当前推荐 Compose 配置

`docker-compose.yml` 顶层固定项目名：

```yaml
name: idea-island
```

服务统一挂载默认网络：

```yaml
services:
  app:
    networks:
      - default

  mysql:
    networks:
      - default

  redis:
    networks:
      - default
```

网络显式命名为稳定名称：

```yaml
networks:
  default:
    name: idea-island_default
```

这样即使在不同目录执行 Compose，也会使用同一个项目名和同一个默认网络。

## 为什么不用自定义网络名

曾尝试：

```yaml
networks:
  idea-island-net:
    name: idea-island_default
```

但服务器已有网络 `idea-island_default` 是由 Compose 的 `default` 网络创建的，带有类似标签：

```text
com.docker.compose.network=default
```

如果新 compose 文件使用 `idea-island-net` 作为网络 key，Compose 会期待标签是：

```text
com.docker.compose.network=idea-island-net
```

两者不一致，会报：

```text
network idea-island_default was found but has incorrect label
```

因此应使用 `default` 作为网络 key。

## 正确更新后端 app

只更新后端应用，不重建 MySQL/Redis：

```bash
docker compose up -d --no-deps --build app
```

更新后确认网络：

```bash
docker inspect idea-island-app --format '{{json .NetworkSettings.Networks}}'
```

应看到 `idea-island_default`。

查看应用日志：

```bash
docker logs -f --tail=200 idea-island-app
```

## volume 警告说明

如果看到：

```text
volume "idea-island-backend_mysql_data" already exists but was created for project "idea-island-backend" (expected "idea-island")
```

这通常不是失败，只是 Compose 项目名变更后发现 volume 的创建项目标签不一致。

只要 volume 名称仍然指向正确的数据卷，数据不会丢失。当前 compose 文件通过显式 volume name 固定数据卷：

```yaml
volumes:
  mysql_data:
    name: ${MYSQL_VOLUME_NAME:-idea-island-backend_mysql_data}
```

如果要完全消除该警告，可以把已有 volume 标为 external，但这会要求新机器部署前先创建 volume；当前项目暂时保留显式 name，不强制 external。

## 接口存在但行为像旧版本的排查

如果前端调用接口有统一返回值，但行为和最新代码不一致，可能是容器仍在运行旧镜像或旧 jar。

建议排查：

```bash
docker ps
docker images | grep idea-island
docker logs --tail=200 idea-island-app
```

更新时务必带 `--build`：

```bash
docker compose up -d --no-deps --build app
```

后续建议在后端启动日志和健康接口中输出版本号、Git commit id、构建时间，方便确认线上实际运行版本。

