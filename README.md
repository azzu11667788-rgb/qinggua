# 清瓜

一款安卓照片整理 App。打开后把手机里的照片/视频变成卡片流：左滑保留、右滑删除、下滑待定。所有记录只存本机，不登录、不上传。

## 功能

- **三个入口**：照片 / 视频 / 合并，共享同一份分类状态，一张照片只整理一次
- **手势**：左滑 → 已保留；右滑 → 回收箱；下滑到屏幕下方固定区域 → 待定
- **撤销**：支持连续多步撤销（设置里可选 10/20/50 步）
- **断点续整**：下次打开接着上次的进度，顶部显示剩余数量
- **回收箱**：单选 / 多选 / 全选后真正从手机删除；未删前可恢复
- **待定区**：独立入口，点进去只能保留或删除，不能再次待定
- **已保留**：可批量移回待整理或移到回收箱，照片本身仍留在系统相册原位
- **筛选**：按相册、日期筛选，排序可切最新/最老
- **卡片信息**：显示拍摄时间、位置坐标、文件大小
- **隐私锁**：可选，跟手机锁屏的指纹/面容解锁（系统验证，App 不存生物信息）

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- MediaStore 读取本机媒体、执行删除
- SharedPreferences + JSON 持久化分类状态、断点、撤销栈、设置
- Coil 加载图片、androidx.biometric 生物识别

## 用 Android Studio 编译出 APK

本工程源码完整，但这个环境没有安装 JDK / Android SDK，无法在这里直接编译。你在自己电脑上这样操作：

1. **安装 Android Studio**（官网下载，安装时勾选 Android SDK）
2. **打开工程**：Android Studio → File → Open → 选择 `qinggua` 文件夹
   - 工程里没有 `gradle-wrapper.jar`（环境限制无法生成二进制文件），Android Studio 打开时会自动补齐，无需手动处理
3. **等待 Gradle 同步**：首次会下载依赖，需要联网，几分钟
4. **连接手机**：手机开启「开发者选项」→「USB 调试」，用数据线连电脑
5. **生成 APK**：菜单 Build → Build Bundle(s)/APK(s) → Build APK(s)
   - 产物在 `app/build/outputs/apk/debug/app-debug.apk`
6. **安装到手机**：把 APK 传到手机，点开安装（需允许「安装未知应用」）

> 想装到别的手机、或上架应用商店，需要生成**签名**的 release APK：Build → Generate Signed Bundle/APK，按向导创建 keystore（务必保存好，丢了就无法更新）。

## 关于 GitHub 云编译（后续）

你说过没有 GitHub 账号。等注册后，可以用 GitHub Actions 免电脑编译出 APK：

1. 注册 GitHub 账号，新建一个仓库，把本工程代码传上去
2. 在仓库里添加 `.github/workflows/build.yml`，用 `gradlew assembleDebug` 编译
3. Actions 跑完后在 Artifacts 里下载 APK

需要的时候我可以帮你把这套 workflow 和脚本写好。

## 权限说明

- 读取图片/视频：Android 13+ 用 `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`，低版本用 `READ_EXTERNAL_STORAGE`
- 位置坐标：需要 `ACCESS_MEDIA_LOCATION`，且部分照片系统不返回 GPS
- 真删：Android 11+ 调用系统 `createDeleteRequest` 弹确认框；低版本直接删除

## 已知限制

- 系统相册的「最近删除」是相册 App 自己的功能，第三方 App 无法把照片放进去。所以本 App 的「回收箱」是自己维护的，真删后不可恢复
- Android 14+ 用户若只授权「部分照片」，App 只能看到被选中的那些
- 坐标依赖照片本身是否带 GPS，且需要位置权限；没有就只显示文件大小
- 本工程已做静态检查（括号平衡、符号引用、import、版本兼容），但**未在本机实际编译运行**，首次用 Android Studio 打开时如有编译报错请把报错内容发我，我马上修
