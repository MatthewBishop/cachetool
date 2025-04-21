package dev.openrune

import com.jagex.cache.Archive
import com.jagex.cache.Cache


fun main() {
    val dir = "C:\\Users\\Home\\Downloads\\cache-runescape-live-en-b357-b358-b359-2006-02-07-00-00-00-openrs2#652\\cache\\"
    val cache = Cache(dir)
    val versionlist = Archive(cache.getFile(0, 5))
    val data = versionlist.getEntry("model_index");
    println(data.toUString())
    println(uniques.sorted())
}

val uniques = HashSet<Int>()

fun ByteArray.toUString(): String {
    val iMax = size - 1
    if (iMax == -1) return "[]"

    val b = StringBuilder()
    b.append('[')
    var i = 0
    while (true) {
        val value = this[i].toInt() and 0xff
        uniques.add(value)
        b.append(value)
        if (i == iMax) return b.append(']').toString()
        b.append(", ")
        i++
    }
}