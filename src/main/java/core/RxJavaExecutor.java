package core;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ComputationScheduler;
import io.reactivex.internal.schedulers.IoScheduler;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.schedulers.Schedulers;
import util.IdGenerator;
import util.NamedThreadFactory;

public class RxJavaExecutor {
  private Logger logger;
  private ExecutorService namedExecutor;
  private SingleScheduler singleScheduler;
  private ComputationScheduler computationScheduler;
  private IoScheduler ioScheduler;
  private IdGenerator idGenerator;

  private Scheduler mainScheduler;

  private ConcurrentHashMap<Integer, Disposable> removableDisposableMap =
          new ConcurrentHashMap<Integer, Disposable>();

  /**
   * RxJavaExecutor is a wrapper for interacting with RxJava schedulers. By default all methods will
   * run on the executor being used and observed on the main scheduler this class creates. The
   * scheduler that observes completed tasks can be overridden.
   *
   * @param name to be added to all schedulers created by this class
   * @param numberOfThreads is the number of threads to keep in the main schedulers thread pool. One
   *     is the default size.
   * @param executorOverride executor passed in to override main scheduler. Null mean use main
   *     scheduler.
   */
  public RxJavaExecutor(String name, int numberOfThreads, Executor executorOverride) {
    logger = LoggerFactory.getLogger(name + "-RxJavaExecutor");
    idGenerator = new IdGenerator(name, 0, 0);

    if (executorOverride == null) {
      namedExecutor =
              new ThreadPoolExecutor(
                      numberOfThreads,
                      numberOfThreads,
                      0L,
                      TimeUnit.MILLISECONDS,
                      new LinkedBlockingQueue<Runnable>(),
                      new NamedThreadFactory(name));
      mainScheduler = Schedulers.from(namedExecutor);
    } else {
      mainScheduler = Schedulers.from(executorOverride);
    }

    computationScheduler =
            new ComputationScheduler(new RxThreadFactory(name + "-RxComputationThreadPool"));
    ioScheduler = new IoScheduler(new RxThreadFactory(name + "-RxCachedThreadScheduler"));
    singleScheduler = new SingleScheduler(new RxThreadFactory(name + "-RxSingleScheduler"));
  }

  /**
   * Schedules a callable to be run using an RxJava {@link io.reactivex.Flowable#timer(long,
   * TimeUnit, Scheduler)}
   *
   * <p>This is run on this RxJavaExecutor's {@link
   * io.reactivex.internal.schedulers.ComputationScheduler}
   *
   * <p>Scheduling a callable with this is limiting as you can't react to the state of the callable
   * return. Recommended to use a {@link io.reactivex.Flowable} and one of this RxJavaExecutor's
   * schedulers directly.
   *
   * @param delay time until the callable is called.
   * @param callable the callable to call
   * @param blocking set to true if runnable is a blocking operation. Will use the ioScheduler
   *     rather than the computationScheduler. For network, database or file operations.
   * @return an Integer identifying the disposable returned by the Flowable. Can be used to cancel
   *     the scheduled callable.
   * @throws Exception thrown when no more id's for disposables are available
   */
  public Integer scheduleSingleCallable(
          long delay, Callable<? extends Object> callable, boolean blocking) throws Exception {
    Integer id = idGenerator.getNewId();
    Disposable disposable =
            Flowable.timer(delay, TimeUnit.MILLISECONDS, blocking ? ioScheduler : computationScheduler)
                    .map(m -> callable.call())
                    .observeOn(mainScheduler)
                    .subscribe(
                            onNext -> logger.trace("scheduleSingleCallable {} ", onNext),
                            error -> {
                              logger.error("scheduleSingleCallable error {} ", error.getMessage());
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            },
                            () -> {
                              logger.trace("scheduleSingleCallable Completed");
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            });

    removableDisposableMap.put(id, disposable);

    return id;
  }

  /**
   * Schedules a runnable to be run using an RxJava {@link io.reactivex.Flowable#timer(long,
   * TimeUnit, Scheduler)}
   *
   * <p>This is run on this RxJavaExecutor's {@link
   * io.reactivex.internal.schedulers.ComputationScheduler}
   *
   * @param delay time until the runnable is run.
   * @param runnable the runnable to be run.
   * @param blocking set to true if runnable is a blocking operation. Will use the ioScheduler
   *     rather than the computationScheduler. For network, database or file operations.
   * @return an Integer identifying the disposable returned by the Flowable. Can be used to cancel
   *     the scheduled runnable.
   * @throws Exception thrown when no more id's for disposables are available
   */
  public Integer scheduleSingleRunnable(long delay, Runnable runnable, boolean blocking)
          throws Exception {
    Integer id = idGenerator.getNewId();
    Disposable disposable =
            Flowable.timer(delay, TimeUnit.MILLISECONDS, blocking ? ioScheduler : computationScheduler)
                    .doOnNext(m -> runnable.run())
                    .observeOn(mainScheduler)
                    .subscribe(
                            onNext -> logger.trace("scheduleSingleRunnable {} ", onNext),
                            error -> {
                              logger.error("scheduleSingleRunnable error {} ", error.getMessage());
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            },
                            () -> {
                              logger.trace("scheduleSingleRunnable Completed");
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            });

    removableDisposableMap.put(id, disposable);

    return id;
  }

  /**
   * Schedules a runnable to be run at a fix rate using an RxJava {@link
   * io.reactivex.Flowable#interval(long, long, TimeUnit, Scheduler)}
   *
   * <p>This is run on this RxJavaExecutor's {@link
   * io.reactivex.internal.schedulers.ComputationScheduler}
   *
   * @param delay time in milliseconds until the runnable should start
   * @param period the time in milliseconds between the runnable executing
   * @param runnable the runnable to be executed
   * @param blocking set to true if runnable is a blocking operation. Will use the ioScheduler
   *     rather than the computationScheduler. For network, database or file operations.
   * @return an Integer identifying the disposable returned by the Flowable. Can be used to cancel
   *     the scheduled runnable.
   * @throws Exception thrown when no more id's for disposables are available
   */
  public Integer scheduleFixedRateRunnable(
          long delay, long period, Runnable runnable, boolean blocking) throws Exception {
    Integer id = idGenerator.getNewId();
    Disposable disposable =
            Flowable.interval(
                    delay, period, TimeUnit.MILLISECONDS, blocking ? ioScheduler : computationScheduler)
                    .doOnNext(m -> runnable.run())
                    .observeOn(mainScheduler)
                    .subscribe(
                            i -> logger.trace("scheduleFixedRateRunnable id: {}", id),
                            error -> {
                              logger.error("scheduleFixedRateRunnable error {}", error.getMessage());
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            },
                            () -> {
                              logger.trace("scheduleFixedRateRunnable Completed");
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            });

    removableDisposableMap.put(id, disposable);

    return id;
  }

  /**
   * Schedules a callable to be run at a fixed rate using an RxJava {@link
   * io.reactivex.Flowable#interval(long, long, TimeUnit, Scheduler)}
   *
   * <p>This is run on this RxJavaExecutor's {@link
   * io.reactivex.internal.schedulers.ComputationScheduler}
   *
   * <p>Scheduling a callable with this is limiting as you can't react to the state of each callable
   * return. Recommended to use a {@link io.reactivex.Flowable} and one of this RxJavaExecutor's
   * schedulers directly.
   *
   * @param delay time in milliseconds until the runnable should start
   * @param period the time in milliseconds between the runnable executing
   * @param callable the runnable to be executed
   * @param blocking set to true if runnable is a blocking operation. Will use the ioScheduler
   *     rather than the computationScheduler. For network, database or file operations.
   * @return an Integer identifying the disposable returned by the Flowable. Can be used to cancel
   *     the scheduled callable.
   * @throws Exception thrown when no more id's for disposables are available
   */
  public Integer scheduleFixedRateCallable(
          long delay, long period, Callable<? extends Object> callable, boolean blocking)
          throws Exception {
    Integer id = idGenerator.getNewId();
    Disposable disposable =
            Flowable.interval(
                    delay, period, TimeUnit.MILLISECONDS, blocking ? ioScheduler : computationScheduler)
                    .map(m -> callable.call())
                    .observeOn(mainScheduler)
                    .subscribe(
                            i -> logger.trace("scheduleFixedRateCallable {}", i),
                            error -> {
                              logger.error("scheduleFixedRateCallable error {}", error.getMessage());
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            },
                            () -> {
                              logger.trace("scheduleFixedRateCallable Completed");
                              idGenerator.recycleId(id);
                              removeCompletedDisposable(id);
                            });

    removableDisposableMap.put(id, disposable);

    return id;
  }

  /**
   * @return Scheduler to be used for single or scheduled tasks
   */
  public SingleScheduler getSingleScheduler() {
    return singleScheduler;
  }

  /** @return Scheduler to be used for running computation heavy operations */
  public ComputationScheduler getComputationScheduler() {
    return computationScheduler;
  }

  /** @return Scheduler to be used for running blocking operations */
  public IoScheduler getIoScheduler() {
    return ioScheduler;
  }

  /**
   * @return main Scheduler that all tasks are observed on.
   */
  public Scheduler getMainScheduler() {
    return mainScheduler;
  }

  /** @return number of current disposables being tracked */
  public Integer countOfScheduledDisposables() {
    return removableDisposableMap.size();
  }

  /** @return id pool size returned by generator */
  public Integer getIdPoolSize() {
    return idGenerator.getIdentityPoolSize();
  }

  /** @return ids in use size returned by generator */
  public Integer getIdsInUseSize() {
    return idGenerator.getIdentitiesInUseSize();
  }

  private void removeCompletedDisposable(Integer id) {
    Disposable dis = removableDisposableMap.remove(id);
    if (dis != null) {
      dis.dispose();
    }
  }

  /**
   * @param id returned by the disposable that should be cancelled.
   * @return true if disposable was cancelled otherwise false.
   */
  public synchronized boolean cancelScheduledDisposable(Integer id) {
    Disposable dis = removableDisposableMap.remove(id);

    if (dis != null) {
      dis.dispose();
      idGenerator.recycleId(id);
      return dis.isDisposed();
    }
    return false;
  }

  /**
   * Shuts down all RxJava Schedulers and the main Java Executor. If no other threads are running
   * program will exit.
   */
  public void shutdownExecutor() {
    logger.debug("Shutdown called, stopping all related schedulers and executor");
    mainScheduler.shutdown();
    singleScheduler.shutdown();
    computationScheduler.shutdown();
    ioScheduler.shutdown();
    namedExecutor.shutdown();
  }

  private void logRunningThread() {
    logger.info("logRunningThread: {}", Thread.currentThread().getName());
  }
}
