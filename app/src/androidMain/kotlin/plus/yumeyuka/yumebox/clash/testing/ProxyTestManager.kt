/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYuka & YumeLira 2025.
 *
 */

package plus.yumeyuka.yumebox.clash.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import plus.yumeyuka.yumebox.core.Clash
import java.util.concurrent.ConcurrentHashMap

class ProxyTestManager(
    private val scope: CoroutineScope,
    private val maxConcurrentTests: Int = 5
) {
    private val semaphore = Semaphore(maxConcurrentTests)
    private val ongoingTests = ConcurrentHashMap.newKeySet<String>()

    private val _testStates = MutableStateFlow<Map<String, TestState>>(emptyMap())
    val testStates: StateFlow<Map<String, TestState>> = _testStates.asStateFlow()

    private val _testResults = MutableSharedFlow<TestResult>(extraBufferCapacity = 100)
    val testResults: SharedFlow<TestResult> = _testResults.asSharedFlow()

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    fun requestTest(
        groupName: String,
        priority: Int = Priority.NORMAL,
        forceTest: Boolean = false
    ) {
        scope.launch {
            try {
                if (ongoingTests.contains(groupName)) {
                    return@launch
                }

                ongoingTests.add(groupName)
                updateQueueState()

                val testStartTime = System.currentTimeMillis()

                updateTestState(groupName) {
                    TestState(
                        status = TestStatus.TESTING,
                        startTime = testStartTime
                    )
                }

                executeTest(groupName, testStartTime)

            } catch (e: Exception) {
                _testResults.tryEmit(
                    TestResult(
                        groupName = groupName,
                        success = false,
                        error = e.message
                    )
                )
            } finally {
                ongoingTests.remove(groupName)
                updateQueueState()
            }
        }
    }

    private suspend fun executeTest(groupName: String, testStartTime: Long) {
        try {
            semaphore.withPermit {
                Clash.healthCheck(groupName).await()

                val endTime = System.currentTimeMillis()
                val testState = TestState(
                    status = TestStatus.COMPLETED,
                    startTime = testStartTime,
                    endTime = endTime,
                    lastSuccessTime = endTime
                )

                updateTestState(groupName) { testState }
                _testResults.tryEmit(
                    TestResult(
                        groupName = groupName,
                        success = true,
                        duration = testState.duration
                    )
                )
            }
        } catch (e: Exception) {
            handleTestFailure(groupName, e)
        }
    }

    private fun handleTestFailure(groupName: String, error: Throwable) {
        updateTestState(groupName) {
            TestState(
                status = TestStatus.FAILED,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                error = error.message
            )
        }

        _testResults.tryEmit(
            TestResult(
                groupName = groupName,
                success = false,
                error = error.message
            )
        )
    }

    private fun updateTestState(groupName: String, update: (TestState?) -> TestState) {
        val currentState = _testStates.value[groupName]
        val newState = update(currentState)

        _testStates.value = _testStates.value.toMutableMap().apply {
            put(groupName, newState)
        }
    }

    private fun updateQueueState() {
        _queueState.value = QueueState(
            queuedCount = 0,
            testingCount = ongoingTests.size,
            maxConcurrent = maxConcurrentTests
        )
    }

    fun getTestStatistics(): TestStatistics {
        val currentStates = _testStates.value
        val completed = currentStates.values.count { it.status == TestStatus.COMPLETED }
        val failed = currentStates.values.count { it.status == TestStatus.FAILED }
        val testing = currentStates.values.count { it.status == TestStatus.TESTING }

        return TestStatistics(
            total = currentStates.size,
            completed = completed,
            failed = failed,
            testing = testing,
            queued = 0,
            successRate = if (completed > 0) (completed.toFloat() - failed) / completed else 0f
        )
    }

    data class TestState(
        val status: TestStatus,
        val startTime: Long = 0,
        val endTime: Long = 0,
        val lastSuccessTime: Long = 0,
        val retryCount: Int = 0,
        val error: String? = null,
        val cancelled: Boolean = false
    ) {
        val duration: Long get() = if (endTime > startTime) endTime - startTime else 0
    }

    data class TestResult(
        val groupName: String,
        val success: Boolean,
        val duration: Long = 0,
        val error: String? = null,
        val retryCount: Int = 0
    )

    data class QueueState(
        val queuedCount: Int = 0,
        val testingCount: Int = 0,
        val maxConcurrent: Int = 5
    )

    data class TestStatistics(
        val total: Int,
        val completed: Int,
        val failed: Int,
        val testing: Int,
        val queued: Int,
        val successRate: Float
    )

    enum class TestStatus {
        PENDING, TESTING, COMPLETED, FAILED, CANCELLED
    }

    object Priority {
        const val LOW = 0
        const val NORMAL = 1
        const val HIGH = 2
        const val URGENT = 3
    }
}
