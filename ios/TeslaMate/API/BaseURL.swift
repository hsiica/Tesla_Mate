import Foundation

enum BaseURL {
    /// Accepts an origin such as `http://192.168.1.10:8080`.
    /// A pasted `/api/v1` or `/api/v1/cars` suffix is removed so requests are not doubled.
    static func normalize(_ input: String) -> String? {
        let trimmed = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard var components = URLComponents(string: trimmed),
              let scheme = components.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              let host = components.host,
              !host.isEmpty
        else {
            return nil
        }
        components.scheme = scheme
        var path = components.path
        while path.hasSuffix("/") {
            path.removeLast()
        }
        for suffix in ["/api/v1/cars", "/api/v1"] where path.hasSuffix(suffix) {
            path.removeLast(suffix.count)
            while path.hasSuffix("/") {
                path.removeLast()
            }
            break
        }
        components.path = path
        components.query = nil
        components.fragment = nil
        guard var result = components.string else { return nil }
        while result.hasSuffix("/") {
            result.removeLast()
        }
        return result
    }
}

struct APIEndpoint: Equatable, Sendable {
    var baseURL: String
    var token: String

    func url(path: String, query: [URLQueryItem] = []) -> URL? {
        guard var components = URLComponents(string: baseURL), components.host != nil else { return nil }
        let extra = path.hasPrefix("/") ? String(path.dropFirst()) : path
        var basePath = components.path
        if basePath == "/" { basePath = "" }
        while basePath.hasSuffix("/") {
            basePath.removeLast()
        }
        components.path = basePath + "/" + extra
        if !query.isEmpty {
            components.queryItems = query
        }
        return components.url
    }
}
