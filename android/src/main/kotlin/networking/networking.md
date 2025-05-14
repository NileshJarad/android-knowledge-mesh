## Networking

### REST API

REST stands for Representational State Transfer. It is an architectural style for designing networked applications,
particularly web services. REST was introduced by Roy Fielding in his 2000 Ph.D. dissertation.

Key Concepts of REST
REST is based on six architectural principles:

1. Stateless
    - Each API call contains all the information needed for the server to fulfill it.
    - The server does not remember any previous interactions.
2. Client-Server Architecture
    - The client and server are separate entities that communicate via HTTP.
    - This separation improves scalability and simplifies development.
3. Uniform Interface Uses standard HTTP methods:
    - GET – Retrieve data
    - POST – Create data
    - PUT – Update data
    - DELETE – Delete data

4. Cacheable
    - Responses can be cached to improve performance.

5. Layered System
    - A REST API can be composed of multiple layers (e.g., caching, authentication) without the client knowing.

6. Code on Demand (Optional)
    - The server can send executable code (e.g., JavaScript), though rarely used in practice.

**RESTful API**

A RESTful API is an API that follows REST principles. It uses HTTP to perform CRUD operations on resources.

### REST vs RESTful

- REST is the design principle.
- RESTful API is an implementation that follows REST principles.

---

### HTTP status code

#### Success code

| HTTP code | Meaning         | 
|-----------|-----------------|
| 200       | Server success  | 
| 201       | Created         |
| 204       | No content      |
| 206       | Partial content |

#### Redirection message

| HTTP code | Meaning     | 
|-----------|-------------|
| 400       | Bad Request | 

#### Client error code

| HTTP code | Meaning            | 
|-----------|--------------------|
| 300       | Multiple Choices   | 
| 301       | Moved permanently  |
| 302       | Found              |
| 307       | Temporary ReDirect |
| 308       | Permanent ReDirect |

#### Server error response

| HTTP code | Meaning               | 
|-----------|-----------------------|
| 501       | Internal Server error | 
| 502       | Bad Gateway           |
| 502       | Service Unavailable   |
| 504       | Gateway timeout       |

---

### Retrofit

---

### OkHttp

---

### Interceptors

In Retrofit (via OkHttp), interceptors are components that can observe, modify, and even short-circuit HTTP requests and
responses. They act like filters or middleware for your network calls.

**Types of Interceptors:**

1. Application Interceptor (addInterceptor)
    - Runs once, before the request is sent.
    - Useful for adding headers, logging, modifying requests, etc.

2. Network Interceptor (addNetworkInterceptor)
    - Runs only if the request goes over the network (not when from cache).
    - Useful for response-based logic like caching headers.

| Use Case                 | Interceptor Type |
|--------------------------|------------------|
| Add headers (auth token) | Application      |
| Log request/response     | Application      |
| Handle offline cache     | Application      |
| Modify response cache    | Network          |

---

### Caching

Caching in Retrofit is handled through OkHttp (which Retrofit uses internally). By enabling HTTP caching in OkHttp,
Retrofit can automatically cache the responses of network requests, which can be reused for subsequent requests,
improving performance and reducing network load.

**Create a Cache Instance**

```kotlin
val cacheSize = (5 * 1024 * 1024).toLong()  // 5 MB
val httpCacheDirectory = File(context.cacheDir, "httpCache")
val cache = Cache(httpCacheDirectory, cacheSize)

```

**Add Cache to OkHttpClient**

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .cache(cache)
    .addInterceptor { chain ->
        val response = chain.proceed(chain.request())
        // Cache responses for 60 seconds
        response.newBuilder()
            .header("Cache-Control", "public, max-age=60")  // cache for 60 seconds
            .build()
    }
    .build()

```

**Handle Offline Cache (Optional)**

You can also create a network interceptor to handle offline caching when the user is not connected to the internet. For
this, you'll need to modify the request headers to force fetching from cache if the network is unavailable.

```kotlin
val offlineInterceptor = Interceptor { chain ->
    var request = chain.request()
    if (!isNetworkAvailable()) {
        request = request.newBuilder()
            .header("Cache-Control", "public, only-if-cached, max-stale=86400")  // 1 day stale cache
            .build()
    }
    chain.proceed(request)
}

val okHttpClient = OkHttpClient.Builder()
    .cache(cache)
    .addInterceptor(offlineInterceptor)
    .addInterceptor(networkInterceptor)  // To handle network responses
    .build()

```

**Note:**

For effective caching, your server needs to send appropriate Cache-Control and ETag headers. Common cache headers
include:

Cache-Control: Directives such as max-age, no-cache, public, private, etc.

ETag: A tag that helps to determine if the response has changed since the last request.

Last-Modified: Timestamp when the resource was last modified.

If the server doesn't send the right caching headers, the caching mechanism will not work efficiently.

---

### Multi-part requests

---

### Socket connection

WebSockets are a protocol that enables full-duplex communication between a client (like an Android app) and a server
over a single, long-lived TCP connection.

Unlike HTTP, where the client must initiate every request, WebSockets allow both client and server to send messages
independently at any time.

**Use Cases of WebSockets in Mobile Apps**

1. Real-Time Chat Applications
    - Apps like WhatsApp, Messenger, Slack use WebSockets.
    - Enables instant message delivery and reception without polling the server.

2. Live Notifications
    - Push notifications for new emails, alerts, or app updates.
    - Faster than polling and more efficient.

3. Real-Time Collaboration
    - Apps like Google Docs or whiteboard apps.
    - Multiple users edit content together and see updates live.

4. Live Gaming / Multiplayer Games
    - Synchronize player actions instantly.
    - Game state updates and position tracking in real time.

5. Live Sports Scores / Stock Ticker
    - Real-time updates without reloading the UI.
    - Common in trading apps or sports tracking dashboards.

6. IoT and Device Control
    - Smart home devices, cameras, or sensors sending and receiving updates instantly.

7. Real-Time Location Sharing
    - Uber-like apps, where drivers and passengers share locations in real-time.

---

| Feature           | REST API                          | WebSocket                          |
|-------------------|-----------------------------------|------------------------------------|
| Direction         | One-way (client → server)         | Two-way (client ⇄ server)          |
| Real-Time Updates | ❌ Needs polling or long-polling   | ✅ Instant                          |
| Overhead          | High (each call = new connection) | Low (single persistent connection) |
| Ideal For         | CRUD, short-lived interactions    | Continuous data exchange           |


### Server Side Event (SSE)

SSE (Server-Sent Events) is a web technology that allows servers to send real-time updates to the client (browser) over
a single HTTP connection. It is commonly used for applications that need to display live information, such as news
feeds, stock market updates, or live chat messages.

**Key Features of SSE:**

- Unidirectional Communication: The data flows from the server to the client only, unlike WebSockets, which allow
  bidirectional communication.
- Built on HTTP: SSE uses the HTTP protocol, making it easier to implement than WebSockets in environments where only
  HTTP is supported.
- Automatic Reconnection: If the connection drops, the browser will attempt to reconnect automatically.
- Event Stream: The server sends a continuous stream of data in a specific format, typically in JSON or plain text.

In the SSE connection is kept live using

1. Long-Lived HTTP Connection:
    - When an SSE connection is established, the client sends a standard HTTP GET request to the server, just like any
      other HTTP request.
    - The server responds with a text/event-stream content type, and the connection is kept open.
    - Instead of sending a single response and closing the connection (like in typical HTTP responses), the server keeps
      the connection open and continuously sends updates in the form of event data.
    - The client is expected to keep reading the data as long as the server is sending it.
2. HTTP Keep-Alive Header:
    - The HTTP/1.1 protocol uses the Connection: keep-alive header to indicate that the server should keep the
      connection open.
    - In the case of SSE, this header is typically sent by the server to ensure the connection is maintained open until
      the server decides to close it or the client disconnects.
3. Heartbeats
    - To ensure that the connection is still alive and prevent it from being closed by intermediate proxies or load
      balancers (which may close idle connections), the server can send heartbeat messages. These are usually just a
      comment or an empty message sent at regular intervals (e.g., every 30 seconds). The client can ignore these
      heartbeat messages.

Http header example from server

```text
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

---

### Auth refresh tokens

--