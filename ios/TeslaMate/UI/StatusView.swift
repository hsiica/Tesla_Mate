import SwiftUI

@MainActor
@Observable
final class StatusModel {
    var status: CarStatus?
    var isLoading = false
    var error: APIError?
    private var generation = 0
    private var loadedCarID: Int?

    func load(carID: Int?, app: AppModel) async {
        guard let carID else { return }
        if loadedCarID != carID {
            status = nil
            loadedCarID = carID
        }
        generation += 1
        let ticket = generation
        if status == nil { isLoading = true }
        error = nil
        defer { if ticket == generation { isLoading = false } }
        do {
            let value = try await app.client.status(app.endpoint, carID: carID)
            guard ticket == generation else { return }
            status = value
        } catch is CancellationError {
            return
        } catch let failure as APIError {
            guard ticket == generation else { return }
            error = failure
        } catch {
            guard ticket == generation else { return }
            self.error = .network
        }
    }
}

struct StatusView: View {
    var isActive: Bool

    @Environment(AppModel.self) private var app
    @State private var model: StatusModel

    init(isActive: Bool, model: StatusModel = StatusModel()) {
        self.isActive = isActive
        _model = State(initialValue: model)
    }

    var body: some View {
        NavigationStack {
            content
        }
    }

    private var content: some View {
        Group {
            if app.carsLoading && app.cars.isEmpty && model.status == nil {
                LoadingView()
            } else if let error = app.carsError, app.cars.isEmpty, model.status == nil {
                ErrorView(title: L10n.navStatus, error: error) {
                    Task { await app.refreshCars() }
                }
            } else if app.didLoadCars && app.cars.isEmpty {
                EmptyStateView(
                    title: L10n.emptyCarsTitle,
                    systemImage: "car",
                    description: L10n.emptyCarsBody
                ) {
                    Task { await app.refreshCars() }
                }
            } else if model.isLoading && model.status == nil {
                LoadingView()
            } else if let error = model.error, model.status == nil {
                ErrorView(title: L10n.navStatus, error: error) {
                    Task { await reload() }
                }
            } else if let status = model.status {
                VStack(spacing: 0) {
                    if let error = model.error {
                        NoticeBanner(text: L10n.message(for: error), systemImage: "wifi.exclamationmark", tint: .red)
                            .padding([.horizontal, .top], 16)
                    }
                    StatusBody(status: status)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else {
                LoadingView()
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                CarPicker(fallback: L10n.navStatus)
            }
        }
        .refreshable {
            await app.refreshCars()
            await reload()
        }
        .task(id: loadKey) {
            guard isActive else { return }
            await reload()
        }
    }

    private var loadKey: String {
        "\(isActive)-\(app.selectedCar?.id ?? 0)-\(app.didLoadCars)"
    }

    private func reload() async {
        await model.load(carID: app.selectedCar?.id, app: app)
    }
}

private struct StatusBody: View {
    var status: CarStatus

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if !status.healthy {
                    NoticeBanner(text: L10n.loggerUnhealthy, systemImage: "exclamationmark.triangle", tint: .red)
                }
                if status.state.caseInsensitiveCompare("asleep") == .orderedSame {
                    NoticeBanner(text: L10n.asleepHint, systemImage: "moon.zzz", tint: .secondary)
                }
                batteryCard
                chargingCard
                climateCard
                vehicleCard
                if status.speed > 0 || !status.shiftState.isEmpty {
                    drivingCard
                }
                if status.tpmsFl > 0 || status.tpmsFr > 0 || status.tpmsRl > 0 || status.tpmsRr > 0 {
                    tpmsCard
                }
            }
            .padding(16)
            .frame(maxWidth: 720)
            .frame(maxWidth: .infinity)
        }
    }

    private var batteryCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(L10n.vehicleState(status.state))
                    .font(.headline)
                    .foregroundStyle(Color.accentColor)
                Spacer()
                let since = L10n.date(status.stateSince)
                if since != L10n.dash {
                    Text(L10n.since(since))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("\(status.batteryLevel)")
                    .font(.system(size: 56, weight: .semibold, design: .rounded))
                    .foregroundStyle(batteryTint(status.batteryLevel))
                Text("%")
                    .font(.title2)
                    .foregroundStyle(.secondary)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(L10n.batteryLevel) \(status.batteryLevel)%")
            Text(L10n.usableBattery(status.usableBatteryLevel))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            ProgressView(value: Double(min(max(status.batteryLevel, 0), 100)), total: 100)
                .tint(batteryTint(status.batteryLevel))
            InfoRow(label: L10n.ratedRange, value: L10n.length(status.ratedRange, units: status.units, decimals: 0))
            InfoRow(label: L10n.estRange, value: L10n.length(status.estRange, units: status.units, decimals: 0))
            InfoRow(label: L10n.idealRange, value: L10n.length(status.idealRange, units: status.units, decimals: 0))
        }
        .cardStyle()
    }

    private var chargingCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.chargingSection).font(.headline)
            InfoRow(label: L10n.chargingSection, value: status.pluggedIn ? L10n.pluggedIn : L10n.notPlugged)
            if !status.chargingState.isEmpty {
                InfoRow(label: L10n.stateCharging, value: L10n.chargingState(status.chargingState))
            }
            if status.pluggedIn || status.chargerPowerKw > 0 {
                InfoRow(label: L10n.chargerPower, value: formatQuantity(status.chargerPowerKw, decimals: 1, unit: "kW"))
                InfoRow(label: L10n.energyAdded, value: formatQuantity(status.chargeEnergyAdded, decimals: 1, unit: "kWh"))
                InfoRow(label: L10n.chargeLimit, value: L10n.percent(status.chargeLimitSoc))
                InfoRow(label: L10n.timeToFull, value: L10n.timeToFull(status.timeToFullHours))
                if status.chargerVoltage > 0 || status.chargerCurrent > 0 {
                    InfoRow(
                        label: L10n.supplyLabel,
                        value: L10n.chargerDetail(
                            voltage: status.chargerVoltage,
                            current: Int(status.chargerCurrent.rounded()),
                            phases: status.chargerPhases
                        )
                    )
                }
                if status.chargePortOpen {
                    Text(L10n.chargePortOpen)
                        .font(.subheadline)
                }
            }
        }
        .cardStyle()
    }

    private var climateCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.climateSection).font(.headline)
            InfoRow(label: L10n.insideTemp, value: L10n.temperature(status.insideTemp, units: status.units))
            InfoRow(label: L10n.outsideTemp, value: L10n.temperature(status.outsideTemp, units: status.units))
            InfoRow(label: L10n.climateSection, value: status.climateOn ? L10n.climateOn : L10n.climateOff)
            if status.preconditioning {
                Text(L10n.preconditioning).font(.subheadline)
            }
        }
        .cardStyle()
    }

    private var vehicleCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.vehicleSection).font(.headline)
            let model = [formatTeslaModel(status.model), status.trim]
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
                .joined(separator: " ")
            if !model.isEmpty {
                InfoRow(label: L10n.modelLabel, value: model)
            }
            InfoRow(label: L10n.odometer, value: L10n.length(status.odometer, units: status.units, decimals: 1))
            InfoRow(
                label: L10n.software,
                value: status.softwareVersion.isEmpty ? L10n.dash : status.softwareVersion
            )
            if status.updateAvailable && !status.updateVersion.isEmpty {
                Text(L10n.updateAvailable(status.updateVersion))
                    .font(.subheadline)
                    .foregroundStyle(Color.accentColor)
            }
            InfoRow(label: L10n.location, value: status.geofence.isEmpty ? L10n.dash : status.geofence)
            InfoRow(
                label: L10n.coordinates,
                value: formatCoordinates(status.latitude, status.longitude) ?? L10n.dash
            )
            InfoRow(label: L10n.locked, value: status.locked ? L10n.locked : L10n.unlocked)
            InfoRow(label: L10n.sentryOn, value: status.sentryMode ? L10n.sentryOn : L10n.sentryOff)
            flagRow
        }
        .cardStyle()
    }

    @ViewBuilder
    private var flagRow: some View {
        let flags: [(String, String)] = [
            status.windowsOpen ? (L10n.windowsOpen, "macwindow") : nil,
            status.doorsOpen ? (L10n.doorsOpen, "rectangle.lefthalf.filled") : nil,
            status.trunkOpen ? (L10n.trunkOpen, "arrow.up.bin") : nil,
            status.frunkOpen ? (L10n.frunkOpen, "arrow.up.bin.fill") : nil,
            status.userPresent ? (L10n.userPresent, "person.fill") : nil,
        ].compactMap { $0 }
        if !flags.isEmpty {
            FlowLayout(spacing: 8) {
                ForEach(flags, id: \.0) { flag in
                    FlagChip(title: flag.0, systemImage: flag.1)
                }
            }
        }
    }

    private var drivingCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.drivingSection).font(.headline)
            InfoRow(label: L10n.shift, value: status.shiftState.isEmpty ? L10n.dash : status.shiftState)
            InfoRow(label: L10n.speed, value: formatSpeed(Double(status.speed), units: status.units))
            InfoRow(label: L10n.power, value: formatQuantity(Double(status.powerKw), decimals: 0, unit: "kW"))
            InfoRow(label: L10n.elevation, value: formatQuantity(Double(status.elevation), decimals: 0, unit: "m"))
        }
        .cardStyle()
    }

    private var tpmsCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.tpms).font(.headline)
            pressureRow(L10n.tpmsFl, status.tpmsFl, soft: status.tpmsSoftFl)
            pressureRow(L10n.tpmsFr, status.tpmsFr, soft: status.tpmsSoftFr)
            pressureRow(L10n.tpmsRl, status.tpmsRl, soft: status.tpmsSoftRl)
            pressureRow(L10n.tpmsRr, status.tpmsRr, soft: status.tpmsSoftRr)
        }
        .cardStyle()
    }

    private func pressureRow(_ label: String, _ value: Double, soft: Bool) -> some View {
        let text = formatQuantity(value, decimals: 2, unit: status.units.pressureLabel)
        return InfoRow(
            label: label,
            value: soft ? "\(text) · \(L10n.softWarning)" : text,
            valueColor: soft ? .orange : .primary
        )
    }
}

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 320
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(
                at: CGPoint(x: x, y: y),
                proposal: ProposedViewSize(width: size.width, height: size.height)
            )
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

private extension View {
    func cardStyle() -> some View {
        padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
