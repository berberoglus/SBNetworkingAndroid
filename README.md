# SBNetworking-Android

A pure HTTP-infrastructure library for App Factory Android apps. The faithful Android port of
the iOS `SBNetworking` Swift package: a configured Retrofit/OkHttp stack, an `AuthTokenProvider`
with bearer/`apikey` header injection and reactive 401 refresh + retry, a uniform sealed
`HttpClientError`, and DTO→domain mapping helpers.

- Namespace: `com.berberoglus.sbnetworking`
- minSdk 28 · compileSdk/targetSdk 36 · JDK 21
- Stack: Retrofit + OkHttp + kotlinx-serialization + Coroutines
- DI-framework-agnostic core (plain constructable classes); an **optional** Hilt module is provided.

## Consuming app

Add `INTERNET` permission to the consuming app's manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Build an `HttpClient` and create your feature `*Api` from its `Retrofit`:

```kotlin
val httpClient = HttpClient.Builder(HttpClientEnvironment(baseUrl = "api.example.com"))
    .authTokenProvider(myTokenProvider)
    .enableLogging(BuildConfig.DEBUG)
    .build()

val authApi: AuthApi = httpClient.retrofit.create(AuthApi::class.java)
```

See `AppFactoryRules/Android/Data/Networking_Domain_Rules.md` for usage rules.
