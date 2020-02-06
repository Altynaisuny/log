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
    //线程池是running，先放进阻塞队列，如果放入成功，返回true，否则false
    if (isRunning(c) && workQueue.offer(command)) {
        //再次检查线程池状态
        int recheck = ctl.get();
        //如果线程池处于非运行状态，则从阻塞队列中删除刚才放入的线程任务，如果成功移除了task，进入if
        //这个判断写的真好！！！&&运算符号的短路功能用的十分好，并且融合了逻辑。
        if (! isRunning(recheck) && remove(command))
            //拒绝策略
            reject(command);
        //线程池处于运行状态，并且线程池中线程数为0，作为核心线程，addworker
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
	//调用 重入锁
    //此处使用了ReentrantLock，JDK中的独占锁，除了synchronized，还有ReentrantLock。
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

@param firestTask 

the task the new thread should run first (or null if none)

@param core 

core if true use corePoolSize as bound else maximumPoolSize.

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    //retry这个关键词很少用到，这里是标记一个循环，类似goto
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
            //当前线程数是否不大于核心线程数或者最大线程数。
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                //addWorkers失败
                return false;
            //Attempts to CAS-increment the workerCount field of ctl.
            //执行CAS的递增过程，增加workerCount。AtomicInteger
            if (compareAndIncrementWorkerCount(c))
                //跳出retry
                break retry;
            //这里有个重复确认，重新获取当前线程池的状态。
            c = ctl.get();  // Re-read ctl
            //在当前添加task的过程中，线程池的状态发生了改变。
            if (runStateOf(c) != rs)
                //从标记retry的地方再次执行
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }
	//此位置表示CAS成功，workCount + 1
    //任务是否成功启动
    boolean workerStarted = false;
    //任务是否添加成功
    boolean workerAdded = false;
    Worker w = null;
    try {
        //构造work
        w = new Worker(firstTask);
        //worker中的thread属性
        final Thread t = w.thread;
        if (t != null) {
            //加锁 可重入锁
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                //持有锁时再次检测。在ThreadFactory错误或者关闭（获得锁之前）时，执行退回
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    //线程是否存活，
                    if (t.isAlive()) 
                        // precheck that t is startable
                        //线程已经启动，并且没有死掉，抛出异常
                        throw new IllegalThreadStateException();
                    //把work加入workers中
                    workers.add(w);
                    int s = workers.size();
                    //线程池中的线程数大于最大线程数，更新最大线程数
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    //任务已经成功添加
                    workerAdded = true;
                }
            } finally {
                //解锁
                mainLock.unlock();
            }
            if (workerAdded) {
                //任务添加成功，运行该任务
                t.start();
                //改变标示
                workerStarted = true;
            }
        }
    } finally {
        //任务没有启动成功，从workers中移除该worker,该过程也是需要加锁的
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

workers 

```java
private final HashSet<Worker> workers = new HashSet<Worker>();
```

thinking：

什么样的worker是可以被放在workers中

* 线程池处于RUNNING 或者 （线程池==shutdown并且worker ==null)
* worker is not alive 。这里有点不懂==