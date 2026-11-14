import SwiftUI

struct SettingsView: View {
    var onboarding: Bool

    @Environment(AppModel.self) private var app
    @State private var url = ""
    @State private var token = ""
    @State private var loaded = false
    @State private var saving = false
    @State private var invalidURL = false
    @State private var revealToken = false
    @State private var confirmClear = false
    @State private var error: APIError?
    @State private var connectedCount: Int?

    var body: some View {
        Group {
            if onboarding {
                form
            } else {
                NavigationStack { form }
            }
        }
    }

    private var form: some View {
        Form {
            if onboarding {
                Section {
                    Text(L10n.onboardingBody)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Section {
                TextField(L10n.baseURLLabel, text: $url, prompt: Text(L10n.baseURLPlaceholder))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .textContentType(.URL)
                    .submitLabel(.done)
                if invalidURL {
                    Text(L10n.invalidURL)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            } header: {
                Text(L10n.baseURLLabel)
            } footer: {
                Text(L10n.baseURLHelp)
            }

            Section {
                Group {
                    if revealToken {
                        TextField(L10n.apiTokenLabel, text: $token, prompt: Text(L10n.apiTokenPlaceholder))
                    } else {
                        SecureField(L10n.apiTokenLabel, text: $token, prompt: Text(L10n.apiTokenPlaceholder))
                    }
                }
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.done)

                Button(revealToken ? L10n.hideToken : L10n.showToken) {
                    revealToken.toggle()
                }
                .font(.footnote)

                if tokenWarning {
                    Text(L10n.tokenShortWarning)
                        .font(.footnote)
                        .foregroundStyle(.orange)
                }
            } header: {
                Text(L10n.apiTokenLabel)
            } footer: {
                Text(L10n.apiTokenHelp)
            }

            if usesHTTP {
                Section {
                    Label(L10n.httpNotice, systemImage: "lock.open")
                        .font(.footnote)
                        .foregroundStyle(.orange)
                }
            }

            Section {
                Button {
                    Task { await save() }
                } label: {
                    if saving {
                        HStack {
                            ProgressView()
                            Text(L10n.saving)
                        }
                    } else {
                        Text(L10n.saveAndConnect)
                            .frame(maxWidth: .infinity)
                    }
                }
                .disabled(saving || url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                if let connectedCount {
                    Text(L10n.connectedCars(connectedCount))
                        .foregroundStyle(.green)
                }
                if let error {
                    Text(L10n.message(for: error))
                        .foregroundStyle(.red)
                }
            }

            if !onboarding && app.settings.isConfigured {
                Section {
                    Button(L10n.clearSettings, role: .destructive) {
                        confirmClear = true
                    }
                }
            }
        }
        .navigationTitle(onboarding ? L10n.onboardingTitle : L10n.settingsTitle)
        .navigationBarTitleDisplayMode(onboarding ? .large : .inline)
        .scrollDismissesKeyboard(.interactively)
        .onAppear(perform: loadOnce)
        .onChange(of: url) { _, _ in
            invalidURL = false
            connectedCount = nil
        }
        .onChange(of: token) { _, _ in
            connectedCount = nil
        }
        .confirmationDialog(L10n.clearSettingsTitle, isPresented: $confirmClear, titleVisibility: .visible) {
            Button(L10n.clearConfirm, role: .destructive) {
                app.clearSettings()
            }
            Button(L10n.cancel, role: .cancel) {}
        } message: {
            Text(L10n.clearSettingsBody)
        }
    }

    private var usesHTTP: Bool {
        url.trimmingCharacters(in: .whitespacesAndNewlines).lowercased().hasPrefix("http://")
    }

    private var tokenWarning: Bool {
        let count = token.trimmingCharacters(in: .whitespacesAndNewlines).count
        return count > 0 && count < 32
    }

    private func loadOnce() {
        guard !loaded else { return }
        url = app.settings.baseURL
        token = app.settings.token
        loaded = true
    }

    private func save() async {
        saving = true
        invalidURL = false
        error = nil
        connectedCount = nil
        defer { saving = false }
        let result = await app.connect(url: url, token: token)
        if let normalized = BaseURL.normalize(url) {
            url = normalized
        }
        if case .invalidURL = result {
            invalidURL = true
            return
        }
        error = result
        if result == nil {
            connectedCount = app.cars.count
        }
    }
}
