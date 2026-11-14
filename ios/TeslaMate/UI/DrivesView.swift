import SwiftUI

private struct DriveRoute: Hashable {
    var carID: Int
    var driveID: Int
}

@MainActor
@Observable
final class DriveListModel {
    var drives: [DriveSummary] = []
    var units: Units = .china
    var isLoading = false
    var isLoadingMore = false
    var error: APIError?
    var moreError: APIError?
    var endReached = false
    var hasLoaded = false
    private var page = 1
    private var carID: Int?
    private var generation = 0

    func reload(carID: Int?, app: AppModel) async {
        guard let carID else { return }
        if self.carID != carID {
            drives = []
            hasLoaded = false
            endReached = false
            self.carID = carID
        }
        generation += 1
        let ticket = generation
        if drives.isEmpty { isLoading = true }
        error = nil
        defer { if ticket == generation { isLoading = false } }
        do {
            let result = try await app.client.drives(app.endpoint, carID: carID, page: 1)
            guard ticket == generation else { return }
            drives = result.drives
            units = result.units
            page = 1
            endReached = result.drives.count < TeslaMateClient.pageSize
            hasLoaded = true
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

    func loadMore(app: AppModel) async {
        guard hasLoaded, !endReached, !isLoading, !isLoadingMore, let carID else { return }
        isLoadingMore = true
        moreError = nil
        let next = page + 1
        let ticket = generation
        defer { isLoadingMore = false }
        do {
            let result = try await app.client.drives(app.endpoint, carID: carID, page: next)
            guard ticket == generation else { return }
            let existing = Set(drives.map(\.id))
            let fresh = result.drives.filter { !existing.contains($0.id) }
            if fresh.isEmpty || result.drives.count < TeslaMateClient.pageSize {
                endReached = true
            }
            if !fresh.isEmpty {
                drives.append(contentsOf: fresh)
                page = next
            }
            units = result.units
        } catch is CancellationError {
            return
        } catch let failure as APIError {
            guard ticket == generation else { return }
            moreError = failure
        } catch {
            guard ticket == generation else { return }
            moreError = .network
        }
    }
}

struct DrivesView: View {
    var isActive: Bool

    @Environment(AppModel.self) private var app
    @State private var model = DriveListModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .navigationDestination(for: DriveRoute.self) { route in
                    DriveDetailView(carID: route.carID, driveID: route.driveID)
                }
        }
        .onChange(of: app.selectedCarID) { _, _ in
            path = NavigationPath()
        }
    }

    @ViewBuilder
    private var content: some View {
        Group {
            if app.carsLoading && app.cars.isEmpty {
                LoadingView()
            } else if let error = app.carsError, app.cars.isEmpty {
                ErrorView(title: L10n.drivesTitle, error: error) {
                    Task { await app.refreshCars() }
                }
            } else if app.didLoadCars && app.cars.isEmpty {
                EmptyStateView(title: L10n.emptyCarsTitle, systemImage: "car", description: L10n.emptyCarsBody) {
                    Task { await app.refreshCars() }
                }
            } else if model.isLoading && model.drives.isEmpty {
                LoadingView()
            } else if let error = model.error, model.drives.isEmpty {
                ErrorView(title: L10n.drivesTitle, error: error) {
                    Task { await reload() }
                }
            } else if model.hasLoaded && model.drives.isEmpty {
                EmptyStateView(title: L10n.emptyDrives, systemImage: "road.lanes", description: nil) {
                    Task { await reload() }
                }
            } else {
                driveList
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                CarPicker(fallback: L10n.drivesTitle)
            }
        }
        .refreshable {
            await app.refreshCars()
            await reload()
        }
        .task(id: "\(isActive)-\(app.selectedCar?.id ?? 0)-\(app.didLoadCars)") {
            guard isActive else { return }
            await reload()
        }
    }

    private var driveList: some View {
        List {
            if let error = model.error {
                NoticeBanner(text: L10n.message(for: error), systemImage: "wifi.exclamationmark", tint: .red)
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                    .listRowBackground(Color.clear)
            }
            ForEach(model.drives) { drive in
                if let carID = app.selectedCar?.id {
                    NavigationLink(value: DriveRoute(carID: carID, driveID: drive.id)) {
                        DriveRow(drive: drive, units: model.units)
                    }
                } else {
                    DriveRow(drive: drive, units: model.units)
                }
            }
            if model.hasLoaded && !model.endReached {
                moreRow
            }
        }
        .listStyle(.plain)
    }

    private var moreRow: some View {
        HStack {
            Spacer()
            if model.isLoadingMore {
                ProgressView()
                Text(L10n.loadingMore)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            } else if let moreError = model.moreError {
                VStack(spacing: 8) {
                    Text(L10n.message(for: moreError))
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .multilineTextAlignment(.center)
                    Button(L10n.retry) {
                        Task { await model.loadMore(app: app) }
                    }
                }
            } else {
                Button(L10n.loadMore) {
                    Task { await model.loadMore(app: app) }
                }
            }
            Spacer()
        }
        .padding(.vertical, 8)
    }

    private func reload() async {
        await model.reload(carID: app.selectedCar?.id, app: app)
    }
}

private struct DriveRow: View {
    var drive: DriveSummary
    var units: Units

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L10n.date(drive.startDate))
                .font(.headline)
            Text(L10n.rangeArrow(L10n.place(drive.startAddress), L10n.place(drive.endAddress)))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(summary)
                .font(.subheadline)
            Text(L10n.batteryArrow(drive.startBattery, drive.endBattery))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private var summary: String {
        var parts = [
            L10n.length(drive.distance, units: units, decimals: 1),
            L10n.duration(drive.durationStr, minutes: drive.durationMin),
        ]
        if let energy = drive.energyConsumedNet {
            parts.append(formatQuantity(energy, decimals: 1, unit: "kWh"))
        }
        return parts.joined(separator: " · ")
    }
}

struct DriveDetailView: View {
    var carID: Int
    var driveID: Int

    @Environment(AppModel.self) private var app
    @State private var detail: DriveDetail?
    @State private var error: APIError?
    @State private var loading = true

    var body: some View {
        Group {
            if loading && detail == nil {
                LoadingView()
            } else if let error, detail == nil {
                ErrorView(title: L10n.driveDetailTitle, error: error) {
                    Task { await load() }
                }
            } else if let detail {
                DriveDetailBody(detail: detail)
            } else {
                EmptyStateView(title: L10n.missingDetail, systemImage: "questionmark.circle", description: nil, retry: nil)
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(L10n.driveDetailTitle)
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await load() }
        .task { await load() }
    }

    private func load() async {
        if detail == nil { loading = true }
        error = nil
        defer { loading = false }
        do {
            detail = try await app.client.drive(app.endpoint, carID: carID, driveID: driveID)
        } catch is CancellationError {
            return
        } catch let failure as APIError {
            if detail == nil { error = failure }
        } catch {
            if detail == nil { self.error = .network }
        }
    }
}

private struct DriveDetailBody: View {
    var detail: DriveDetail

    private var drive: DriveSummary { detail.drive }
    private var units: Units { detail.units }

    var body: some View {
        List {
            Section(L10n.driveDetailTitle) {
                InfoRow(label: L10n.startPlace, value: L10n.place(drive.startAddress))
                InfoRow(label: L10n.endPlace, value: L10n.place(drive.endAddress))
                InfoRow(label: L10n.distance, value: L10n.length(drive.distance, units: units, decimals: 1))
                InfoRow(label: L10n.duration, value: L10n.duration(drive.durationStr, minutes: drive.durationMin))
                InfoRow(label: L10n.timeRange, value: "\(L10n.date(drive.startDate)) – \(L10n.date(drive.endDate))")
                if let energy = drive.energyConsumedNet {
                    InfoRow(label: L10n.energyConsumed, value: formatQuantity(energy, decimals: 2, unit: "kWh"))
                }
                if let consumption = drive.consumptionNet {
                    InfoRow(label: L10n.consumption, value: formatQuantity(consumption, decimals: 0, unit: units.consumptionLabel))
                }
                InfoRow(label: L10n.speedMax, value: formatSpeed(Double(drive.speedMax), units: units))
                InfoRow(label: L10n.speedAvg, value: formatSpeed(drive.speedAvg, units: units))
                InfoRow(label: L10n.powerMax, value: formatQuantity(Double(drive.powerMax), decimals: 0, unit: "kW"))
                InfoRow(label: L10n.powerMin, value: formatQuantity(Double(drive.powerMin), decimals: 0, unit: "kW"))
                InfoRow(label: L10n.batteryChange, value: L10n.batteryArrow(drive.startBattery, drive.endBattery))
                InfoRow(
                    label: L10n.rangeRatedChange,
                    value: L10n.rangeArrow(
                        L10n.length(drive.ratedRangeStart, units: units, decimals: 0),
                        L10n.length(drive.ratedRangeEnd, units: units, decimals: 0)
                    )
                )
                if let outside = drive.outsideTemp {
                    InfoRow(label: L10n.outsideTemp, value: L10n.temperature(outside, units: units))
                }
                if let inside = drive.insideTemp {
                    InfoRow(label: L10n.insideTemp, value: L10n.temperature(inside, units: units))
                }
                if detail.trackPointCount > 0 {
                    Text(L10n.trackPoints(detail.trackPointCount))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
