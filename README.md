# 荆州纪南污水厂监控系统

本项目包含污水处理工艺看板、三维设备场景、PLC 状态与控制、监测历史、告警和运行维护脚本。先读 [项目导读](docs/项目导读.md)，再按需查看源码、数据库和模型索引。

## 前端构建

在 `frontend` 目录中运行：

```sh
npm ci
npm run build
```

开发服务器使用 `npm run dev`。本地联调时，通过 `VITE_API_PROXY_TARGET` 指定测试后端。生产构建由后端托管时，参照 [使用说明](docs/使用说明.txt) 和 [生产配置样例](backend/application-prod.example.yaml)。

## 恢复大型资源

资源包包含原始库中迁出的 15 个 GLB 和 1 个 MP4。在仓库根目录使用 Python 3.11 或更高版本执行以下命令，恢复原文件名和目录结构，并校验 SHA-256：

```sh
python tools/restore-assets.py /path/to/wushuichulichang-assets.zip
```

需要完整的原仓库模型及视频时，在构建前执行恢复。可在 [模型与视频索引](docs/模型与视频索引.json) 中查询资源位置。仓库仍保留 8 个 GLB、全部原始图片、前端代码、JAR、SQL、运维脚本和原始文档。
