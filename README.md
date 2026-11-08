# TeslaMate Android 客户端

这是一个给自建 [TeslaMate](https://github.com/teslamate-org/teslamate) 用的 Android 客户端。它不直接连 Postgres、Grafana 或 MQTT，而是请求旁边的 [TeslaMateApi](https://github.com/tobiasehlert/teslamateapi)（`tobiasehlert/teslamateapi`）。

界面默认是简体中文。系统语言为英语时使用英文。长度和温度按接口返回的单位显示；把 TeslaMate 设成公里和摄氏度后，就是国内习惯的 km / °C。

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

## 安装 APK

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

## 在 App 里配置

1. 手机和 TeslaMateApi 要能互相访问，一般是同一个 Wi-Fi，或者手机已经连上家里的 VPN。
2. 打开 App，填写服务根地址，例如 `http://192.168.1.10:8080`。不要自己加 `/api/v1`；如果粘贴进去了，保存时会去掉。
3. 局域网可以用明文 `http`。应用允许 cleartext，只建议在你信任的网络里这样用。
4. API 令牌留空即可。若填写，长度建议至少 32 位。
5. 点「保存并连接」。能读到车辆后会进入状态页。多辆车时，点顶部车名切换。
6. 状态下拉可以刷新。行程和充电列表可以继续加载，点进一条记录看距离、时长、电量、费用等。

费用按 TeslaMate 存下来的数字显示。接口本身不带币种。

## 应用结构

- Kotlin、Jetpack Compose、Material 3，`minSdk` 26
- Gradle Kotlin DSL，版本在 `gradle/libs.versions.toml`
- Hilt 注入 Retrofit + OkHttp + kotlinx.serialization
- 地址和令牌存在 DataStore（`teslamate_settings`）。未开启备份，避免令牌进云备份
- 界面：首次设置、车辆状态、行程列表和详情、充电列表和详情

读接口失败时，TeslaMateApi 经常仍返回 HTTP 200，正文是 `{"error":"..."}`。客户端会把它当成错误，而不是空数据。

## 后续可以做

行程地图、能耗曲线、桌面小组件、用 MQTT 做实时刷新，以及在配置了令牌之后发送解锁、空调这类命令。
