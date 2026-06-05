# 清单 Android

基于 Jetpack Compose 开发的本地清单管理 Android 应用，适合礼事、租赁、活动物品等场景的物品登记、数量统计和清单生成。

## 功能特性

- 物品管理：添加、修改、删除、排序、批量导入
- 数量登记：增加、减少、直接输入数量、重置数量
- 清单生成：生成小票风格物品清单
- 清单操作：复制文本、保存清单图片到相册
- 店铺信息：维护店铺名称、联系人、手机号和地址
- 本地保存：使用 SharedPreferences 保存数据，不依赖服务器

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin
- SharedPreferences + JSON

## 页面说明

- `登记`：录入每个物品的数量
- `清单`：查看已登记物品，并复制或保存为图片
- `物品管理`：维护物品名称、顺序和批量导入
- `店铺信息`：维护清单底部展示的联系方式

## 构建方式

使用 Android Studio 打开项目，等待 Gradle 同步完成后运行即可。

也可以在命令行构建：

```bash
./gradlew assembleDebug
```

如果本机需要指定 Android Studio 自带 JDK：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug
```

## 预览

项目 UI 使用 Jetpack Compose 编写，`MainActivity.kt` 中包含 `@Preview`，可以在 Android Studio 中查看登记页和清单页预览。

## 数据说明

应用数据保存在本机 SharedPreferences 中，主要包括：

- 物品名称、数量、排序
- 清单标题
- 店铺名称、联系人、手机号、地址

当前版本为本地单机应用，不会上传数据到服务器。
