import SwiftUI

struct RootView: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        Group {
            if app.showMain {
                MainTabView()
            } else {
                NavigationStack {
                    SettingsView(onboarding: true)
                }
            }
        }
    }
}

private struct MainTabView: View {
    @Environment(AppModel.self) private var app
    @State private var tab = Tab.status

    var body: some View {
        TabView(selection: $tab) {
            StatusView(isActive: tab == .status)
                .tabItem { Label(L10n.navStatus, systemImage: "car.fill") }
                .tag(Tab.status)

            DrivesView(isActive: tab == .drives)
                .tabItem { Label(L10n.navDrives, systemImage: "point.topleft.down.to.point.bottomright.curvepath") }
                .tag(Tab.drives)

            ChargesView(isActive: tab == .charges)
                .tabItem { Label(L10n.navCharges, systemImage: "bolt.fill") }
                .tag(Tab.charges)

            SettingsView(onboarding: false)
                .tabItem { Label(L10n.navSettings, systemImage: "gearshape") }
                .tag(Tab.settings)
        }
        .task {
            await app.refreshCars()
        }
    }
}

private enum Tab: Hashable {
    case status
    case drives
    case charges
    case settings
}
