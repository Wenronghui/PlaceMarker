# 足迹地图 - PlaceMarker

一款专为户外探险设计的地点标记与轨迹记录应用。

## 功能特性

### 核心功能
- **地点标记**：点击地图添加标记点，记录你去过的任何地点
  - 支持添加名称、描述和分类（景点、美食、住宿、出行、其他）
  - 分类图标和颜色区分
- **轨迹记录**：记录你的行走路径，保存历史轨迹
  - 点击开始按钮开始记录，移动自动标记轨迹点
  - 实时显示已行走距离（公里）
  - 停止后自动保存轨迹

### 地图功能
- **多种地图图层**：支持标准地图、卫星图、山地等高线图等多种图层切换
- **比例尺显示**：左下角实时显示比例尺（1cm = X米）
- **方向指示**：东南西北方向标识（N红色/S/E/W灰色）
- **经纬度坐标**：左下角显示地图中心点的经纬度坐标
- **网络地图**：支持在线下载地图瓦片

### 轨迹管理
- 查看所有历史轨迹记录
- 显示每条轨迹的起止时间、距离、途经点数
- 轨迹点格式化时间显示

## 技术栈

- **UI**: Jetpack Compose + Material Design 3
- **地图**: OSMDroid (开源地图，支持离线)
- **数据库**: Room (本地存储)
- **架构**: MVVM + Clean Architecture
- **定位**: Google Location Services
- **地图图层**: OSMDroid + 自定义瓦片源

## 项目结构

```
app/src/main/java/com/footprint/footprint/
├── MainActivity.kt          # 主入口
├── FootprintApp.kt          # 应用配置
├── ui/
│   ├── map/                 # 地图相关UI
│   │   ├── MapScreen.kt     # 地图页面
│   │   └── MapViewModel.kt  # 地图逻辑
│   ├── markers/             # 标记列表
│   └── tracks/              # 轨迹列表
│       ├── TracksScreen.kt  # 轨迹页面
│       └── TracksViewModel.kt
├── data/
│   ├── local/               # Room数据库
│   │   ├── AppDatabase.kt
│   │   ├── MarkerDao.kt
│   │   └── TrackDao.kt
│   └── repository/          # 数据仓库
├── domain/
│   └── model/               # 数据模型
│       ├── Marker.kt
│       ├── MarkerCategory.kt
│       ├── Track.kt
│       └── TrackPoint.kt
└── util/                    # 工具类
    └── LocationUtils.kt
```

## 构建

```bash
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`

## 权限

- `ACCESS_FINE_LOCATION` - 精确定位
- `ACCESS_COARSE_LOCATION` - 粗略定位
- `WRITE_EXTERNAL_STORAGE` - 离线地图保存
- `INTERNET` - 在线地图下载

## 使用说明

### 添加标记
1. 点击右下角的📍编辑位置按钮
2. 点击地图上你想要标记的位置
3. 填写名称、描述，选择分类
4. 点击保存

### 记录轨迹
1. 点击右下角的▶️开始按钮
2. 移动手机，应用会自动记录你的行走轨迹
3. 状态栏显示"轨迹记录中"和当前距离
4. 点击⏹️停止按钮结束记录

### 切换地图图层
1. 点击右下角的📚图层按钮
2. 选择你想要的地图类型（标准/卫星/等高线）
