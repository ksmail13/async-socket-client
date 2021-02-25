package io.github.ksmail13.scheduler

import java.lang.Exception
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.functions.FunctionN

typealias AsyncResult<R> = Future<R>

interface Scheduler {
    fun schedule(r: Runnable)
    fun <T> schedule(c: Callable<T>): AsyncResult<T>
    fun <T, R> schedule(p1: T, f: Function1<T, R>): AsyncResult<R>
    fun <T1, T2, R> schedule(p1: T1, p2: T2, f: Function2<T1, T2, R>): AsyncResult<R>
    fun <T1, T2, T3, R> schedule(p1: T1, p2: T2, p3: T3, f: Function3<T1, T2, T3, R>): AsyncResult<R>
    fun <T1, T2, T3, T4, R> schedule(p1: T1, p2: T2, p3: T3, p4: T4, f: Function4<T1, T2, T3, T4, R>): AsyncResult<R>
    fun <R> schedule(arr: Array<Any>, f: FunctionN<R>): AsyncResult<R>
}

class SchedulerThreadFactory(private val name: String, private val daemon: Boolean = false) : ThreadFactory {
    override fun newThread(r: Runnable): Thread {
        return Thread(r, name).apply { this.isDaemon = daemon }
    }
}

class StatelessScheduler(private val threadSize: Int, private val name: String = "scheduler") : Scheduler {
    private val executors: List<ExecutorService> = ((0..threadSize)
        .map { Executors.newSingleThreadExecutor(SchedulerThreadFactory("${name}-${it}", true)) }
        .toList())

    private val counter: AtomicInteger = AtomicInteger()

    private fun nextExecutor(): ExecutorService = executors[counter.getAndUpdate { (it + 1) % threadSize }]


    private fun <R> run(r: () -> R): AsyncResult<R> {
        val executor = nextExecutor()
        val future = CompletableFuture<R>()
        executor.execute {
            try {
                future.complete(r())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    override fun schedule(r: Runnable) {
        val poll = nextExecutor()
        poll.execute(r)
    }

    override fun <T> schedule(c: Callable<T>): AsyncResult<T> = run { c.call() }

    override fun <T, R> schedule(p1: T, f: (T) -> R): AsyncResult<R> = run { f(p1) }

    override fun <T1, T2, R> schedule(p1: T1, p2: T2, f: (T1, T2) -> R): AsyncResult<R> = run { f(p1, p2) }

    override fun <T1, T2, T3, R> schedule(p1: T1, p2: T2, p3: T3, f: (T1, T2, T3) -> R): AsyncResult<R> = run { f(p1, p2, p3) }

    override fun <T1, T2, T3, T4, R> schedule(p1: T1, p2: T2, p3: T3, p4: T4, f: (T1, T2, T3, T4) -> R) = run { f(p1, p2, p3, p4) }

    override fun <R> schedule(arr: Array<Any>, f: FunctionN<R>) = run { f(arr) }

}