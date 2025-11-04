package plus.yumeyuka.yumeyuka.bridge

import kotlinx.coroutines.await
import kotlin.js.Promise

@JsModule("./ksu-bridge.js")
@JsNonModule
private external object KsuBridgeModule {
    fun exec(command: String, options: dynamic = definedExternally): Promise<ExecResult>
    fun spawn(command: String, args: Array<String> = definedExternally, options: dynamic = definedExternally): ChildProcess
    fun fullScreen(isFullScreen: Boolean)
    fun toast(message: String)
}

external interface ExecResult {
    val errno: Int
    val stdout: String
    val stderr: String
}

external interface ChildProcess {
    val stdin: Stdio
    val stdout: Stdio
    val stderr: Stdio
    
    fun on(event: String, listener: (dynamic) -> Unit)
}

external interface Stdio {
    fun on(event: String, listener: (dynamic) -> Unit)
    fun emit(event: String, vararg args: dynamic)
}

data class ExecOptions(
    val cwd: String? = null,
    val env: Map<String, String>? = null,
    val timeout: Int? = null
) {
    fun toDynamic(): dynamic {
        val obj = js("{}")
        cwd?.let { obj.cwd = it }
        env?.let { obj.env = JSON.parse(JSON.stringify(it)) }
        timeout?.let { obj.timeout = it }
        return obj
    }
}

data class SpawnOptions(
    val cwd: String? = null,
    val env: Map<String, String>? = null,
    val shell: Boolean? = null
) {
    fun toDynamic(): dynamic {
        val obj = js("{}")
        cwd?.let { obj.cwd = it }
        env?.let { obj.env = JSON.parse(JSON.stringify(it)) }
        shell?.let { obj.shell = it }
        return obj
    }
}

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val isSuccess: Boolean get() = exitCode == 0
    
    companion object {
        internal fun fromExecResult(result: ExecResult): CommandResult {
            return CommandResult(
                exitCode = result.errno,
                output = result.stdout,
                error = result.stderr
            )
        }
    }
}

object KsuBridge {
    
    suspend fun exec(
        command: String,
        options: ExecOptions? = null
    ): CommandResult {
        return try {
            val result = if (options != null) {
                KsuBridgeModule.exec(command, options.toDynamic()).await()
            } else {
                KsuBridgeModule.exec(command).await()
            }
            CommandResult.fromExecResult(result)
        } catch (e: Throwable) {
            throw Exception("Failed to execute command: $command", e)
        }
    }
    
    suspend fun execSimple(command: String): String {
        val result = exec(command)
        if (!result.isSuccess) {
            throw Exception("Command failed with exit code ${result.exitCode}: ${result.error}")
        }
        return result.output
    }
    
    fun spawn(
        command: String,
        args: List<String> = emptyList(),
        options: SpawnOptions? = null
    ): ChildProcess {
        return try {
            when {
                options != null -> KsuBridgeModule.spawn(
                    command,
                    args.toTypedArray(),
                    options.toDynamic()
                )
                args.isNotEmpty() -> KsuBridgeModule.spawn(command, args.toTypedArray())
                else -> KsuBridgeModule.spawn(command)
            }
        } catch (e: Throwable) {
            throw Exception("Failed to spawn process: $command", e)
        }
    }
    
    fun setFullScreen(enabled: Boolean) {
        try {
            KsuBridgeModule.fullScreen(enabled)
        } catch (e: Throwable) {
            console.error("Failed to set fullscreen mode", e)
        }
    }
    
    fun toast(message: String) {
        try {
            KsuBridgeModule.toast(message)
        } catch (e: Throwable) {
            console.error("Failed to show toast", e)
        }
    }
    
    suspend fun commandExists(command: String): Boolean {
        return try {
            val result = exec("which $command")
            result.isSuccess && result.output.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}