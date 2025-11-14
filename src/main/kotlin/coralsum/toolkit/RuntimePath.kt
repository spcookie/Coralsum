package coralsum.toolkit

import java.io.File
import java.net.URI
import java.nio.file.Paths

object RuntimePath {
    fun getExecutableDir(): File {
        // 1) JVM 下：通过 ProtectionDomain 拿到 jar 路径
        val uri: URI? = try {
            object {}.javaClass.protectionDomain.codeSource.location.toURI()
        } catch (_: Exception) {
            null
        }

        if (uri != null) {
            val path = Paths.get(uri).toFile()
            // 运行 jar 包时 → 返回 myapp.jar
            // IDE 调试时 → 返回 out/production/classes
            val file = if (path.isFile) path.parentFile else path
            if (file.exists()) return file
        }

        // 2) GraalVM native-image 下，ProcessHandle 能返回当前 exe 路径
        val exe = ProcessHandle.current().info().command().orElse(null)
        if (exe != null && exe.endsWith(".exe") || exe.contains("/")) {
            val f = File(exe)
            if (f.exists()) return f.parentFile
        }

        // 3) 兜底：使用工作目录
        return File(System.getProperty("user.dir"))
    }
}
