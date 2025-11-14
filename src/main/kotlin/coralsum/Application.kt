package coralsum

import coralsum.toolkit.RuntimePath
import io.micronaut.runtime.Micronaut.run
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.h2.tools.Server

@OpenAPIDefinition(
    info = Info(
        title = "coralsum",
        version = "0.0"
    )
)
object Api

fun main(args: Array<String>) {
    initProps()
    run(*args)
    startH2Console()
}

fun initProps() {
    val absolutePath = RuntimePath.getExecutableDir().absolutePath
    System.setProperty("user.dir", absolutePath)
    System.setProperty("store.base", absolutePath.replace("\\", "/"))
}

fun startH2Console() {
    val h2Console = Server.createWebServer("-web", "-webPort", "8082")
    h2Console.start()
    println("H2 console started at http://localhost:8082")
}
