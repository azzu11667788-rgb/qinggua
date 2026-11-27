# 清瓜 · GitHub 云编译指南

本工程已配好 GitHub Actions 云编译工作流（`.github/workflows/build-apk.yml`）。
代码推到 GitHub 后，云端会自动编译出 APK，你直接下载到手机安装，本机不需要装 Java / Android Studio。

---

## 第 1 步：在 GitHub 上创建仓库

1. 打开 https://github.com/new 并登录
2. 填写信息：
   - **Repository name**：`qinggua`（或任意名字）
   - **Description**：随便填，可留空
   - **Public / Private**：都可以（Public 免费额度更多，Private 也够用）
   - ⚠️ **不要**勾选 "Add a README file"、"Add .gitignore"、"Choose a license"
     —— 仓库必须是空的，否则推送会冲突
3. 点 **Create repository**

创建后页面会显示一段 "…or push an existing repository from the command line" 的命令，先别关页面，第 3 步要用。

---

## 第 2 步：确认本机代码已提交

工程目录已经初始化好 Git 并完成首次提交，你不需要做任何事。
（如果以后改了代码，需要重新提交，见文末"代码改动后如何重新构建"。）

---

## 第 3 步：推送代码到 GitHub

在本工程目录 `qinggua/` 下打开命令行（Git Bash 或 PowerShell），执行：

```bash
git remote add origin https://github.com/你的用户名/qinggua.git
git branch -M main
git push -u origin main
```

> 把 `你的用户名` 换成你自己的 GitHub 用户名，仓库名要和第 1 步创建的一致。
>
> 推送时 GitHub 会要求登录认证：
> - 如果用账号密码，现在 GitHub 已不接受密码，需要用 **Personal Access Token**
>   （GitHub 右上角头像 → Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token，勾选 `repo` 权限）
> - 把 token 当作密码输入即可
> - 建议用 GitHub Desktop 图形化登录，免去 token 的麻烦

---

## 第 4 步：触发云编译

推送成功后：

1. 打开你的仓库页面，点顶部 **Actions** 标签
2. 左侧点 **构建清瓜 APK**，右侧点 **Run workflow** → 再点绿色的 **Run workflow**
   （推送代码时也会自动触发一次，所以第一次可能已经在跑了）
3. 等待编译，通常 3～8 分钟。点进任务可以看到实时日志
4. 编译成功后，在任务页面最下方 **Artifacts** 区域找到 **qinggua-apk**，点它下载（是一个 zip）
5. 解压 zip，得到 `qinggua-v1.0.apk`

---

## 第 5 步：安装到手机

1. 把 `qinggua-v1.0.apk` 传到安卓手机（微信文件传输助手、数据线、网盘都行）
2. 在手机上点开这个 APK
3. 系统会提示"未知来源应用"，选择**允许安装**
   （设置 → 安全 → 安装未知应用 → 给文件管理器/微信相应权限）
4. 安装完成，打开"清瓜"
5. 首次打开会请求读取图片/视频权限，选择**允许**

---

## 代码改动后如何重新构建

改完代码后，在工程目录执行：

```bash
git add .
git commit -m "说明改了什么"
git push
```

推送后 Actions 会自动重新构建。去 Actions 页面下载新的 APK 即可（旧的构建产物 90 天后自动失效）。

---

## 常见问题

**Q：Actions 页面显示红色 ❌ 编译失败？**
点进失败的任务，展开红色的步骤看日志，把错误信息发给我，我帮你修。

**Q：手机安装时提示"应用未安装"？**
- 检查手机是否已有同名应用（先卸载旧的）
- 检查 Android 版本是否 ≥ 8.0（minSdk 26）
- 确认下载的是完整 APK 文件，不是 zip

**Q：每次都要手动点 Run workflow 吗？**
不用。推送代码会自动触发。手动触发只是为了方便你重新构建同一版本。

**Q：云编译要钱吗？**
公开仓库完全免费。私有仓库每月有 2000 分钟免费额度，编译一次约 5 分钟，个人使用绰绰有余。
