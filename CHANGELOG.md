# 更新日志

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/lang/zh-CN/).

## [1.1.0] - 2026-04-14

### 新增功能

#### 地图交互优化
- **移动地图不再弹出标记框**：区分地图拖动和点击操作
- **点击添加标记模式**：新增📍按钮，点击后进入点击添加模式，再点击地图位置即可添加标记
- **比例尺显示**：左下角显示比例尺，随缩放自动更新，显示1cm代表多少米
- **东南西北方向指示**：地图边缘显示N(红)/S/E/W(灰)方向标识
- **经纬度坐标显示**：左下角实时显示地图中心点的经度和纬度坐标

#### 轨迹记录功能
- **开始/停止轨迹记录**：新增▶️/⏹️按钮，点击开始记录轨迹
- **自动轨迹点记录**：开始记录后，移动手机自动记录GPS轨迹点
- **实时距离显示**：状态栏显示当前已记录轨迹的距离（公里）
- **轨迹保存**：停止记录后自动保存轨迹到本地数据库

### 修复问题
- 修复 `MapLayer` 枚举格式错误问题
- 修复 `ComposeColor.Red` 颜色引用问题
- 修复 `TracksScreen` 中 `formatDate` 函数缺失问题

## [1.0.0] - 2026-04-13

### 首次发布

#### 核心功能
- 地点标记（长按添加）
- 多种地图图层切换（标准/卫星/等高线）
- 轨迹记录基础功能
- 标记列表管理
- 轨迹列表管理

#### 技术实现
- Jetpack Compose + Material Design 3
- OSMDroid 地图引擎
- Room 本地数据库
- MVVM 架构
