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

在 Maven 项目中添加以下依赖：
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>simple-request</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 使用说明

### **1. 添加注解配置**

- 使用 `@EnableRestClients` 开启 REST 客户端功能，并指定接口所在的基础包路径。
- 定义 REST 客户端接口，并使用注解标注请求的参数和配置。

#### 配置示例

```java
@EnableRestClients(basePackages = "com.example.clients")
@Configuration
public class AppConfig {
}
```

#### 定义接口

```java
@RestClient(baseUrl = "https://api.example.com")
public interface ExampleClient {

    @RestRequest(path = "/users/{id}", method = HttpMethod.GET)
    User getUser(@PathVariable("id") String userId);

    @RestRequest(path = "/users", method = HttpMethod.POST)
    User createUser(@BodyParam User user);

    @RestRequest(path = "/users", method = HttpMethod.GET)
    List<User> getUsers(@QueryParam("page") int page, @QueryParam("size") int size);
}
```

------

### **2. 使用 REST 客户端**

在 Spring 项目中，直接通过依赖注入（`@Autowired` 或 `@Resource`）即可使用生成的 REST 客户端代理对象：

```java
@RestController
public class UserController {

    @Autowired
    private ExampleClient exampleClient;

    @GetMapping("/user/{id}")
    public User getUserById(@PathVariable String id) {
        return exampleClient.getUser(id);
    }

    @PostMapping("/user")
    public User addUser(@RequestBody User user) {
        return exampleClient.createUser(user);
    }

    @GetMapping("/users")
    public List<User> listUsers(@RequestParam int page, @RequestParam int size) {
        return exampleClient.getUsers(page, size);
    }
}
```

------

## **详细注解说明**

### **@RestClient**

标注接口，定义基础配置。

| 参数          | 说明                                 | 示例                |
| ------------- | ------------------------------------ | ------------------- |
| `baseUrl`     | 基础 URL，所有请求的公共部分         | `"https://api.com"` |
| `propertyKey` | 可选，优先从配置文件中加载的属性键名 | `"api.base.url"`    |

------

### **@RestRequest**

标注接口方法，定义具体的 HTTP 请求。

| 参数      | 说明                       | 示例                         |
| --------- | -------------------------- | ---------------------------- |
| `path`    | 请求路径，支持路径参数替换 | `"/users/{id}"`              |
| `method`  | HTTP 方法                  | `HttpMethod.GET`             |
| `headers` | 请求头，支持占位符替换     | `{"Authorization: {token}"}` |

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



## 待完善功能

- [ ] 自定义序列化
- [ ] 更多的客户端类型（OkHttp等）
