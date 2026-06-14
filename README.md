# SBNetworking-Android

A lightweight, type-safe HTTP networking library for Android apps, built on Retrofit, OkHttp,
kotlinx-serialization, and Coroutines. It gives you a ready-to-use, auth-aware networking stack —
a configured `Retrofit`/`OkHttpClient`, automatic token-header injection with reactive 401 refresh,
a single uniform error type, and helpers for envelope/pagination decoding and DTO→domain mapping —
so feature modules only declare their own `*Api` interfaces and DTOs.

- Namespace: `com.berberoglus.sbnetworking`
- minSdk 28 · compileSdk 36 · JDK 21
- Stack: Retrofit + OkHttp + kotlinx-serialization + Coroutines
- DI-framework-agnostic core (plain constructable classes); an **optional** Hilt snippet is provided.

## Features

- ✅ Configured Retrofit/OkHttp stack from a single `HttpClient.Builder`
- ✅ Type-safe, coroutine-based requests via your own Retrofit `*Api` interfaces
- ✅ Uniform sealed `HttpClientError` — every transport and HTTP failure maps to one type
- ✅ `AuthTokenProvider` with automatic `apikey` + `Authorization: Bearer` header injection
- ✅ Reactive 401 handling: refresh once, retry once, then surface the error
- ✅ Per-request body, headers, query parameters, and timeouts
- ✅ Dynamic, programmatic requests via `EndpointSpec` (no interface required)
- ✅ Generic `@Serializable` envelope/pagination DTOs
- ✅ DTO→domain mapping via free extension functions (or the optional `ModelConvertible` marker)
- ✅ Raw-bytes passthrough and empty-body (`204`) handling
- ✅ Opt-in request/response logging
- ✅ Easy to test with MockWebServer

## Requirements

- Android `minSdk` 28, `compileSdk` 36
- JDK 21
- AGP 9.2.1, Kotlin 2.3.21, Gradle 9.5.0

## Installation

This library is a `com.android.library` module. Include it in your settings and depend on it from
your app or feature modules:

```kotlin
// settings.gradle.kts
include(":sbnetworking")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":sbnetworking"))
}
```

(Alternatively, publish the produced `sbnetworking-release.aar` to your artifact repository and
depend on it as a normal coordinate.)

Add the `INTERNET` permission to the consuming app's manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Quick Start

### 1. Build an `HttpClient`

```kotlin
val httpClient = HttpClient.Builder(HttpClientEnvironment(baseUrl = "api.example.com"))
    .authTokenProvider(myTokenProvider)   // optional
    .enableLogging(BuildConfig.DEBUG)     // optional
    .build()
```

`HttpClientEnvironment` accepts a host (`api.example.com`) or host-with-port (`127.0.0.1:54321`) and
defaults to the `https` scheme.

### 2. Declare a feature `*Api` and create it from `retrofit`

```kotlin
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class UserDto(val id: String, val name: String, val email: String)

interface UserApi {
    @GET("users/{id}")
    suspend fun user(@Path("id") id: String): UserDto
}

val userApi: UserApi = httpClient.retrofit.create(UserApi::class.java)
```

### 3. Call it and handle errors

```kotlin
suspend fun loadUser(id: String): UserDto? =
    try {
        userApi.user(id)
    } catch (e: HttpClientError.Unauthorized) {
        // session expired
        null
    } catch (e: HttpClientError.NotConnectedToInternet) {
        // offline
        null
    }
```

Every failure — transport or HTTP — surfaces as an `HttpClientError`, so a `*Api` `suspend` call
never throws a raw `IOException` or `HttpException`.

## Core Concepts

### `HttpClient` and `HttpClient.Builder`

`HttpClient` owns a configured `Retrofit` and `OkHttpClient`. Build it once (per environment) and
reuse it. Builder options:

| Method | Purpose |
|---|---|
| `authTokenProvider(provider)` | Supplies tokens for header injection + 401 refresh |
| `enableLogging(enabled)` | Toggles full request/response logging (`OkHttp` `BODY` level) |
| `connectTimeoutSeconds` / `readTimeoutSeconds` / `writeTimeoutSeconds` | Client timeouts |
| `okHttpClient(client)` | Build on top of an existing `OkHttpClient` (custom TLS, interceptors, tests) |
| `build()` | Produces the `HttpClient` |

Exposed members: `httpClient.retrofit`, `httpClient.okHttpClient`, `httpClient.environment`,
`httpClient.authTokenProvider`.

> Logging defaults to **off** because `BODY`-level logging prints `Authorization`/`apikey` headers.
> Opt in with `enableLogging(BuildConfig.DEBUG)`.

### `HttpClientError`

A sealed type covering every outcome:

`InvalidUrl`, `InvalidResponse`, `DecodingFailed`, `Unauthorized`, `ClientError(statusCode, body)`,
`ServerError(statusCode, body)`, `UnexpectedStatusCode`, `NotFound`, `NotConnectedToInternet`,
`NetworkConnectionLost`, `NotImplemented`.

Status-code mapping: `200..299` and `304` succeed; `204` yields `null`; `401`→`Unauthorized`;
`404`→`NotFound`; `400/402/403/405..499`→`ClientError`; `500..599`→`ServerError`; anything else →
`UnexpectedStatusCode`.

### `AuthTokenProvider`

The injection seam for authentication. Implement it (backed by your secure token storage) and pass
it to the builder. When present, the client adds the `apikey` header (when `apiKey` is non-null) and
`Authorization: Bearer <accessToken>` (when `accessToken` is non-null), and on a 401 it calls
`refresh()` and retries the request once.

## Use Cases

### Authentication & token refresh

```kotlin
class SessionTokenProvider(
    private val storage: TokenStorage,
    private val authService: AuthService,
) : AuthTokenProvider {
    override val accessToken: String? get() = storage.accessToken
    override val refreshToken: String? get() = storage.refreshToken
    override val apiKey: String? = "your-anon-key"

    override fun updateTokens(accessToken: String, refreshToken: String) {
        storage.save(accessToken, refreshToken)
    }

    // Called automatically on a 401. Use a SEPARATE client (without this library's authenticator)
    // for the refresh call so a 401 from the refresh endpoint cannot recurse.
    override suspend fun refresh() {
        val new = authService.refresh(refreshToken ?: throw HttpClientError.Unauthorized)
        updateTokens(new.accessToken, new.refreshToken)
    }
}

val httpClient = HttpClient.Builder(HttpClientEnvironment(baseUrl = "api.example.com"))
    .authTokenProvider(SessionTokenProvider(storage, authService))
    .build()
```

If you don't supply a provider (or `refresh()` throws the default), a 401 surfaces as
`HttpClientError.Unauthorized`.

### Custom headers

```kotlin
interface ReportApi {
    @Headers("Accept: application/pdf")
    @GET("reports/{id}")
    suspend fun report(@Path("id") id: String): ByteArray
}
```

Headers you set explicitly (via `@Headers`/`@Header`) always win; the auth headers are only added
when absent.

### Request with a body

```kotlin
@Serializable
data class CreatePostBody(val title: String, val body: String, val userId: Int)

@Serializable
data class PostDto(val id: Int, val title: String, val body: String, val userId: Int)

interface PostApi {
    @POST("posts")
    suspend fun create(@Body body: CreatePostBody): PostDto
}
```

The `application/json` content type is applied automatically when a body is present and no explicit
content type was set.

### Query parameters

```kotlin
interface SearchApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("page") page: Int,
    ): SearchResultsDto
}
```

### Dynamic / programmatic requests with `EndpointSpec`

When a request is built at runtime and you don't want a dedicated `*Api`, describe it with
`EndpointSpec` and send it through `execute`:

```kotlin
val result: SearchResultsDto? = httpClient.execute(
    EndpointSpec(
        path = "/search",
        method = HttpMethod.GET,
        queryParameters = mapOf("term" to "kotlin", "page" to "1"),
        timeoutSeconds = 15,
    ),
)
```

You can also have a feature request model build its own spec:

```kotlin
data class SearchRequest(val term: String, val page: Int) : EndpointConvertible {
    override fun toEndpoint() = EndpointSpec(
        path = "/search",
        method = HttpMethod.GET,
        queryParameters = mapOf("term" to term, "page" to page.toString()),
    )
}

val result: SearchResultsDto? = httpClient.execute(SearchRequest("kotlin", 1))
```

`execute<T>` decodes the JSON body into `T`, returns `null` on `204`, and — when `T` is `ByteArray`
— returns the raw response bytes without decoding. Use `executeRaw(spec)` to always get the raw
bytes.

### Raw bytes

```kotlin
val bytes: ByteArray? = httpClient.execute<ByteArray>(
    EndpointSpec(path = "/files/123", method = HttpMethod.GET),
)
```

### Paginated / enveloped responses

For APIs that wrap collections in a `data` + `meta` envelope:

```kotlin
@Serializable
data class ItemDto(val id: Int, val name: String)

val page = httpClient.execute<EnvelopeDto<ItemDto>>(
    EndpointSpec(path = "/items", method = HttpMethod.GET),
)
val items: List<ItemDto> = page?.data.orEmpty()
val totalPages: Int = page?.meta?.totalPages ?: 0
```

### DTO → domain mapping

Keep transport DTOs and domain models separate. Map with free extension functions:

```kotlin
data class User(val id: String, val name: String)

fun UserDto.toDomain() = User(id = id, name = name)

val user = userApi.user("123").toDomain()
```

Or, optionally, implement the `ModelConvertible<M>` marker on a DTO and call `toDomain()`.

### Error handling

```kotlin
when (val outcome = runCatching { postApi.create(body) }.exceptionOrNull()) {
    null -> { /* success */ }
    is HttpClientError.ClientError -> log("HTTP ${outcome.statusCode}")
    is HttpClientError.ServerError -> log("server ${outcome.statusCode}")
    is HttpClientError.NotConnectedToInternet -> showOffline()
    is HttpClientError -> showGenericError(outcome.message)
    else -> throw outcome
}
```

### Logging

```kotlin
val httpClient = HttpClient.Builder(HttpClientEnvironment(baseUrl = "api.example.com"))
    .enableLogging(BuildConfig.DEBUG)
    .build()
```

## Testing

The library is easy to test against a real HTTP stack with MockWebServer:

```kotlin
@Test
fun `decodes user`() = runTest {
    val server = MockWebServer().also { it.start() }
    server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"1","name":"Ada"}"""))

    val client = HttpClient.Builder(
        HttpClientEnvironment(scheme = "http", baseUrl = server.hostName + ":" + server.port),
    ).build()
    val api = client.retrofit.create(UserApi::class.java)

    val user = api.user("1")

    assertThat(user.name).isEqualTo("Ada")
    server.shutdown()
}
```

## Optional Hilt wiring

This library does **not** depend on Hilt — `HttpClient` is a plain constructable class, so the core
is DI-framework-agnostic and never forces Hilt on consumers. If your app uses Hilt, drop this module
into your **app** (where the Hilt plugin is already applied) and provide an `HttpClientEnvironment`
(and optionally an `AuthTokenProvider`) binding in your graph:

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

## License

MIT.
