# ThreadPoolExecutor

## base

线程池的5种状态

```java
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;
```

这里都用到了COUNT_BITS（workerCount所占位数）

```java
private static final int COUNT_BITS = Integer.SIZE - 3;
```

SIZE 以二进制补码的形式表示int值得位数 Integer.SIZE = 32 

线程池的状态一共有5种 需要3位来表示，至少需要3位来表示。

000 SHUTDOWN，001 STOP，010 TIDYING，011 TERMINATED，111 RUNNING

所以状态值左移32-3=29位，高三位表示线程池的运行状态runState、低29位表示workerCount。

需要注意一个变量ctl

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

AtomicInteger表示原子操作类。

如果使用Interger，必须加上synchronized保证不会出现并发线程同时访问的情况 

AtomicInteger里使用```private volatile int value;```**volatile** 表示线程共享变量。

注意：AtomicInteger使用非阻塞算法实现并发控制(里面很奇妙，暂不解释)

重点说下ctlof

```java
private static int ctlOf(int rs, int wc) { return rs | wc; }
```

rs=runState

wc=workerCount

| 或运算 有1为1 否则为0 

这样的目的是保持高位不变，地位进行workCount的递增。

继续回到ctl初始化的过程，ctlof(RUNNING, 0) 

RUNNING = 111 00000....000

初始化的结果为 线程池处于运行状态，线程数为0

获取线程池运行状态

```java
private static int runStateOf(int c)     { return c & ~CAPACITY; }
```

获取线程池线程数

```java
private static int workerCountOf(int c)  { return c & CAPACITY; }
```

这里用到了

```java
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
```

1<<29变成 001 00....00 

再-1 变成    000 11....11

~取反 变成 111 00.....00

再通过&计算 同1为1 否则为0

这样 runStateOf 取得是前3位，后29位全为0。

 workerCountOf 取的是后29位。前3位全是0。

bingo！！！

补充计算机基础姿势：

* 负数的表示方法：补码表示法（原码的反码+1）

* 正数的最高位是0，负数的最高位是1（简单理解）

## execute
英文步骤：

```wiki
/*
 * Proceed in 3 steps:
 *
 * 1. If fewer than corePoolSize threads are running, try to
 * start a new thread with the given command as its first
 * task.  The call to addWorker atomically checks runState and
 * workerCount, and so prevents false alarms that would add
 * threads when it shouldn't, by returning false.
 *
 * 2. If a task can be successfully queued, then we still need
 * to double-check whether we should have added a thread
 * (because existing ones died since last checking) or that
 * the pool shut down since entry into this method. So we
 * recheck state and if necessary roll back the enqueuing if
 * stopped, or start a new thread if there are none.
 *
 * 3. If we cannot queue task, then we try to add a new
 * thread.  If it fails, we know we are shut down or saturated
 * and so reject the task.
 */
```
注意上面第二步中，如果task可以被放入队列，我们仍然需要二次确认 是否我们应该增加这个线程

因为存在一种可能：在上次确认的时候，这个线程已经死亡。

还要检查在进入这个方法时，线程池是否已经shutdown

所以我们需要重新检查，以便回滚

```java
private static boolean isRunning(int c) {
        return c < SHUTDOWN;
}
```



代码实现

```java

public void execute(Runnable command) {
    if (command == null)
            throw new NullPointerException();
    //获取当前线程池状态。
    int c = ctl.get();
    //小于核心线程数
    if (workerCountOf(c) < corePoolSize) {
        //放入核心线程池里执行
        if (addWorker(command, true))
            return;
        //重新获取线程池状态。
        c = ctl.get();
    }
    //大于核心线程数
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        if (! isRunning(recheck) && remove(command))
            //拒绝策略
            reject(command);
        else if (workerCountOf(recheck) == 0)
            //放入任务
            addWorker(null, false);
    }
    else if (!addWorker(command, false))
        //拒绝策略
        reject(command);
}
```

## workQueue

阻塞队列

声明

 ```java
private final BlockingQueue<Runnable> workQueue;
 ```

来瞧一下这个阻塞队列

```java
package java.util.concurrent;
public interface BlockingQueue<E> extends Queue<E> {}
```

有以下接口

* add(E)
* offer(E)
* put(E)
* offer(E, long, TimeUnit)添加一个元素并返回true，如果队列已满，则返回false
* take()
* poll(long , TimeUnit)
* remainingCapacity()
* remove(Object)
* contains(Object)
* drainTo(Collection<? super E>)
* drainTo(Collection<? super E>)
``` java
package java.util;
public interface Queue<E> extends Collection<E> {}
```
常见的队列interface：
* BlockingDeque(java.util.concurrent)

* BlockingQueue(java.util.concurrent)

* Deque(java.util)

* TransferQueue(java.util.concurrent)
  由此可见并发包中常用的阻塞队列有BlockingDeque、BlockingQueue、TransferQueue

  线程池中用到的是BlockingQueue

* @see java.util.concurrent.LinkedBlockingQueue

* @see java.util.concurrent.BlockingQueue

* @see java.util.concurrent.ArrayBlockingQueue

* @see java.util.concurrent.LinkedBlockingQueue

* @see java.util.concurrent.PriorityBlockingQueue
###  ArrayBlockingQueue
#### constructor

> boolean fair 公平参数 设定为true  等待时间最长的线程会优先处理

1、指定容量

``` java
public ArrayBlockingQueue(int capacity) {
  this(capacity, false);
}
```
2、指定容量、是否需要公平性

```java
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}
```
3、指定容量、是否需要公平性、collection

```java
public ArrayBlockingQueue(int capacity, boolean fair,
                              Collection<? extends E> c) {
    this(capacity, fair);

    final ReentrantLock lock = this.lock;
    lock.lock(); // Lock only for visibility, not mutual exclusion
    try {
        int i = 0;
        try {
            for (E e : c) {
                checkNotNull(e);
                items[i++] = e;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new IllegalArgumentException();
        }
        count = i;
        putIndex = (i == capacity) ? 0 : i;
    } finally {
        lock.unlock();
    }
}
```
#### offer

```java
/**
  * Inserts the specified element at the tail of this queue, waiting
  * up to the specified wait time for space to become available if
  * the queue is full.
  *
  * @throws InterruptedException {@inheritDoc}
  * @throws NullPointerException {@inheritDoc}
*/
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {

    checkNotNull(e);
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length) {
            if (nanos <= 0)
                return false;
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(e);
        return true;
    } finally {
        lock.unlock();
    }
}
```
### LinkedBlockingQueue

#### constructor

#### offer



## addWorker

放入线程池中，线程池执行的过程也是一个队列  FIFO  先进先出

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    retry:
    for (;;) {
        //当前线程池状态
        int c = ctl.get();
        //runState
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        //判断条件
        //1、rs >= SHUTDOWN
        //2、! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty() )
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;

        for (;;) {
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            if (compareAndIncrementWorkerCount(c))
                break retry;
            c = ctl.get();  // Re-read ctl
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }

    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

