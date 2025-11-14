package coralsum.toolkit

import java.io.File

class Upscayl(
    private var executable: String = "./upscayl-bin"
) {
    private var input: String? = null
    private var output: String? = null
    private var model: String? = null
    private var modelPath: String? = null
    private var outputScale: Int? = null
    private var compress: Int? = null
    private var tileSize: Int? = null
    private var gpuId: String? = null
    private var format: String? = null
    private var tta: Boolean = false
    private var verbose: Boolean = false

    fun input(path: String) = apply { input = path }
    fun output(path: String) = apply { output = path }
    fun model(name: String) = apply { model = name }
    fun modelPath(path: String) = apply { modelPath = path }
    fun scale(scale: Int) = apply { outputScale = scale }
    fun compress(level: Int) = apply { compress = level }
    fun tile(size: Int) = apply { tileSize = size }
    fun gpu(id: String) = apply { gpuId = id }
    fun format(fmt: String) = apply { format = fmt }
    fun tta(enable: Boolean = true) = apply { tta = enable }
    fun verbose(enable: Boolean = true) = apply { verbose = enable }

    fun buildCommand(): List<String> {
        val args = mutableListOf(executable)

        if (input != null) args += listOf("-i", input!!)
        if (output != null) args += listOf("-o", output!!)
        if (model != null) args += listOf("-n", model!!)
        if (modelPath != null) args += listOf("-m", modelPath!!)
        if (outputScale != null) args += listOf("-s", outputScale.toString())
        if (compress != null) args += listOf("-c", compress.toString())
        if (tileSize != null) args += listOf("-t", tileSize.toString())
        if (gpuId != null) args += listOf("-g", gpuId!!)
        if (format != null) args += listOf("-f", format!!)
        if (tta) args += "-x"
        if (verbose) args += "-v"

        return args
    }

    fun run(
        workingDir: File = File("."),
        onOutput: (String) -> Unit = { print(it) },
        onError: (String) -> Unit = { System.err.print(it) }
    ): Int {
        val cmd = buildCommand()
        val process = ProcessBuilder(cmd)
            .directory(workingDir)
            .redirectErrorStream(false)
            .start()

        val stdout = process.inputStream.bufferedReader()
        val stderr = process.errorStream.bufferedReader()

        stdout.lines().forEach { onOutput(it + "\n") }
        stderr.lines().forEach { onError(it + "\n") }

        return process.waitFor()
    }
}
