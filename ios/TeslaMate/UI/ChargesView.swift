import SwiftUI

private struct ChargeRoute: Hashable {
    var carID: Int
    var chargeID: Int
}

@MainActor
@Observable
final class ChargeListModel {
    var charges: [ChargeSummary] = []
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
            charges = []
            hasLoaded = false
            endReached = false
            self.carID = carID
        }
        generation += 1
        let ticket = generation
        if charges.isEmpty { isLoading = true }
        error = nil
        defer { if ticket == generation { isLoading = false } }
        do {
            let result = try await app.client.charges(app.endpoint, carID: carID, page: 1)
            guard ticket == generation else { return }
            charges = result.charges
            units = result.units
            page = 1
            endReached = result.charges.count < TeslaMateClient.pageSize
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
            let result = try await app.client.charges(app.endpoint, carID: carID, page: next)
            guard ticket == generation else { return }
            let existing = Set(charges.map(\.id))
            let fresh = result.charges.filter { !existing.contains($0.id) }
            if fresh.isEmpty || result.charges.count < TeslaMateClient.pageSize {
                endReached = true
            }
            if !fresh.isEmpty {
                charges.append(contentsOf: fresh)
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

struct ChargesView: View {
    var isActive: Bool

    @Environment(AppModel.self) private var app
    @State private var model = ChargeListModel()
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            content
                .navigationDestination(for: ChargeRoute.self) { route in
                    ChargeDetailView(carID: route.carID, chargeID: route.chargeID)
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
                ErrorView(title: L10n.chargesTitle, error: error) {
                    Task { await app.refreshCars() }
                }
            } else if app.didLoadCars && app.cars.isEmpty {
                EmptyStateView(title: L10n.emptyCarsTitle, systemImage: "car", description: L10n.emptyCarsBody) {
                    Task { await app.refreshCars() }
                }
            } else if model.isLoading && model.charges.isEmpty {
                LoadingView()
            } else if let error = model.error, model.charges.isEmpty {
                ErrorView(title: L10n.chargesTitle, error: error) {
                    Task { await reload() }
                }
            } else if model.hasLoaded && model.charges.isEmpty {
                EmptyStateView(title: L10n.emptyCharges, systemImage: "bolt.slash", description: nil) {
                    Task { await reload() }
                }
            } else {
                chargeList
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                CarPicker(fallback: L10n.chargesTitle)
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

    private var chargeList: some View {
        List {
            if let error = model.error {
                NoticeBanner(text: L10n.message(for: error), systemImage: "wifi.exclamationmark", tint: .red)
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                    .listRowBackground(Color.clear)
            }
            ForEach(model.charges) { charge in
                if let carID = app.selectedCar?.id {
                    NavigationLink(value: ChargeRoute(carID: carID, chargeID: charge.id)) {
                        ChargeRow(charge: charge, units: model.units)
                    }
                } else {
                    ChargeRow(charge: charge, units: model.units)
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

private struct ChargeRow: View {
    var charge: ChargeSummary
    var units: Units

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L10n.date(charge.startDate))
                .font(.headline)
            Text(L10n.place(charge.address))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(
                [
                    formatQuantity(charge.energyAdded, decimals: 1, unit: "kWh"),
                    L10n.duration(charge.durationStr, minutes: charge.durationMin),
                    formatFixed(charge.cost, decimals: 2),
                ].joined(separator: " · ")
            )
            .font(.subheadline)
            Text(L10n.batteryArrow(charge.startBattery, charge.endBattery))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(L10n.length(charge.odometer, units: units, decimals: 0))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

struct ChargeDetailView: View {
    var carID: Int
    var chargeID: Int

    @Environment(AppModel.self) private var app
    @State private var detail: ChargeDetail?
    @State private var error: APIError?
    @State private var loading = true

    var body: some View {
        Group {
            if loading && detail == nil {
                LoadingView()
            } else if let error, detail == nil {
                ErrorView(title: L10n.chargeDetailTitle, error: error) {
                    Task { await load() }
                }
            } else if let detail {
                ChargeDetailBody(detail: detail)
            } else {
                EmptyStateView(title: L10n.missingDetail, systemImage: "questionmark.circle", description: nil, retry: nil)
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(L10n.chargeDetailTitle)
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await load() }
        .task { await load() }
    }

    private func load() async {
        if detail == nil { loading = true }
        error = nil
        defer { loading = false }
        do {
            detail = try await app.client.charge(app.endpoint, carID: carID, chargeID: chargeID)
        } catch is CancellationError {
            return
        } catch let failure as APIError {
            if detail == nil { error = failure }
        } catch {
            if detail == nil { self.error = .network }
        }
    }
}

private struct ChargeDetailBody: View {
    var detail: ChargeDetail

    private var charge: ChargeSummary { detail.charge }
    private var units: Units { detail.units }

    var body: some View {
        List {
            Section(L10n.chargeDetailTitle) {
                InfoRow(label: L10n.address, value: L10n.place(charge.address))
                InfoRow(label: L10n.timeRange, value: "\(L10n.date(charge.startDate)) – \(L10n.date(charge.endDate))")
                InfoRow(label: L10n.duration, value: L10n.duration(charge.durationStr, minutes: charge.durationMin))
                InfoRow(label: L10n.energyAdded, value: formatQuantity(charge.energyAdded, decimals: 2, unit: "kWh"))
                InfoRow(label: L10n.energyUsed, value: formatQuantity(charge.energyUsed, decimals: 2, unit: "kWh"))
                InfoRow(label: L10n.cost, value: formatFixed(charge.cost, decimals: 2))
                Text(L10n.costHint)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                InfoRow(label: L10n.batteryChange, value: L10n.batteryArrow(charge.startBattery, charge.endBattery))
                InfoRow(
                    label: L10n.rangeRatedChange,
                    value: L10n.rangeArrow(
                        L10n.length(charge.ratedRangeStart, units: units, decimals: 0),
                        L10n.length(charge.ratedRangeEnd, units: units, decimals: 0)
                    )
                )
                InfoRow(label: L10n.outsideTemp, value: L10n.temperature(charge.outsideTemp, units: units))
                InfoRow(label: L10n.odometer, value: L10n.length(charge.odometer, units: units, decimals: 1))
                InfoRow(
                    label: L10n.coordinates,
                    value: formatCoordinates(charge.latitude, charge.longitude) ?? L10n.dash
                )
                if detail.sampleCount > 0 {
                    Text(L10n.sampleCount(detail.sampleCount))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                if let peak = detail.chargerPeak {
                    InfoRow(label: L10n.peakCharge, value: formatQuantity(Double(peak.powerKw), decimals: 0, unit: "kW"))
                    InfoRow(
                        label: L10n.supplyLabel,
                        value: L10n.chargerDetail(voltage: peak.voltage, current: peak.current, phases: peak.phases)
                    )
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}
