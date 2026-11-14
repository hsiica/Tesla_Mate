import Foundation

enum APIError: Error, Equatable, Sendable {
    case notConfigured
    case invalidURL
    /// HTTP 200 body `{"error":"..."}`, which is how TeslaMateApi reports a failed query.
    case api(String)
    case http(Int, String?)
    case network
    case parse
}
