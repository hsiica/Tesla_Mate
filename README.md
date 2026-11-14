# TeslaMate 客户端

这是给自建 [TeslaMate](https://github.com/teslamate-org/teslamate) 用的手机客户端，包含 Android 和 iOS。两边都不直接连 Postgres、Grafana 或 MQTT，而是请求旁边的 [TeslaMateApi](https://github.com/tobiasehlert/teslamateapi)（`tobiasehlert/teslamateapi`）。

界面默认是简体中文。系统语言为英语时使用英文。长度和温度按接口返回的单位显示；把 TeslaMate 设成公里和摄氏度后，就是国内习惯的 km / °C。接口没带单位时，按 km / °C 显示。

Android 在仓库根目录的 Gradle 工程里。iOS 在 `ios/TeslaMate.xcodeproj`，和 Gradle 工程互不影响。

## 在现有 TeslaMate 旁边加上 TeslaMateApi

官方 TeslaMate 的 Compose 里，数据库服务一般叫 `database`，MQTT 一般叫 `mosquitto`。把下面这段加进同一个 `docker-compose.yml` 的 `services:` 下面。`ENCRYPTION_KEY`、数据库账号必须和 TeslaMate 用的是同一套，主机名也要和你现有服务名一致。

```yaml
  teslamateapi:
    image: tobiasehlert/teslamateapi:latest
    restart: always
    depends_on:
      - database
      - mosquitto
    environment:
      - ENCRYPTION_KEY=这里填和 TeslaMate 相同的密钥
      - DATABASE_USER=teslamate
      - DATABASE_PASS=secret
      - DATABASE_NAME=teslamate
      - DATABASE_HOST=database
      - MQTT_HOST=mosquitto
      - TZ=Asia/Shanghai
      # 可选。至少 32 位。不设置时，车辆/状态/行程/充电这些读接口不需要令牌。
      # - API_TOKEN=请换成至少32位的随机字符串
    ports:
      - "8080:8080"
```

然后：

```bash
docker compose up -d teslamateapi
```

浏览器或手机访问 `http://<服务器局域网IP>:8080/api/v1/cars`，能看到 JSON 就说明服务起来了。

状态接口 `/api/v1/cars/{id}/status` 依赖 MQTT。不要设置 `DISABLE_MQTT=true`，否则这个接口会返回无法使用。

`API_TOKEN` 只在命令、唤醒、日志开关这类接口上强制校验。本应用只读数据，令牌可以留空。如果填了，每次请求都会带 `Authorization: Bearer <令牌>`，方便你前面还有一层会检查这个头的反向代理。

更完整的环境变量以 [TeslaMateApi README](https://github.com/tobiasehlert/teslamateapi/blob/main/README.md) 为准。

## 安装 Android APK

不需要在电脑上装 Android SDK。仓库里的 GitHub Actions 会打出 debug APK。

1. 打开本仓库的 Actions，进入 **Build debug APK**。
2. 选最近一次成功的运行，下载 artifact **teslamate-debug-apk**。
3. 解压后把 `app-debug.apk` 拷到手机上安装。系统会提示允许安装未知来源应用。
4. debug 包使用调试签名。以后如果换了签名，需要先卸载再装。

也可以在本机编译（需要 JDK 17+ 和 Android SDK 35）：

```bash
./gradlew assembleDebug
```

APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

## 用 Xcode 打开并运行 iOS

需要一台 Mac，以及 **Xcode 16 或更新版本**（带 iOS 17 SDK）。工程用的是 Xcode 16 的文件夹同步项目，Xcode 15 打不开。这个环境是 Linux，这里不能编译 iOS，也没有配 GitHub Actions 的模拟器构建：那需要 macOS 和 Xcode，而且没有 Apple 签名。

1. 在 Finder 里双击 `ios/TeslaMate.xcodeproj`，或在 Xcode 里选 Open 打开它。不需要 CocoaPods，也没有额外的 Swift Package。
2. 等索引结束。如果 Xcode 提示 “Update to recommended settings”，可以先点取消，工程按现在的设置就能编。
3. 窗口顶部的运行目标选一台 **iPhone 模拟器**（例如 iPhone 16）。模拟器不需要付费的 Apple Developer 账号。
4. 按 Run（▶），或按 `Cmd+R`。
5. 如果签名报错：点左侧的 TeslaMate 工程，选中 **TeslaMate** target，打开 **Signing & Capabilities**，Team 里选你的 Personal Team。Bundle Identifier 默认是 `app.teslamate.client.ios`。如果提示已被占用，改成你自己的，例如 `app.teslamate.client.ios.yourname`。
6. 真机：用数据线连上 iPhone（系统需要 iOS 17 或更高），顶部目标改成这台手机，同样在 Signing 里选好 Team，再 Run。手机如果提示开发者不受信任，到 **设置 → 通用 → VPN 与设备管理** 里信任你的证书。

第一次启动会进入「连接 TeslaMate」。地址和令牌与 Android 用的是同一套 TeslaMateApi：

1. 服务地址填根地址，例如 `http://192.168.1.10:8080`。不要自己加 `/api/v1`；如果粘贴进去了，保存时会去掉。
2. API 令牌留空即可。填写后会存在钥匙串里（仅本机），之后每个请求带 `Authorization: Bearer`。
3. 模拟器走的是 Mac 的网络。服务就在这台 Mac 上时填 `http://127.0.0.1:8080`；服务在局域网另一台机器上时填那台机器的 IP。真机必须能访问这个地址，一般是同一个 Wi-Fi，或手机已经连上家里的 VPN。
4. 第一次访问局域网，系统会弹出本地网络权限，需要允许。说明文字在 `NSLocalNetworkUsageDescription`。
5. 明文 HTTP 已放行：`Info.plist` 里同时开了 `NSAllowsLocalNetworking` 和 `NSAllowsArbitraryLoads`，这样局域网 IP 和 VPN 网段都能用 `http`。只建议在你信任的网络里这样用。
6. 点「保存并连接」。能读到车辆后进入状态页。多辆车时，点顶部车名切换。下拉可以刷新。行程和充电可以继续加载，点进一条看距离、时长、电量、费用和峰值充电采样。

地址存在 UserDefaults。清除配置会同时删掉地址和钥匙串里的令牌。

## 在 App 里配置

1. 手机和 TeslaMateApi 要能互相访问，一般是同一个 Wi-Fi，或者手机已经连上家里的 VPN。
2. 打开 App，填写服务根地址，例如 `http://192.168.1.10:8080`。不要自己加 `/api/v1`；如果粘贴进去了，保存时会去掉。
3. 局域网可以用明文 `http`。Android 允许 cleartext，iOS 用上面的 ATS 例外。只建议在你信任的网络里这样用。
4. API 令牌留空即可。若填写，长度建议至少 32 位。
5. 点「保存并连接」。能读到车辆后会进入状态页。多辆车时，点顶部车名切换。
6. 状态下拉可以刷新。行程和充电列表可以继续加载，点进一条记录看距离、时长、电量、费用等。

费用按 TeslaMate 存下来的数字显示。接口本身不带币种。

## 应用结构

Android：

- Kotlin、Jetpack Compose、Material 3，`minSdk` 26
- Gradle Kotlin DSL，版本在 `gradle/libs.versions.toml`
- Hilt 注入 Retrofit + OkHttp + kotlinx.serialization
- 地址和令牌存在 DataStore（`teslamate_settings`）。未开启备份，避免令牌进云备份
- 界面：首次设置、车辆状态、行程列表和详情、充电列表和详情

iOS：

- Swift、SwiftUI，`@Observable`，`async/await`，`URLSession`，最低 iOS 17
- 工程在 `ios/TeslaMate.xcodeproj`。源码在 `ios/TeslaMate/`，Xcode 16 会按文件夹自动收进 target
- 地址存在 UserDefaults，令牌存在钥匙串
- 界面和 Android 对应：首次设置、车辆状态、行程列表和详情、充电列表和详情
- 文案在 `Localizable.xcstrings`，开发语言是 `zh-Hans`，另有英文

两边读的都是这些接口（和 TeslaMateApi `src/webserver.go` 一致）：

- `GET /api/v1/cars`
- `GET /api/v1/cars/{id}/status`
- `GET /api/v1/cars/{id}/drives?page&show`
- `GET /api/v1/cars/{id}/drives/{driveId}`
- `GET /api/v1/cars/{id}/charges?page&show`
- `GET /api/v1/cars/{id}/charges/{chargeId}`

读接口失败时，TeslaMateApi 经常仍返回 HTTP 200，正文是 `{"error":"..."}`。客户端会把它当成错误，而不是空数据。

## 后续可以做

行程地图、能耗曲线、桌面小组件、用 MQTT 做实时刷新，以及在配置了令牌之后发送解锁、空调这类车辆命令。
