import SwiftUI

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
            Text(L10n.loading)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorView: View {
    var title: String
    var error: APIError
    var retry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: "wifi.exclamationmark")
        } description: {
            Text(L10n.message(for: error))
        } actions: {
            Button(L10n.retry, action: retry)
                .buttonStyle(.borderedProminent)
        }
    }
}

struct EmptyStateView: View {
    var title: String
    var systemImage: String
    var description: String?
    var retry: (() -> Void)?

    var body: some View {
        ContentUnavailableView {
            Label(title, systemImage: systemImage)
        } description: {
            if let description {
                Text(description)
            }
        } actions: {
            if let retry {
                Button(L10n.retry, action: retry)
                    .buttonStyle(.borderedProminent)
            }
        }
    }
}

struct InfoRow: View {
    var label: String
    var value: String
    var valueColor: Color = .primary

    var body: some View {
        LabeledContent {
            Text(value)
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.trailing)
        } label: {
            Text(label)
        }
        .accessibilityElement(children: .combine)
    }
}

struct NoticeBanner: View {
    var text: String
    var systemImage: String
    var tint: Color

    var body: some View {
        Label(text, systemImage: systemImage)
            .font(.subheadline)
            .foregroundStyle(tint)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct FlagChip: View {
    var title: String
    var systemImage: String

    var body: some View {
        Label(title, systemImage: systemImage)
            .font(.footnote.weight(.medium))
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color.accentColor.opacity(0.12), in: Capsule())
    }
}

struct CarPicker: View {
    @Environment(AppModel.self) private var app
    var fallback: String

    var body: some View {
        if app.cars.count > 1 {
            Menu {
                ForEach(app.cars) { car in
                    Button {
                        app.selectCar(car.id)
                    } label: {
                        if car.id == app.selectedCar?.id {
                            Label(L10n.carTitle(car), systemImage: "checkmark")
                        } else {
                            Text(L10n.carTitle(car))
                        }
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .lineLimit(1)
                    Image(systemName: "chevron.down")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                .accessibilityLabel(L10n.selectCar)
            }
        } else {
            Text(title)
                .font(.headline)
                .lineLimit(1)
        }
    }

    private var title: String {
        if let car = app.selectedCar {
            return L10n.carTitle(car)
        }
        return fallback
    }
}

func batteryTint(_ level: Int) -> Color {
    switch level {
    case ..<20: return .red
    case ..<50: return .orange
    default: return .green
    }
}
