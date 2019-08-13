# spring boot permission auth

## datasource 

* 1.DataSourceAutoConfiguration
```java
    private ClassLoader getDataSourceClassLoader(ConditionContext context) {
			Class<?> dataSourceClass = DataSourceBuilder
					.findType(context.getClassLoader());
			return (dataSourceClass != null) ? dataSourceClass.getClassLoader() : null;
	}
```
* 2.DataSourceBuilder
``` java
    private static final String[] DATA_SOURCE_TYPE_NAMES = new String[];
```
数据库连接池默认支持三种 hikari、tomcat、dbcp2
```
{
    "com.zaxxer.hikari.HikariDataSource",
    "org.apache.tomcat.jdbc.pool.DataSource", 
    "org.apache.commons.dbcp2.BasicDataSource"
};
```
* 3.application.yml
```yml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/sas
        username: root
        password: mysql
        driver-class-name: com.mysql.jdbc.Driver
        #type: com.zaxxer.hikari.HikariDataSource
```
默认采用HikariDataSource 连接池
代码如下：
DATA_SOURCE_TYPE_NAMES 

```Java
    public static Class<? extends DataSource> findType(ClassLoader classLoader) {
        String[] var1 = DATA_SOURCE_TYPE_NAMES;
        int var2 = var1.length;
        int var3 = 0;

        while(var3 < var2) {
            String name = var1[var3];

            try {
                return ClassUtils.forName(name, classLoader);
            } catch (Exception var6) {
                ++var3;
            }
        }

        return null;
    }
```

要改变连接池，需要声明type，要改成druid-datasource-pool
type主要和connection pool 有关
DataSourceProperties.java

```java
	/**
	 * Fully qualified name of the connection pool implementation to use. By default, it
	 * is auto-detected from the classpath.
	 */
	private Class<? extends DataSource> type;
	
	public DataSourceBuilder<?> initializeDataSourceBuilder() {
		return DataSourceBuilder.create(getClassLoader()).type(getType())
				.driverClassName(determineDriverClassName()).url(determineUrl())
				.username(determineUsername()).password(determinePassword());
	}
```

这边遇到一个错误
```xml
    java.sql.SQLNonTransientConnectionException: CLIENT_PLUGIN_AUTH is required
	at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:110) ~[mysql-connector-java-8.0.15.jar:8.0.15]
	Loading class `com.mysql.jdbc.Driver'. This is deprecated. The new driver class is `com.mysql.cj.jdbc.Driver'. The driver is automatically registered via the SPI and manual loading of the driver class is generally unnecessary.
```
目前猜测是因为连接池的版本和mysql-connector-java 版本不一致
mysql-connector-java-8.0.15版本太高了，需要降级。
使用5.x版本的最后一个

```xml
	<dependency>
		<groupId>mysql</groupId>
		<artifactId>mysql-connector-java</artifactId>
		<version>5.1.47</version>
	</dependency>
```

另外yml配置中不要使用如下配置，该配置在IDEA代码提醒的第一行，不要使用，因为不是目标包的factory.properties

​    data-username:
​    data-password: 

## spring  security
> spring 依赖中如果有插件，一直扫描factories.properties  在yml中使用exclude



### 关于springboot的异常处理
> spring boot 封装了BasicErrorController，对request过程中抛出的异常进行封装，共有两种形式的封装。
```java
package org.springframework.boot.autoconfigure.web.servlet.error;

public class BasicErrorController extends AbstractErrorController {}
public abstract class AbstractErrorController implements ErrorController {}

package org.springframework.boot.web.servlet.error;

@FunctionalInterface
public interface ErrorController {
    String getErrorPath();
}
```
* 基于JSON
```java 
	@RequestMapping
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
		Map<String, Object> body = getErrorAttributes(request,
				isIncludeStackTrace(request, MediaType.ALL));
		HttpStatus status = getStatus(request);
		return new ResponseEntity<>(body, status);
	}
```
* 基于html
```java 
	@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView errorHtml(HttpServletRequest request,
			HttpServletResponse response) {
		HttpStatus status = getStatus(request);
		Map<String, Object> model = Collections.unmodifiableMap(getErrorAttributes(
				request, isIncludeStackTrace(request, MediaType.TEXT_HTML)));
		response.setStatus(status.value());
		ModelAndView modelAndView = resolveErrorView(request, response, status, model);
		return (modelAndView != null) ? modelAndView : new ModelAndView("error", model);
	}
```
异常返回类型：
```json
{
    "timestamp": "2019-05-17T08:34:16.226+0000",
    "status": 404,
    "error": "Not Found",
    "message": "No message available",
    "path": "/guest/"
}
```
spring 封装了所有的异常信息1xx，2xx，3xx
package org.springframework.http;
public enum HttpStatus;

## shiro
### shiro maven pom
* 1.shiro-spring-boot-web-starter
使用spring-boot-starter的设计模式，可以使用yml进行信息配置。
``` xml
<!-- shiro-starter安全框架-->
<dependency>
    <groupId>org.apache.shiro</groupId>
    <artifactId>shiro-spring-boot-web-starter</artifactId>
    <version>1.4.0</version>
</dependency>
```
* 2.shiro-spring
```xml
<dependency>
    <groupId>org.apache.shiro</groupId>
    <artifactId>shiro-spring</artifactId>
    <version>1.4.0</version>
</dependency>
```
### subject 
subject中都包含那些对象  
servletRequest
servletResponse
principals
host
session
sessionCreationEnabled
securityManager

### AuthorizingRealm

> 自定义token时，遇到一个问题，自定义AuthenticationToken throw AuthenticationException 抛出异常。

```powershell
org.apache.shiro.authc.IncorrectCredentialsException: Submitted credentials for token 
[org.apache.shiro.authc.UsernamePasswordToken - personal, rememberMe=false] did not match the expected credentials.
```
token不是期望的类型，not the expected credentials，系统 提供的usernameandpasswordtiken已经够用，不需要自定义token。

```java
//shiro封装了密码的匹配：
//AuthenticationToken token, AuthenticationInfo info
//分别是从数据库中取出的密码 和 入参加密后的密码
if (info != null) {
	assertCredentialsMatch(token, info);
} else {
	log.debug("No AuthenticationInfo found for submitted AuthenticationToken [{}].  Returning null.", token);
}

byte[] tokenBytes = toBytes(tokenCredentials);
byte[] accountBytes = toBytes(accountCredentials);
return MessageDigest.isEqual(tokenBytes, accountBytes);
```

### 盐值
当两个用户的密码相同时，单纯使用不加盐的MD5加密方式，会发现数据库中存在相同结构的密码，这样也是不安全的。我们希望即便是两个人的原始密码一样，加密后的结果也不一样。
具体实现：

```java
userPassToken = new UsernamePasswordToken();
ByteSource salt = ByteSource.Util.bytes(loginUser.getUsername());
//将密码进行MD5转换后，与自定义盐值运算1024次。
SimpleHash simpleHash = new SimpleHash("MD5", loginUser.getPassword(), salt, 1024);
userPassToken.setUsername(loginUser.getUsername());
userPassToken.setPassword(simpleHash.toString().toCharArray());
```
### 路径安全过滤
在shiroconfig中生成一个bean，自定义一个chainfilter，拦截过程从上至下。
权限分为anon,authc,logout,port。。。
anon：任何用户发送的请求都能够访问	
authc：经过认证的请求可访问

```java 
@Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
        // all paths are managed via annotations
        chainDefinition.addPathDefinition("/guest/**", "anon");
        chainDefinition.addPathDefinition("/admin/**", "authc, roles[admin]");
        chainDefinition.addPathDefinition("/user/**", "authc, roles[user]");

        // all other paths require a logged in user
        chainDefinition.addPathDefinition("/**", "authc");

        return chainDefinition;
    }

```
### 自定义token
1.shiroconfig.java中重写SessionManager
```java
@Bean
protected SessionManager sessionManager(){
DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
        //过期时间（毫秒）
        webSessionManager.setGlobalSessionTimeout(18000000);
        //相隔多久检查session的有效性
        webSessionManager.setSessionValidationInterval(1800000);
        SimpleCookie simpleCookie = new SimpleCookie();
        //设置Cookie名字
        simpleCookie.setName("token");
        //设置Cookie的域名
        simpleCookie.setDomain("");
        //设置Cookie的过期时间，秒为单位，默认-1表示关闭浏览器时过期Cookie；
        simpleCookie.setMaxAge(3600);
        //如果设置为true，则客户端不会暴露给客户端脚本代码
        simpleCookie.setHttpOnly(true);
        //设置Cookie的路径，默认空，即存储在域名根下；
        simpleCookie.setPath("");
        webSessionManager.setSessionIdCookie(simpleCookie);
        return webSessionManager;
    }
```
header请求示例：
POST http://localhost:8085/guest/checkToken
Accept: application/json
Content-Type: application/json
Cache-Control: no-cache
Cookie: token=1575D79932ECE5F0E2304C09AF379CBC

需要在header中携带token

### shiro的身份认证
每次request都被shiro的filter拦截。在自定义realm中，通过重写doGetAuthenticationInfo和doGetAuthorizationInfo分别进行身份认证和权限认证（role和permission）逻辑如下：
* 首先进行身份认证，代码示例如下
```java
protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String username = (String) token.getPrincipal();
        List<User> userList = userRepository.findByUsername(username);
        if (CollectionUtils.isEmpty(userList)){
            throw new UnknownAccountException("用户不存在");
        }
        User user = userList.get(0);
        // principal credentials realmName
        return new SimpleAuthenticationInfo(user, user.getPassword(), "CustomRealm");
    }
```
* 然后进行权限认证，代码示例如下
```java 
@Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        Object principal= principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        //首先确定角色
        List<Role> roles =  userService.findRoleById(principal.toString());
        Set<String>  roleSet = new HashSet<>();
        roles.forEach(role->{
            roleSet.add(role.getRoleName());
        });
        authorizationInfo.setRoles(roleSet);
        //然后确定权限
        List<ResourceDto> resourceDtos = userService.findPermissionByRoleName(roleSet);
        //permission
        Set<String> permissions = new HashSet<>();
        resourceDtos.forEach(resource -> {
            permissions.add(resource.getResourceUrl());
        });
        authorizationInfo.setStringPermissions(permissions);
        //登录成功后会创建一个subject subject role create role
        return authorizationInfo;
    }
```
在controller如下使用
```java
@RequiresRoles("ROLE_SUPER")
@RequiresRoles("ROLE_USER")
```
**身份认证是每次请求必须的**

在认证中通过logincontroller中生成UsernamePasswordToken在doGetAuthenticationInfo中传入，realm中通过dao访问得到当前登录用户的信息，包括账号密码，生成SimpleAuthenticationInfo(user, user.getPassword(), "CustomRealm")
然后shiro的SimpleCredentialsMatcher会比对两个token，如果通过，会生成一个subject，subject中包含用户的唯一性标识，也可以放置实体。

示例：
访问游客登录接口：
* request：
POST http://localhost:8085/guest/login
Accept: application/json
Cache-Control: no-cache
Content-Type: application/json

{"username":"personal","password":"1234567"}
* response：
```json
{
    "code": "1",
    "message": null,
    "data": "org.apache.shiro.web.subject.support.WebDelegatingSubject@6f86c4a7login successfully"
}
```
拿到token：1ca1db4b-d4d4-43ef-9977-9ec7b2cfc0a6

访问结果权限限制的接口：
http://localhost:8085/test/test
shiro需要自定义loginUrl，此处没有login.html,所以显示404，applicaiton.yml中配置如下：
```yml
shiro:
  enabled: true
  loginUrl: /login.html
  successUrl: /success.html
  unauthorizedUrl: /unPermission
  sessionManager:
    sessionIdCookieEnabled: true
    sessionIdUrlRewritingEnabled: false
```
spring boot BasicErrorController封装的json类型的body response：
```json
{
    "timestamp": "2019-05-20T06:26:53.280+0000",
    "status": 404,
    "error": "Not Found",
    "message": "No message available",
    "path": "/login.json;JSESSIONID=e2b10d83-7ca5-4903-9e39-bebae8c7f5ad"
}
```
携带如下token：
token=1ca1db4b-d4d4-43ef-99
77-9ec7b2cfc0a6; path=/; domain=localhost; HttpOnly; Expires=Mon, 20 May 2019 07:20:33 GMT;

```json
{
    "id": null,
    "username": "personal",
    "password": null,
    "token": null,
    "updateTime": null,
    "expireTime": null
}
```

request 中header部分携带token可以正常访问

