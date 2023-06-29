package org.koin.benchmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.Koin
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.awaitAllStartJobs
import org.koin.core.lazyModules
import org.koin.core.time.measureDurationForResult
import org.koin.dsl.koinApplication
import kotlin.math.roundToInt

object PerfRunner {

    suspend fun runAll(scope: CoroutineScope): PerfResult {
        val results = (1..10).map { i -> withContext(scope.coroutineContext) { runScenario(i) } }
        val avgStart = (results.sumOf { it.first } / results.size).round(100)
        val avgExec = (results.sumOf { it.second } / results.size).round(1000)

        println("Avg start: $avgStart ms")
        println("Avg execution: $avgExec ms")
        return PerfResult(avgStart,avgExec)
    }

    @OptIn(KoinExperimentalAPI::class)
    fun runScenario(index: Int): Pair<Double, Double> {
        val (app, duration) = measureDurationForResult {
            koinApplication {
                lazyModules(
                    perfModule400()
                )
            }
        }
        println("Perf[$index] start in $duration ms")

        val koin: Koin = app.koin

        runBlocking {
            koin.awaitAllStartJobs()
        }

        val (_, executionDuration) = measureDurationForResult {
            koinScenario(koin)
        }
        println("Perf[$index] run in $executionDuration ms")
        app.close()
        return Pair(duration, executionDuration)
    }

    fun koinScenario(koin: Koin){
        koin.get<A27>()
        koin.get<A31>()
        koin.get<A12>()
        koin.get<A42>()
    }
}

fun Double.round(digits : Int) : Double = (this * digits).roundToInt() / digits.toDouble()

data class PerfLimit(val startTime : Double, val execTime : Double)

data class PerfResult(val startTime : Double, val execTime : Double) {
    var worstMaxStartTime = 0.0
    var worstExecTime = 0.0
    var isStartOk = false
    var isExecOk = false
    var isOk = true

    fun applyLimits(limits: PerfLimit) {
        worstMaxStartTime = limits.startTime
        worstExecTime = limits.execTime

        isStartOk = startTime <= worstMaxStartTime
        isExecOk = execTime <= worstExecTime
        isOk = isStartOk && isExecOk
    }
}