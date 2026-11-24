# Shadow Dungeon 网页版

这个项目将 Shadow Dungeon 的 Java 游戏逻辑接入了浏览器。后端处理角色移动、碰撞、射击、敌人、商店和房间切换。前端采集键鼠输入，在 Canvas 上绘制后端返回的画面。

## 本地运行

需要 JDK 17 或更新版本。在项目目录运行：

```sh
./run.sh
```

打开 [http://127.0.0.1:8080](http://127.0.0.1:8080)。如需更换端口，可运行 `./run.sh 8081`。脚本使用 JDK 自带的编译器和 HTTP 服务器。

进入游戏后，按 **R** 选择机器人，或按 **M** 选择陆战队员。用 **WASD** 移动，鼠标瞄准，左键射击。按 **空格** 打开商店，其余按键列在游戏旁边。若键盘没有响应，先点击游戏画面。

## 项目结构

| 目录 | 内容 |
| --- | --- |
| `backend/src/` | Java 游戏规则、绘图指令和 HTTP 服务 |
| `frontend/` | 游戏页面、Canvas 渲染和键鼠输入 |
| `res/` | 游戏图片、字体、文案和关卡配置 |

浏览器通过 `POST /api/session` 创建一局游戏，通过 `POST /api/frame` 提交输入并获取绘图指令。浏览器记住存档标识，刷新时尝试恢复原来的游戏。同一浏览器的标签页共用这一进度。服务默认仅监听本机。

## Vite 开发

需要 Node.js 24、npm 和 JDK 17 或更新版本。先运行 `./run.sh` 启动 Java 服务，再在另一个终端运行：

```sh
npm ci
npm run dev
```

Vite 会代理 `/api` 和 `/res` 到本机 Java 服务。运行 `npm run build` 可生成 `dist/`。使用 `FRONTEND_DIR=dist ./run.sh` 测试生产构建。

## 部署

仓库的 `Dockerfile` 通过 Vite 构建前端、编译 Java，然后由一个 Java 服务在同一个域名提供页面、资源和 API。容器默认监听 `0.0.0.0:8080`，也读取托管平台提供的 `PORT`。

`render.yaml` 声明了 Render 的免费 Docker Web Service。可以在 Render 创建 Blueprint 并选中此仓库。免费服务空闲后会休眠，首次访问可能需要等待唤醒。


## 保存游戏进度

Java 服务通过 JDBC 连接 PostgreSQL。启动时会自动创建 `game_saves` 表，数据库账号需要建表及读写权限。在运行服务的环境中设置：

| 变量 | 示例或用途 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://127.0.0.1:5432/shadow_dungeon` |
| `DB_USER` | PostgreSQL 用户 |
| `DB_PASSWORD` | PostgreSQL 密码 |

使用远程数据库时，按托管方要求为 JDBC URL 添加 TLS 参数。Docker 部署已包含驱动。本地使用 `./run.sh` 时，首次配置数据库会从 Maven Central 下载并校验驱动。在线部署时，在 Render 服务的环境变量中填写数据库连接信息并重新部署。

游戏会在切换房间、选角、金币或钥匙变化、重开时保存，并在游玩期间定期保存。需要立即保存时可点击「保存进度」。刷新优先恢复内存里的最新状态。服务重启后会从数据库读取最近一次成功存档。存档标识保存在当前浏览器中，清除网站数据或换浏览器后会开始新游戏。未设置 `DB_URL` 时，刷新仍可恢复尚未过期的内存会话，服务重启后会重新开局。

### 用 Docker 本地启动 PostgreSQL

在终端设置 `DB_PASSWORD` 后执行以下命令。数据卷保留存档，数据库端口只绑定本机。

```sh
export DB_PASSWORD='替换为你的本地数据库密码'
POSTGRES_PASSWORD="$DB_PASSWORD" docker run -d --name shadow-dungeon-db \
  -p 127.0.0.1:5432:5432 -v shadow-dungeon-data:/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD -e POSTGRES_DB=shadow_dungeon postgres:17

export DB_URL='jdbc:postgresql://127.0.0.1:5432/shadow_dungeon'
export DB_USER=postgres
./run.sh
```
