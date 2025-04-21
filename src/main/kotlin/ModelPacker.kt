package dev.openrune

import com.jagex.cache.Cache
import java.io.File

object ModelPacker {

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = "C:\\Users\\Home\\Downloads\\cache-runescape-live-en-b357-b358-b359-2006-02-07-00-00-00-openrs2#652\\cache\\"
        val cache = Cache(dir)
        val models = loadModels("C:\\Users\\Home\\Downloads\\All Data Releases\\474 Data\\474 Items\\")
        models.forEach {
            cache.writeFile(1, it.first, it.second)
        }
        cache.rebuildModels()
    }

    fun loadModels(dirPath: String): List<Pair<Int, ByteArray>> {
        val dir = File(dirPath).also { require(it.isDirectory) { "Not a directory: $dirPath" } }
        return dir.walk()
            .filter { it.isFile }
            .mapNotNull { file ->
                val id = file.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null
                id to file.readBytes()
            }.toList()
    }
}