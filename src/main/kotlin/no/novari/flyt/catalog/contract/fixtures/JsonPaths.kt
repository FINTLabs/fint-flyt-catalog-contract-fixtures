package no.novari.flyt.catalog.contract.fixtures

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException

internal object JsonPaths {
    private val configuration: Configuration =
        Configuration
            .defaultConfiguration()
            .addOptions(Option.SUPPRESS_EXCEPTIONS)

    fun prune(
        json: String,
        paths: List<String>,
    ): String {
        if (paths.isEmpty()) {
            return json
        }

        var context = JsonPath.using(configuration).parse(json)
        paths.forEach { path ->
            context =
                try {
                    context.delete(path)
                } catch (_: PathNotFoundException) {
                    context
                }
        }
        return context.jsonString()
    }
}
