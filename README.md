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

浏览器通过 `POST /api/session` 创建一局游戏，通过 `POST /api/frame` 提交输入并获取绘图指令。每个标签页创建独立的一局。进度存放在 Java 进程内存里，刷新页面会重新开始。服务默认仅监听本机。

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
