/*
 * Copyright 2025 Enaium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.enaium.jimmer.buddy.utility

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class DelayedExecutionQueue(
    private val defaultDelay: Long,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val tasks = ConcurrentHashMap<String, Job>()

    fun schedule(
        key: String,
        delayMillis: Long = defaultDelay,
        block: suspend () -> Unit
    ) {
        tasks[key]?.let { existingJob ->
            existingJob.cancel()
            tasks.remove(key)
        }

        val newJob = coroutineScope.launch {
            try {
                delay(delayMillis)
                block()
            } finally {
                if (tasks[key] == this) {
                    tasks.remove(key)
                }
            }
        }

        tasks[key] = newJob
    }
}