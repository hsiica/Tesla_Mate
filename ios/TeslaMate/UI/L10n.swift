import Foundation

enum L10n {
    static var onboardingTitle: String { tr("onboarding_title", "连接 TeslaMate") }
    static var onboardingBody: String { tr("onboarding_body", "填写 TeslaMateApi 的地址。手机需要能访问这台服务，一般是同一局域网，或已经连上家里的 VPN。") }
    static var settingsTitle: String { tr("settings_title", "设置") }
    static var baseURLLabel: String { tr("base_url_label", "服务地址") }
    static var baseURLPlaceholder: String { tr("base_url_placeholder", "http://192.168.1.10:8080") }
    static var baseURLHelp: String { tr("base_url_help", "填写根地址即可，不要带 /api/v1。局域网可以使用 http。") }
    static var apiTokenLabel: String { tr("api_token_label", "API 令牌（可选）") }
    static var apiTokenPlaceholder: String { tr("api_token_placeholder", "至少 32 位，留空则不发送") }
    static var apiTokenHelp: String { tr("api_token_help", "读取车辆、状态、行程和充电时，TeslaMateApi 默认不校验令牌。如果填写了，每次请求都会带上 Authorization: Bearer。命令类接口和部分反向代理会用到它。") }
    static var tokenShortWarning: String { tr("token_short_warning", "令牌少于 32 位。TeslaMateApi 建议至少 32 位，短令牌在需要鉴权的接口上会被拒绝。") }
    static var saveAndConnect: String { tr("save_and_connect", "保存并连接") }
    static var saving: String { tr("saving", "正在连接…") }
    static var invalidURL: String { tr("invalid_url", "请输入以 http:// 或 https:// 开头的完整地址") }
    static var clearSettings: String { tr("clear_settings", "清除配置") }
    static var clearSettingsTitle: String { tr("clear_settings_title", "清除已保存的地址和令牌？") }
    static var clearSettingsBody: String { tr("clear_settings_body", "清除后需要重新填写才能查看车辆数据。") }
    static var clearConfirm: String { tr("clear_confirm", "清除") }
    static var cancel: String { tr("cancel", "取消") }
    static var httpNotice: String { tr("http_notice", "当前使用明文 HTTP，只建议在可信的局域网里这样用。") }
    static var navStatus: String { tr("nav_status", "状态") }
    static var navDrives: String { tr("nav_drives", "行程") }
    static var navCharges: String { tr("nav_charges", "充电") }
    static var navSettings: String { tr("nav_settings", "设置") }
    static var retry: String { tr("retry", "重试") }
    static var loading: String { tr("loading", "正在加载…") }
    static var loadingMore: String { tr("loading_more", "正在加载更多…") }
    static var emptyCarsTitle: String { tr("empty_cars_title", "还没有车辆") }
    static var emptyCarsBody: String { tr("empty_cars_body", "TeslaMateApi 已连通，但数据库里没有车辆。确认 TeslaMate 已经同步过至少一辆车。") }
    static var emptyDrives: String { tr("empty_drives", "还没有已完成的行程") }
    static var emptyCharges: String { tr("empty_charges", "还没有充电记录") }
    static var errorNetwork: String { tr("error_network", "连不上 TeslaMateApi。请检查地址、端口，以及手机和服务器是否在同一网络。") }
    static var errorParse: String { tr("error_parse", "服务器返回的数据无法解析。请确认地址指向的是 TeslaMateApi，而不是 TeslaMate 网页或 Grafana。") }
    static var errorNotConfigured: String { tr("error_not_configured", "还没有配置 TeslaMateApi 地址") }
    static var loadMore: String { tr("load_more", "加载更多") }
    static var batteryLevel: String { tr("battery_level", "电量") }
    static var ratedRange: String { tr("rated_range", "额定续航") }
    static var estRange: String { tr("est_range", "预估续航") }
    static var idealRange: String { tr("ideal_range", "理想续航") }
    static var odometer: String { tr("odometer", "里程表") }
    static var software: String { tr("software", "软件版本") }
    static var insideTemp: String { tr("inside_temp", "车内温度") }
    static var outsideTemp: String { tr("outside_temp", "车外温度") }
    static var location: String { tr("location", "位置") }
    static var coordinates: String { tr("coordinates", "坐标") }
    static var locked: String { tr("locked", "已锁车") }
    static var unlocked: String { tr("unlocked", "未锁车") }
    static var sentryOn: String { tr("sentry_on", "哨兵已开启") }
    static var sentryOff: String { tr("sentry_off", "哨兵已关闭") }
    static var stateOnline: String { tr("state_online", "在线") }
    static var stateAsleep: String { tr("state_asleep", "休眠") }
    static var stateOffline: String { tr("state_offline", "离线") }
    static var stateCharging: String { tr("state_charging", "充电中") }
    static var stateDriving: String { tr("state_driving", "行驶中") }
    static var stateSuspended: String { tr("state_suspended", "已挂起") }
    static var stateUpdating: String { tr("state_updating", "更新中") }
    static var stateUnknown: String { tr("state_unknown", "未知") }
    static var pluggedIn: String { tr("plugged_in", "已插枪") }
    static var notPlugged: String { tr("not_plugged", "未插枪") }
    static var chargingSection: String { tr("charging_section", "充电") }
    static var chargingStateCharging: String { tr("charging_state_charging", "正在充电") }
    static var chargingStateComplete: String { tr("charging_state_complete", "充电完成") }
    static var chargingStateStopped: String { tr("charging_state_stopped", "已停止") }
    static var chargingStateDisconnected: String { tr("charging_state_disconnected", "未连接") }
    static var chargingStateNoPower: String { tr("charging_state_nopower", "无供电") }
    static var chargingStateStarting: String { tr("charging_state_starting", "启动中") }
    static var chargerPower: String { tr("charger_power", "充电功率") }
    static var chargeLimit: String { tr("charge_limit", "充电上限") }
    static var energyAdded: String { tr("energy_added", "充入电量") }
    static var energyUsed: String { tr("energy_used", "消耗电量") }
    static var timeToFull: String { tr("time_to_full", "充满还需") }
    static var chargePortOpen: String { tr("charge_port_open", "充电口已打开") }
    static var windowsOpen: String { tr("windows_open", "有车窗打开") }
    static var doorsOpen: String { tr("doors_open", "有车门打开") }
    static var trunkOpen: String { tr("trunk_open", "后备箱打开") }
    static var frunkOpen: String { tr("frunk_open", "前备箱打开") }
    static var climateOn: String { tr("climate_on", "空调开启") }
    static var climateOff: String { tr("climate_off", "空调关闭") }
    static var preconditioning: String { tr("preconditioning", "正在预处理电池") }
    static var userPresent: String { tr("user_present", "车内有人") }
    static var loggerUnhealthy: String { tr("logger_unhealthy", "TeslaMate 记录器报告这辆车当前不健康，状态可能不是最新的。") }
    static var asleepHint: String { tr("asleep_hint", "车辆正在休眠。下面是 MQTT 上一次上报的数据，不会为了刷新而唤醒车辆。") }
    static var shift: String { tr("shift", "挡位") }
    static var speed: String { tr("speed", "车速") }
    static var power: String { tr("power", "功率") }
    static var elevation: String { tr("elevation", "海拔") }
    static var tpms: String { tr("tpms", "胎压") }
    static var tpmsFl: String { tr("tpms_fl", "左前") }
    static var tpmsFr: String { tr("tpms_fr", "右前") }
    static var tpmsRl: String { tr("tpms_rl", "左后") }
    static var tpmsRr: String { tr("tpms_rr", "右后") }
    static var drivingSection: String { tr("driving_section", "行驶") }
    static var vehicleSection: String { tr("vehicle_section", "车辆") }
    static var climateSection: String { tr("climate_section", "温度") }
    static var drivesTitle: String { tr("drives_title", "行程") }
    static var chargesTitle: String { tr("charges_title", "充电") }
    static var driveDetailTitle: String { tr("drive_detail_title", "行程详情") }
    static var chargeDetailTitle: String { tr("charge_detail_title", "充电详情") }
    static var distance: String { tr("distance", "距离") }
    static var duration: String { tr("duration", "时长") }
    static var speedMax: String { tr("speed_max", "最高速度") }
    static var speedAvg: String { tr("speed_avg", "平均速度") }
    static var powerMax: String { tr("power_max", "最大功率") }
    static var powerMin: String { tr("power_min", "最低功率") }
    static var consumption: String { tr("consumption", "净能耗") }
    static var energyConsumed: String { tr("energy_consumed", "净耗电") }
    static var cost: String { tr("cost", "费用") }
    static var costHint: String { tr("cost_hint", "金额按 TeslaMate 里记录的数字显示，接口不返回币种。") }
    static var batteryChange: String { tr("battery_change", "电量变化") }
    static var rangeRatedChange: String { tr("range_rated_change", "额定续航变化") }
    static var startPlace: String { tr("start_place", "出发") }
    static var endPlace: String { tr("end_place", "到达") }
    static var address: String { tr("address", "地点") }
    static var unknownPlace: String { tr("unknown_place", "未知地点") }
    static var selectCar: String { tr("select_car", "选择车辆") }
    static var modelLabel: String { tr("model_label", "车型") }
    static var timeRange: String { tr("time_range", "时间") }
    static var supplyLabel: String { tr("supply_label", "供电") }
    static var missingDetail: String { tr("missing_detail", "没有找到这条记录") }
    static var showToken: String { tr("show_token", "显示令牌") }
    static var hideToken: String { tr("hide_token", "隐藏令牌") }
    static var dash: String { tr("dash", "—") }
    static var peakCharge: String { tr("peak_charge", "峰值充电") }
    static var softWarning: String { tr("soft_warning", "偏低") }

    static func connectedCars(_ count: Int) -> String {
        tr("connected_cars", "已连接，找到 %lld 辆车", Int64(count))
    }

    static func errorHTTP(_ code: Int) -> String {
        tr("error_http", "请求失败（HTTP %lld）", Int64(code))
    }

    static func errorHTTP(_ code: Int, message: String) -> String {
        tr("error_http_message", "请求失败（HTTP %lld）：%@", Int64(code), message)
    }

    static func errorAPI(_ message: String) -> String {
        tr("error_api", "接口返回错误：%@", message)
    }

    static func usableBattery(_ level: Int) -> String {
        tr("usable_battery", "可用电量 %lld%%", Int64(level))
    }

    static func updateAvailable(_ version: String) -> String {
        tr("update_available", "可更新到 %@", version)
    }

    static func hoursMinutes(_ hours: Int, _ minutes: Int) -> String {
        tr("hours_minutes", "%lld 小时 %lld 分钟", Int64(hours), Int64(minutes))
    }

    static func minutesOnly(_ minutes: Int) -> String {
        tr("minutes_only", "%lld 分钟", Int64(minutes))
    }

    static func trackPoints(_ count: Int) -> String {
        tr("track_points", "轨迹点 %lld 个", Int64(count))
    }

    static func sampleCount(_ count: Int) -> String {
        tr("sample_count", "采样 %lld 条", Int64(count))
    }

    static func since(_ date: String) -> String {
        tr("since", "状态更新于 %@", date)
    }

    static func carFallback(_ id: Int) -> String {
        tr("car_fallback", "车辆 %lld", Int64(id))
    }

    static func chargerDetail(voltage: Int, current: Int, phases: Int) -> String {
        tr("charger_detail", "%lld V · %lld A · %lld 相", Int64(voltage), Int64(current), Int64(phases))
    }

    static func drivesCount(_ count: Int) -> String {
        tr("drives_count", "%lld 次行程", Int64(count))
    }

    static func chargesCount(_ count: Int) -> String {
        tr("charges_count", "%lld 次充电", Int64(count))
    }

    static func message(for error: APIError) -> String {
        switch error {
        case .notConfigured:
            return errorNotConfigured
        case .invalidURL:
            return invalidURL
        case .api(let message):
            return errorAPI(message)
        case .http(let code, let serverMessage):
            if let serverMessage, !serverMessage.isEmpty {
                return errorHTTP(code, message: serverMessage)
            }
            return errorHTTP(code)
        case .network:
            return errorNetwork
        case .parse:
            return errorParse
        }
    }

    static func vehicleState(_ raw: String) -> String {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "online": return stateOnline
        case "asleep": return stateAsleep
        case "offline": return stateOffline
        case "charging": return stateCharging
        case "driving": return stateDriving
        case "suspended": return stateSuspended
        case "updating": return stateUpdating
        case "": return stateUnknown
        default: return raw
        }
    }

    static func chargingState(_ raw: String) -> String {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "charging": return chargingStateCharging
        case "complete": return chargingStateComplete
        case "stopped": return chargingStateStopped
        case "disconnected": return chargingStateDisconnected
        case "nopower": return chargingStateNoPower
        case "starting": return chargingStateStarting
        case "": return dash
        default: return raw
        }
    }

    static func place(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? unknownPlace : trimmed
    }

    static func date(_ raw: String) -> String {
        formatAPIDate(raw) ?? dash
    }

    static func duration(_ durationStr: String, minutes: Int) -> String {
        formatDuration(durationStr, minutes: minutes) ?? dash
    }

    static func timeToFull(_ hours: Double) -> String {
        guard let parts = splitHours(hours) else { return dash }
        if parts.hours > 0 {
            return hoursMinutes(parts.hours, parts.minutes)
        }
        return minutesOnly(parts.minutes)
    }

    static func carTitle(_ car: Car) -> String {
        let trimmed = car.name.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? carFallback(car.id) : trimmed
    }

    static func length(_ value: Double, units: Units, decimals: Int = 1) -> String {
        formatQuantity(value, decimals: decimals, unit: units.lengthLabel)
    }

    static func temperature(_ value: Double, units: Units) -> String {
        formatQuantity(value, decimals: 1, unit: units.temperatureLabel)
    }

    static func percent(_ value: Int) -> String {
        "\(value)%"
    }

    static func batteryArrow(_ start: Int, _ end: Int) -> String {
        "\(start)% → \(end)%"
    }

    static func rangeArrow(_ start: String, _ end: String) -> String {
        "\(start) → \(end)"
    }

    private static func tr(_ key: String, _ fallback: String, _ args: CVarArg...) -> String {
        let template = NSLocalizedString(key, value: fallback, comment: "")
        guard !args.isEmpty else { return template }
        return String(format: template, locale: Locale.current, arguments: args)
    }
}
