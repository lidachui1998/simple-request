# Simple Request

Simple Request 是一个轻量级的 Java REST 客户端框架，旨在简化基于注解的 HTTP 调用。通过简单的配置，您可以快速构建强大、优雅的 REST 客户端，减少冗余代码并提高开发效率。

## 功能特性

- **基于注解的接口定义**：通过注解快速定义 REST API。
- **动态代理生成客户端**：无需手动实现接口，支持 CGLIB 动态代理。
- **灵活的请求构建**：支持路径参数、查询参数、自定义头信息以及请求体。
- **响应验证**：提供可扩展的响应验证机制。
- **与 Spring 深度集成**：支持通过 Spring 容器自动注册客户端。

---

## 安装

### Maven 依赖

在 Maven 项目中添加以下依赖：
```xml
<dependency>
    <groupId>com.lidachui</groupId>
    <artifactId>simple-request</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle 依赖

在 Gradle 项目中添加以下依赖：
```gradle
implementation 'com.lidachui:simple-request:1.0.0'
```

## 快速开始

### **第一步：启用 REST 客户端**

在 Spring Boot 应用的配置类或启动类上添加 `@EnableRestClients` 注解：

```java
@SpringBootApplication
@EnableRestClients(basePackages = "com.example.clients")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

或者在单独的配置类中：

```java
@EnableRestClients(basePackages = "com.example.clients")
@Configuration
public class RestClientConfig {
}
```

### **第二步：定义 REST 客户端接口**

创建接口并使用注解定义 API 调用：

```java
@RestClient(baseUrl = "https://api.example.com")
public interface UserClient {

    // GET 请求 - 获取单个用户
    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    User getUser(@PathVariable("id") String userId);

    // POST 请求 - 创建用户
    @RestRequest(path = "/users", method = HttpMethod.POST)
    User createUser(@BodyParam User user);

    // GET 请求 - 分页查询用户
    @RestRequest(path = "/users", method = HttpMethod.GET)
    List<User> getUsers(@QueryParam("page") int page, @QueryParam("size") int size);

    // 带请求头的请求
    @RestRequest(path = "/users/profile", method = HttpMethod.GET,
                headers = {"Authorization: Bearer {token}"})
    UserProfile getUserProfile(@HeaderParam("token") String token);
}
```

### **第三步：使用 REST 客户端**

在 Spring 项目中，通过依赖注入使用生成的 REST 客户端：

```java
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserClient userClient;

    @GetMapping("/user/{id}")
    public User getUserById(@PathVariable String id) {
        return userClient.getUser(id);
    }

    @PostMapping("/user")
    public User addUser(@RequestBody User user) {
        return userClient.createUser(user);
    }

    @GetMapping("/users")
    public List<User> listUsers(@RequestParam int page, @RequestParam int size) {
        return userClient.getUsers(page, size);
    }

    @GetMapping("/profile")
    public UserProfile getProfile(@RequestHeader("Authorization") String token) {
        return userClient.getUserProfile(token.replace("Bearer ", ""));
    }
}
```

---

## 高级功能使用

### **异步请求**

使用 `@Async` 注解实现异步请求，配合 `@Callback` 处理异步响应：

```java
@RestClient(baseUrl = "https://api.example.com")
public interface AsyncUserClient {

    // 异步请求，使用回调处理结果
    @Async
    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    void getUserAsync(@PathVariable("id") String userId,
                     @Callback ResponseCallback<User> callback);

    // 异步请求，返回 CompletableFuture
    @Async
    @RestRequest(path = "/users", method = HttpMethod.GET)
    CompletableFuture<List<User>> getUsersAsync(@QueryParam("page") int page);
}
```

**使用示例：**

```java
@Service
public class UserService {

    @Autowired
    private AsyncUserClient asyncUserClient;

    public void handleUserAsync() {
        // 方式1：使用回调
        asyncUserClient.getUserAsync("123", new ResponseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                System.out.println("获取用户成功: " + user.getName());
            }

            @Override
            public void onFailure(Exception e) {
                System.err.println("获取用户失败: " + e.getMessage());
            }
        });

        // 方式2：使用 CompletableFuture
        asyncUserClient.getUsersAsync(1)
            .thenAccept(users -> System.out.println("获取到 " + users.size() + " 个用户"))
            .exceptionally(throwable -> {
                System.err.println("请求失败: " + throwable.getMessage());
                return null;
            });
    }
}
```

### **缓存功能**

使用 `@Cacheable` 注解为请求添加缓存支持，支持本地缓存和 Redis 缓存：

```java
@RestClient(baseUrl = "https://api.example.com")
public interface CachedUserClient {

    // 本地缓存，缓存60秒
    @Cacheable(expire = 60, timeUnit = TimeUnit.SECONDS)
    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    User getUserWithCache(@PathVariable("id") String userId);

    // Redis缓存，缓存10分钟
    @Cacheable(expire = 10, timeUnit = TimeUnit.MINUTES,
               strategy = RedisCacheStrategy.class)
    @RestRequest(path = "/users", method = HttpMethod.GET)
    List<User> getUsersWithRedisCache(@QueryParam("page") int page);

    // 自定义缓存过期时间
    @Cacheable(expire = 1, timeUnit = TimeUnit.HOURS)
    @RestRequest(path = "/users/stats", method = HttpMethod.GET)
    UserStats getUserStats();
}
```

**缓存配置：**

```java
@Configuration
public class CacheConfig {

    // 配置 Redis 缓存策略（可选）
    @Bean
    public RedisCacheStrategy redisCacheStrategy(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheStrategy(redisTemplate);
    }
}
```

### **Mock 数据生成**

使用 `@Mock` 注解在开发和测试阶段生成模拟数据：

```java
@RestClient(baseUrl = "https://api.example.com")
public interface MockUserClient {

    // 使用默认的 JavaFaker 生成 Mock 数据
    @Mock
    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    User getMockUser(@PathVariable("id") String userId);

    // 使用自定义 Mock 生成器
    @Mock(mockGenerator = CustomMockGenerator.class)
    @RestRequest(path = "/users", method = HttpMethod.GET)
    List<User> getMockUsers();
}
```

**自定义 Mock 生成器：**

```java
@Component
public class CustomMockGenerator implements MockGenerator {

    @Override
    public <T> T generate(Class<T> clazz) {
        if (clazz == User.class) {
            User user = new User();
            user.setId("mock-" + System.currentTimeMillis());
            user.setName("Mock User");
            user.setEmail("mock@example.com");
            return (T) user;
        }
        return null;
    }
}
```

### **请求拦截器**

通过继承 `AbstractRequestFilter` 实现自定义请求拦截逻辑：

```java
@Component
public class CustomRequestFilter extends AbstractRequestFilter {

    @Override
    public void preHandle(Request request) {
        RequestContext requestContext = getRequestContext();
        // 请求前处理，如添加通用请求头、日志记录等
        request.getHeaders().put("X-Request-ID", UUID.randomUUID().toString());
        System.out.println("发送请求: " + request.getUrl());
    }

    @Override
    public void afterCompletion(Request request, Response response) {
        RequestContext requestContext = getRequestContext();
        // 请求完成后处理，如日志记录、性能统计等
        System.out.println("请求完成，状态码: " + response.getStatusCode());
    }

    @Override
    public void error(Request request, Response response, Exception e) {
        RequestContext requestContext = getRequestContext();
        // 异常处理，如错误日志记录、告警等
        System.err.println("请求异常: " + e.getMessage());
        super.error(request, response, e);
    }
}
```

### **动态 Host 配置**

使用 `@Host` 注解动态指定请求的主机地址：

```java
@RestClient(baseUrl = "https://api.example.com")
public interface DynamicHostClient {

    // 动态指定 Host
    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    User getUser(@PathVariable("id") String userId, @Host String host);
}
```

**使用示例：**

```java
@Service
public class UserService {

    @Autowired
    private DynamicHostClient dynamicHostClient;

    public User getUser(String userId, String environment) {
        String host = switch (environment) {
            case "dev" -> "https://dev-api.example.com";
            case "test" -> "https://test-api.example.com";
            case "prod" -> "https://api.example.com";
            default -> "https://api.example.com";
        };

        return dynamicHostClient.getUser(userId, host);
    }
}
```

------

## **详细注解说明**

### **@RestClient**

标注接口，定义基础配置。

| 参数          | 说明                                 | 示例                | 默认值 |
| ------------- | ------------------------------------ | ------------------- | ------ |
| `baseUrl`     | 基础 URL，所有请求的公共部分         | `"https://api.com"` | `""`   |
| `propertyKey` | 可选，优先从配置文件中加载的属性键名 | `"api.base.url"`    | `""`   |
| `name`        | 客户端名称，用于 Spring Bean 注册    | `"userClient"`      | `""`   |

**使用配置文件示例：**

```java
@RestClient(propertyKey = "user.api.baseUrl")
public interface UserClient {
    // ...
}
```

在 `application.yml` 中配置：
```yaml
user:
  api:
    baseUrl: https://api.example.com
```

------

### **@RestRequest**

标注接口方法，定义具体的 HTTP 请求。

| 参数      | 说明                       | 示例                         | 默认值        |
| --------- | -------------------------- | ---------------------------- | ------------- |
| `path`    | 请求路径，支持路径参数替换 | `"/users/{id}"`              | `""`          |
| `method`  | HTTP 方法                  | `HttpMethod.GET`             | `GET`         |
| `headers` | 请求头，支持占位符替换     | `{"Authorization: {token}"}` | `{}`          |

**支持的 HTTP 方法：**
- `HttpMethod.GET`
- `HttpMethod.POST`
- `HttpMethod.PUT`
- `HttpMethod.DELETE`
- `HttpMethod.PATCH`
- `HttpMethod.HEAD`
- `HttpMethod.OPTIONS`

------

### **@PathVariable**

标注路径参数，用于替换 URL 中的占位符。

```java
@RestRequest(path = "/users/{id}", method = HttpMethod.GET)
User getUser(@PathVariable("id") String userId);
```

### **@QueryParam**

标注查询参数，将参数拼接到 URL 中。

```java
@RestRequest(path = "/users", method = HttpMethod.GET)
List<User> getUsers(@QueryParam("page") int page, @QueryParam("size") int size);
```

### **@BodyParam**

标注请求体参数，用于 POST、PUT 请求的请求体。

```java
@RestRequest(path = "/users", method = HttpMethod.POST)
User createUser(@BodyParam User user);
```

### **@HeaderParam**

标注请求头参数，动态解析 Header 值。

```java
@RestRequest(path = "/users", method = HttpMethod.GET, headers = {"Authorization: {token}"})
User getUserWithAuth(@HeaderParam("token") String token);
```

### @Auth

身份验证功能，为请求增加身份信息；目前可以随意修改Request，修改时要谨慎避免影响正常流程运作。支持自定义身份验证逻辑（实现AuthProvider接口，自定义身份验证逻辑，注入spring，如果spring中没有，会用构造器创建一个，会浪费性能）

```java
@Auth(provider = ApiKeyAuthProvider.class) // 覆盖全局验证
    SecureData getSecureData();
```

### @Retry

重试机制，支持请求失败根据异常重试机制

```java
@Retry(maxRetries = 5, delay = 1000,retryFor = {RuntimeException.class, NullPointerException.class})
Map getLocation();
```

### @ResponseHeader

得到响应的头信息，只对Map有效

```java
@RestRequest(path = "/user/login", method = HttpMethod.POST})
User login(@ResponseHeader("Authorization") Map<String,String> token);
```

------

## **案例展示**

以下展示了一个调用 GitHub API 获取用户信息的案例：

#### 接口定义

```java
@RestClient(baseUrl = "https://api.github.com")
public interface GitHubClient {

    @RestRequest(path = "/users/{username}", method = HttpMethod.GET)
    GitHubUser getUser(@PathVariable("username") String username);
}
```

#### 使用

```java
@RestController
public class GitHubController {

    @Autowired
    private GitHubClient gitHubClient;

    @GetMapping("/github/{username}")
    public GitHubUser getGitHubUser(@PathVariable String username) {
        return gitHubClient.getUser(username);
    }
}
```

#### 示例响应

**请求**: `GET /github/octocat`
**响应**:

```json
{
  "login": "octocat",
  "id": 1,
  "avatar_url": "https://avatars.githubusercontent.com/u/1?v=4",
  "html_url": "https://github.com/octocat"
}
```



---

## 案例

以下是一个完整的使用案例：

1. 定义客户端接口：
   ```java
   @RestClient(baseUrl = "https://jsonplaceholder.typicode.com", name = "jsonPlaceholderClient")
   public interface JsonPlaceholderClient {
   
       @RestRequest(method = HttpMethod.GET, path = "/posts")
       List<Post> getAllPosts();
   
       @RestRequest(method = HttpMethod.GET, path = "/posts/{id}")
       Post getPostById(@PathVariable("id") int id);
   
       @RestRequest(method = HttpMethod.POST, path = "/posts")
       Post createPost(@BodyParam Post post);
   
       @RestRequest(method = HttpMethod.DELETE, path = "/posts/{id}")
       void deletePost(@PathVariable("id") int id);
   }
   ```

2. 调用 REST 接口：
   ```java
   @RestController
   @RequestMapping("/api")
   public class ApiController {
   
       private final JsonPlaceholderClient jsonPlaceholderClient;
   
       public ApiController(JsonPlaceholderClient jsonPlaceholderClient) {
           this.jsonPlaceholderClient = jsonPlaceholderClient;
       }
   
       @GetMapping("/posts")
       public List<Post> getAllPosts() {
           return jsonPlaceholderClient.getAllPosts();
       }
   
       @PostMapping("/posts")
       public Post createPost(@RequestBody Post post) {
           return jsonPlaceholderClient.createPost(post);
       }
   }
   ```

3. 定义模型类：
   ```java
   public class Post {
       private int id;
       private String title;
       private String body;
   
       // Getters and Setters
   }
   ```

---

## 高级功能

### 响应验证

实现 `ResponseValidator` 接口，自定义响应校验逻辑：
```java
@Component
public class CustomResponseValidator implements ResponseValidator {

    @Override
    public ValidationResult validate(Object response) {
        // 校验逻辑
        return ValidationResult.valid();
    }

    @Override
    public void onFailure(Request request, Object response, ValidationResult validationResult) {
        // 处理失败情况
        throw new RuntimeException("Response validation failed!");
    }
}
```

---

## 配置选项

### **HTTP 客户端配置**

框架支持多种 HTTP 客户端，可以通过配置选择：

```java
@Configuration
public class HttpClientConfig {

    // 使用 RestTemplate（默认）
    @Bean
    @Primary
    public AbstractHttpClientHandler restTemplateHandler() {
        return new RestTemplateHandler();
    }

    // 使用 OkHttp
    @Bean
    public AbstractHttpClientHandler okHttpHandler() {
        return new OkHttpHandler();
    }

    // 使用 Apache HttpClient
    @Bean
    public AbstractHttpClientHandler httpClientHandler() {
        return new HttpClientHandler();
    }
}
```

### **序列化器配置**

支持多种 JSON 序列化器：

```java
@Configuration
public class SerializerConfig {

    // 使用 Jackson（默认）
    @Bean
    @Primary
    public Serializer jacksonSerializer() {
        return new JacksonSerializer();
    }

    // 使用 Gson
    @Bean
    public Serializer gsonSerializer() {
        return new GsonSerializer();
    }

    // 使用 FastJson
    @Bean
    public Serializer fastJsonSerializer() {
        return new FastJsonDeserializer();
    }
}
```

### **全局配置**

在 `application.yml` 中进行全局配置：

```yaml
simple-request:
  # 默认连接超时时间（毫秒）
  connect-timeout: 5000
  # 默认读取超时时间（毫秒）
  read-timeout: 10000
  # 默认写入超时时间（毫秒）
  write-timeout: 10000
  # 是否启用请求日志
  enable-logging: true
  # 默认重试次数
  default-retry-count: 3
  # 默认重试间隔（毫秒）
  default-retry-delay: 1000
```

## 单独使用（非 Spring 环境）

在非 Spring 环境中，可以直接使用 `SimpleClient`：

```java
public class NonSpringExample {

    public static void main(String[] args) {
        // 创建客户端
        SimpleClient client = SimpleClient.create();

        // 构建请求
        Request request = new Request();
        request.setUrl("https://api.example.com/users");
        request.setMethod(HttpMethod.GET);

        // 设置请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer your-token");
        request.setHeaders(headers);

        // 构建复杂类型
        Type listOfUsersType = TypeBuilder.paramType(List.class, User.class);
        Type resultType = TypeBuilder.paramType(Result.class, listOfUsersType);

        // 执行请求
        Response response = client.execute(request, resultType);
        Result<List<User>> result = (Result<List<User>>) response.getBody();

        System.out.println("获取到 " + result.getData().size() + " 个用户");
    }
}
```

**自定义客户端配置：**

```java
// 使用自定义配置
OkHttpHandler okHttpHandler = new OkHttpHandler();
GsonSerializer gsonSerializer = new GsonSerializer();
SimpleClient customClient = SimpleClient.create(okHttpHandler, gsonSerializer);
```

## 最佳实践

### **1. 接口设计原则**

```java
// ✅ 推荐：按业务模块划分接口
@RestClient(baseUrl = "https://api.example.com")
public interface UserClient {
    // 用户相关的所有操作
}

@RestClient(baseUrl = "https://api.example.com")
public interface OrderClient {
    // 订单相关的所有操作
}

// ❌ 不推荐：所有操作放在一个接口中
@RestClient(baseUrl = "https://api.example.com")
public interface ApiClient {
    // 混合了用户、订单、商品等各种操作
}
```

### **2. 异常处理**

```java
@Service
public class UserService {

    @Autowired
    private UserClient userClient;

    public User getUserSafely(String userId) {
        try {
            return userClient.getUser(userId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null; // 用户不存在
            }
            throw new BusinessException("获取用户信息失败", e);
        } catch (Exception e) {
            throw new BusinessException("系统异常", e);
        }
    }
}
```

### **3. 超时和重试配置**

```java
@RestClient(baseUrl = "https://api.example.com")
public interface ReliableClient {

    // 对于重要的接口，配置重试机制
    @Retry(maxRetries = 3, delay = 1000, retryFor = {ConnectTimeoutException.class})
    @RestRequest(path = "/important-data", method = HttpMethod.GET)
    ImportantData getImportantData();

    // 对于快速响应的接口，使用较短的超时时间
    @RestRequest(path = "/quick-check", method = HttpMethod.GET)
    HealthStatus quickHealthCheck();
}
```

### **4. 缓存策略**

```java
@RestClient(baseUrl = "https://api.example.com")
public interface CachedDataClient {

    // 对于变化频率低的数据使用长时间缓存
    @Cacheable(expire = 1, timeUnit = TimeUnit.HOURS)
    @RestRequest(path = "/config", method = HttpMethod.GET)
    SystemConfig getSystemConfig();

    // 对于用户相关数据使用短时间缓存
    @Cacheable(expire = 5, timeUnit = TimeUnit.MINUTES)
    @RestRequest(path = "/user/{id}/profile", method = HttpMethod.GET)
    UserProfile getUserProfile(@PathVariable("id") String userId);
}
```

### **5. 安全性考虑**

```java
@Component
public class SecurityRequestFilter extends AbstractRequestFilter {

    @Override
    public void preHandle(Request request) {
        // 添加安全相关的请求头
        request.getHeaders().put("X-API-Version", "v1");
        request.getHeaders().put("X-Client-ID", "your-client-id");

        // 对敏感信息进行脱敏处理（日志记录时）
        String url = request.getUrl();
        if (url.contains("password") || url.contains("token")) {
            // 记录脱敏后的URL
        }
    }
}
```

## 注意事项

### **1. 性能注意事项**

- **连接池配置**：在高并发场景下，合理配置 HTTP 客户端的连接池大小
- **超时设置**：根据接口的响应时间特性设置合适的超时时间
- **缓存使用**：对于频繁调用且数据变化不频繁的接口，使用缓存可以显著提升性能

### **2. 错误处理**

- **网络异常**：处理连接超时、读取超时等网络异常
- **HTTP 状态码**：根据不同的 HTTP 状态码进行相应的业务处理
- **业务异常**：解析响应中的业务错误码和错误信息

### **3. 日志和监控**

- **请求日志**：记录关键接口的请求和响应信息，便于问题排查
- **性能监控**：监控接口调用的响应时间和成功率
- **异常告警**：对于重要接口的调用异常，及时进行告警

### **4. 版本兼容性**

- **API 版本管理**：通过请求头或路径参数管理 API 版本
- **向后兼容**：在升级框架版本时，注意保持向后兼容性

## 待完善功能

- [ ] 支持更多的认证方式（OAuth2、JWT等）
- [ ] 支持请求和响应的数据压缩
- [ ] 支持更细粒度的缓存控制
- [ ] 支持熔断器模式
- [ ] 支持负载均衡
