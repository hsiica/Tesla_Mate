import SwiftUI

@main
struct TeslaMateApp: App {
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
                .tint(Color("AccentColor"))
        }
    }
}
