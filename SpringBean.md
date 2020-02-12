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

这是一个接口

```java
org.springframework.beans.factory.support.BeanDefinitionReader
```

有以下实现类

* AbstractBeanDefinitionReader
* GroovyBeanDefinitionReader
* PropertiesBeanDefinitionReader
* XmlBeanDefinitionReader

以上实现类分别表示具体的reader过程，从几种途径来识别成为bean，这里就是一个识别的过程。

此处只分析一下接口中的内容

```java
//Return the bean factory to register the bean definitions with.返回用来注册的beanFactory
BeanDefinitionRegistry getRegistry();
//Return the resource loader to use for resource locations.返回定位bean位置的ResourceLoader
ResourceLoader getResourceLoader();
//Return the class loader to use for bean classes.返回bean所在class的类加载器
ClassLoader getBeanClassLoader();
//Return the BeanNameGenerator to use for anonymous beans
BeanNameGenerator getBeanNameGenerator();
int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;
int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;
int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;
int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;
```



### BeanDefinitionRegistry

Bean的注册过程，接口org.springframework.beans.factory.support.BeanDefinitionRegistry：
```java
void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
    throws BeanDefinitionStoreException;
```

实现类：org.springframework.beans.factory.support.DefaultListableBeanFactory

```java
//---------------------------------------------------------------------
// Implementation of BeanDefinitionRegistry interface
//---------------------------------------------------------------------

@Override
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
    throws BeanDefinitionStoreException {}
```

具体代码分步骤如下：

* 校验beanDefinition instanceof AbstractBeanDefinition
```java
if (beanDefinition instanceof AbstractBeanDefinition) {
	try {
		((AbstractBeanDefinition) beanDefinition).validate();
	}catch (BeanDefinitionValidationException ex) {
		throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,"Validation of bean definition failed", ex);
	}
}

```
* beanDefinitionMap 是否已经声明了bean

  ```java
  BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName); 
  ```
  * beanDefinitionMap已经声明了该bean
    是否允许重写
    ```java
    if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
    ```
    一堆打印的日志
    ```java
    else if (existingDefinition.getRole() < beanDefinition.getRole()) {
						// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
						if (logger.isInfoEnabled()) {
							logger.info("Overriding user-defined bean definition for bean '" + beanName +
									"' with a framework-generated bean definition: replacing [" +
									existingDefinition + "] with [" + beanDefinition + "]");
						}
					}
    			else if (!beanDefinition.equals(existingDefinition)) {
    				if (logger.isDebugEnabled()) {
    					logger.debug("Overriding bean definition for bean '" + beanName +
    							"' with a different definition: replacing [" + existingDefinition +
    							"] with [" + beanDefinition + "]");
  				}
    			}
  			else {
    				if (logger.isTraceEnabled()) {
    					logger.trace("Overriding bean definition for bean '" + beanName +
    							"' with an equivalent definition: replacing [" + existingDefinition +
    							"] with [" + beanDefinition + "]");
    				}
    			}
    ```
    put（重要）：放入beanDefinitionMap（ConcurrentHashMap）中
    ```java
    this.beanDefinitionMap.put(beanName, beanDefinition);
    ```
    
  * 未声明
  
    ```java
    if (hasBeanCreationStarted()) {
    	// Cannot modify startup-time collection elements anymore (for stable iteration)
        synchronized (this.beanDefinitionMap) {
            this.beanDefinitionMap.put(beanName, beanDefinition);
            List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
            updatedDefinitions.addAll(this.beanDefinitionNames);
            updatedDefinitions.add(beanName);
            this.beanDefinitionNames = updatedDefinitions;
            removeManualSingletonName(beanName);
        }
    }
    else {
        // Still in startup registration phase
        this.beanDefinitionMap.put(beanName, beanDefinition);
        this.beanDefinitionNames.add(beanName);
        removeManualSingletonName(beanName);
    }
    this.frozenBeanDefinitionNames = null;
    }
    ```
* 单例
```java
if (existingDefinition != null || containsSingleton(beanName)) {
  resetBeanDefinition(beanName);
}
```

-----end----

### BeanDefinitionBuilder

> 这是一个final class

看下这个方法的构造，这里要求必须传入一个BeanDefinition
```java
/**
 * Enforce the use of factory methods.
 */
private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
    this.beanDefinition = beanDefinition;
}
```

builder 的过程主要是一些set过程

包括：factoryMethod，initMethodName，destoryMethodName，scope，autowireMode，role，

这里简单放一些源码：

```java
/**
	 * Set the init method for this definition.
	 */
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * Set the destroy method for this definition.
	 */
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}
/**
	 * Set the scope of this definition.
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}
/**
	 * Set whether or not this definition is abstract.
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * Set whether beans for this definition should be lazily initialized or not.
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}
```



## BeanFactory

DefaultListableBeanFactory

注册到IOC容器中的BeanFactory（extends AbstractAutowireCapableBeanFactory 
implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable）

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

