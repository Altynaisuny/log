# spring bean

## spring boot 启动

main入口

```java
SpringApplication.run(KafkaProApplication.class, args);
```

SpringApplication

```java
public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
   return run(new Class[]{primarySource}, args);
}
```

constructor

```java
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
        this.sources = new LinkedHashSet();
        this.bannerMode = Mode.CONSOLE;
        this.logStartupInfo = true;
        this.addCommandLineProperties = true;
        this.addConversionService = true;
        this.headless = true;
        this.registerShutdownHook = true;
        this.additionalProfiles = new HashSet();
        this.isCustomEnvironment = false;
        this.resourceLoader = resourceLoader;
        Assert.notNull(primarySources, "PrimarySources must not be null");
        this.primarySources = new LinkedHashSet(Arrays.asList(primarySources));
    //webApplicationType
        this.webApplicationType = WebApplicationType.deduceFromClasspath();
        this.setInitializers(this.getSpringFactoriesInstances(ApplicationContextInitializer.class));
        this.setListeners(this.getSpringFactoriesInstances(ApplicationListener.class));
        this.mainApplicationClass = this.deduceMainApplicationClass();
    }
```

计算WebApplicationType

```java
public enum WebApplicationType {
    NONE,
    SERVLET,
    REACTIVE;
}
```

此处return Servlet

ClassUtils:

```java
public static Class<?> forName(String name, @Nullable ClassLoader classLoader){}
```

name: org.springframework.web.reactive.DispatcherHandler

classLoader: getDefaultClassLoader()

## ResourceLoader 

> 资源文件位置定位

在DefaultResourceLoader 中，getResource中有一下几种方式对Bean文件进行定位
PS：class DefaultResourceLoader implements ResourceLoader

源码在这：

```java
public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        Iterator var2 = this.protocolResolvers.iterator();

        Resource resource;
        do {
            if (!var2.hasNext()) {
                if (location.startsWith("/")) {
                    return this.getResourceByPath(location);
                }

                if (location.startsWith("classpath:")) {
                    return new ClassPathResource(location.substring("classpath:".length()), this.getClassLoader());
                }

                try {
                    URL url = new URL(location);
                    return (Resource)(ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
                } catch (MalformedURLException var5) {
                    return this.getResourceByPath(location);
                }
            }

            ProtocolResolver protocolResolver = (ProtocolResolver)var2.next();
            resource = protocolResolver.resolve(location, this);
        } while(resource == null);

        return resource;
    }
```



* 类路径

  ```java
  if (location.startsWith("classpath:")) {
      return new ClassPathResource
          (location.substring("classpath:".length()),this.getClassLoader());
  }
  ```

  

* 文件系统

  ```java
  if (location.startsWith("/")) {
     return this.getResourceByPath(location);
  }
  ```

  

* URL

  ```java
  try {
      URL url = new URL(location);
      return (Resource)
          (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
  } catch (MalformedURLException var5) {
      return this.getResourceByPath(location);
  }
  ```

## BeanDefinition
> 源码注释：
>
> A BeanDefinition describes a bean instance, which has property values,
> constructor argument values, and further information supplied by
> concrete implementations.
>
> 这是一个描述Bean的接口，包括了Bean的属性，构造参数，实例化的信息
### BeanDefinitionReader

### BeanDefinitionRegistry

### BeanDefinitionBuilder

## BeanFactory

DefaultListableBeanFactory

```java
/** Resolver to use for checking if a bean definition is an autowire candidate. */
	private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

	/** Map from dependency type to corresponding autowired value. */
	private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

	/** Map of bean definition objects, keyed by bean name. */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

	/** Map of singleton and non-singleton bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

	/** Map of singleton-only bean names, keyed by dependency type. */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

	/** List of bean definition names, in registration order. */
	private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

	/** List of names of manually registered singletons, in registration order. */
	private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);
```

