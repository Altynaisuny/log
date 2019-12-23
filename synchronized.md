# synchronized小问题

遇到一个小需求，需要维护MySQL中的一个字段，按照XZMS+时间（20191212）+当前序号（五位数，当天00001递增）例如：XZMS2019121200001、XZMS2019121200002、XZMS2019121212345

## 悲观锁（synchronized）方案

新增序号表table(xz_index)  column(index, varchar )

@service

```java
//读
private void read(){
    synchronized (this){
        //dao
    }
}
//写
private void write(){
    synchronized (this){
        //dao
    }
}
//逻辑处理
private void handle(){
    read();
    //执行逻辑+1
    write();
}
```

@service为spring管理的单例，synchronized对**this** 加锁 ，service对象的Mark Word就会有锁标识，当完成读写时，线程独占结束。

注意几个写法：

```java
//读
private void read(){
}
//写
private void write(){
}
private void handle2(){
    synchronized (this){
        read();
        //执行逻辑+1
        write();
    }
}
```

两种写法俱可，只要是位于同一service。synchronized代码块尽量出现在可能出现同步的代码处，为了标识给开发者容易识别。

## Mark Word补充

Object Header (128bit) = Mark Word(64bit) + Klass Word (63bit)

放一张图例

```
|  unused:25 | identity_hashcode:31 | unused:1 | age:4 | biased_lock:1 | lock:2
|  thread:54 |         epoch:2      | unused:1 | age:4 | biased_lock:1 | lock:2 
|                     ptr_to_lock_record:62                            
|                     ptr_to_heavyweight_monitor:62                   
```

biased_lock 偏向锁标记 1（启动）0（无锁）

lock 锁状态标记位

age GC年龄

identity_hashcode 对象标识Hash码

thread 持有偏向锁的thread ID和extra info