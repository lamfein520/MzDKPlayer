# MzDKPlayer 项目分析与学习计划

## 项目概述

**MzDKPlayer** 是一款专为安卓电视（Android TV）设计的本地弹幕视频播放器，支持多种网络协议播放及高端音频视频格式播放。

### 技术栈分析
- **UI框架**: Jetpack Compose for TV
- **媒体播放**: ExoPlayer + 自定义扩展
- **弹幕引擎**: AKDanmaku
- **字幕渲染**: ASS字幕渲染库
- **网络协议**: 自定义SMB/FTP/WebDAV/NFS客户端实现
- **架构模式**: MVVM (使用ViewModel)

## 项目结构分析

### 核心目录结构
```
app/src/main/java/org/mz/mzdkplayer/
├── MainActivity.kt                    # 主Activity，处理密度适配
├── MzDkPlayerApplication.kt          # Application类
├── danmaku/                          # 弹幕相关
├── logic.model/                      # 数据模型和Repository
├── tool/                            # 工具类（重点关注）
│   ├── SMBDataSource.kt             # SMB数据源实现
│   ├── FTPDataSource.kt             # FTP数据源实现
│   ├── WebDavDataSource.kt          # WebDAV数据源实现
│   ├── SmbUtils.kt                  # SMB工具类
│   └── FileMediaInfo.kt             # 媒体信息提取
└── ui/                              # UI相关
    ├── MzDKPlayerAPP.kt             # 主应用UI
    ├── screen/                      # 各种界面
    │   ├── smbfile/                 # SMB文件浏览
    │   ├── ftp/                     # FTP文件浏览
    │   ├── webdavfile/              # WebDAV文件浏览
    │   ├── nfs/                     # NFS文件浏览
    │   └── vm/                      # ViewModels
    ├── videoplayer/                 # 视频播放器
    └── audioplayer/                 # 音频播放器
```

### 关键技术实现

#### 1. 文件传输协议实现
- **SMB协议**: 使用smbj库，实现了自定义DataSource
- **FTP协议**: 使用Apache Commons Net库
- **WebDAV协议**: 使用sardine-android库
- **NFS协议**: 使用nfs-client库

#### 2. 性能优化策略
- SMB数据源针对大文件优化，使用直接偏移量读取
- FTP增大缓冲区，设置Socket超时
- WebDAV使用Range请求头优化随机访问

## 当前存在的性能问题

### SMB播放卡顿问题（核心问题）
**问题描述**: 虽然预加载速度稳定（9.7~9.9 MB/s），但播放开始后读取速度明显下降

**根本原因**:
1. **单线程/低优先级 I/O 设计**: SMB数据源未与视频解码线程充分解耦
2. **设备性能瓶颈**: 在低端设备上资源争抢严重
3. **ExoPlayer缓冲策略未优化**: 默认缓冲区较小

**优化方向**:
- 独立下载线程 + 动态缓冲区 + 优先级调度
- 异步预缓冲机制
- 针对SMB优化的缓冲策略

## 学习计划

### 阶段一：基础理解（1-2周）
- [x] 项目结构分析
- [ ] Kotlin语法学习（针对项目中的用法）
- [ ] Jetpack Compose基础学习
- [ ] MVVM架构理解
- [ ] ExoPlayer基础概念

### 阶段二：核心功能研究（2-3周）
- [ ] 深入研究SMBDataSource实现
- [ ] 分析FTPDataSource和WebDavDataSource
- [ ] 理解ExoPlayer DataSource接口
- [ ] 研究弹幕引擎AKDanmaku的使用
- [ ] 分析媒体信息提取逻辑

### 阶段三：性能问题诊断（1-2周）
- [ ] 搭建调试环境
- [ ] 复现SMB播放卡顿问题
- [ ] 使用性能分析工具定位瓶颈
- [ ] 分析线程调度和资源竞争

### 阶段四：优化实现（3-4周）
- [ ] 设计异步预缓冲方案
- [ ] 实现独立下载线程
- [ ] 优化缓冲区管理策略
- [ ] 添加线程优先级调度
- [ ] 性能测试和调优

### 阶段五：移植和扩展（2-3周）
- [ ] 将优化后的功能移植到公司TV应用
- [ ] 适配公司应用的架构
- [ ] 添加必要的功能扩展
- [ ] 完整测试和文档编写

## 技术重点关注

### 1. 网络协议实现
- SMB协议的Java实现（smbj库）
- FTP协议的实现细节
- WebDAV协议的HTTP Range请求
- 各协议的性能特点和优化策略

### 2. ExoPlayer扩展
- 自定义DataSource的实现
- 媒体播放器的缓冲策略
- 多轨道音视频处理

### 3. Android TV开发
- Compose for TV的使用
- 遥控器交互处理
- 大屏UI设计原则

### 4. 性能优化
- 多线程编程
- 内存管理
- 网络I/O优化
- 缓冲区设计

## 预期成果

1. **技术能力提升**: 掌握Kotlin、Compose、MVVM等现代Android开发技术
2. **问题解决能力**: 解决SMB等文件传输性能问题
3. **项目经验**: 完成开源项目贡献和企业级应用移植
4. **简历亮点**: 展示从问题发现到解决的完整技术能力

## 下一步行动

1. 开始Kotlin语法学习，重点关注项目中使用的特性
2. 搭建开发环境，能够成功编译和运行项目
3. 深入研究SMBDataSource.kt的实现细节
4. 开始记录学习笔记和技术总结

## 最新更新（2025-10-01）
- **学习要点**: 理解Compose中`NavHost`/`NavigationDrawer`的组合式导航模式，以及Kotlin `remember` + `mutableIntStateOf`如何取代Java中`ViewModel`或字段保存UI状态。
- **代码分析**: `app/src/main/java/org/mz/mzdkplayer/ui/MzDKPlayerAPP.kt`实现TV端主界面，负责搭建侧边抽屉菜单、维护选中索引，并通过多个`composable`路由关联分屏（本次仅阅读，无修改）。
- **问题与解答**: “MzDKPlayerAPP的作用是什么？”——它作为Compose入口组装全局导航，并把不同协议/播放界面绑定到导航路由。
- **学习进度**: Kotlin/Compose基础阅读启动，尚未动手编写示例代码；下一步准备梳理`rememberNavController`与传统`FragmentManager`的差异。

---
*更新时间: 2025-10-01*
*状态: 项目分析完成，持续学习中*
