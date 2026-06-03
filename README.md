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

## Optional Hilt wiring

This library does **not** depend on Hilt — `HttpClient` is a plain constructable class, so the
core is DI-framework-agnostic and never forces Hilt on consumers. If your app uses Hilt, drop this
module into your **app** (where the Hilt plugin is already applied) and provide an
`HttpClientEnvironment` (and optionally an `AuthTokenProvider`) binding in your graph:

```kotlin
import com.berberoglus.sbnetworking.AuthTokenProvider
import com.berberoglus.sbnetworking.HttpClient
import com.berberoglus.sbnetworking.HttpClientEnvironment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SBNetworkingModule {

    @Provides
    @Singleton
    fun provideHttpClient(
        environment: HttpClientEnvironment,
        authTokenProvider: AuthTokenProvider?,
    ): HttpClient = HttpClient.Builder(environment)
        .authTokenProvider(authTokenProvider)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(httpClient: HttpClient): Retrofit = httpClient.retrofit
}
```

Feature modules then create their own `*Api` from the injected `Retrofit`.
