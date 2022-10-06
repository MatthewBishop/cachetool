# cachelib

Usage
There is a Cache Checker and Cache Repacker in the tools package.

Put the cache you are wanting to pack to in the dest folder
Put the cache you are wanting to pack from in the source folder

Click the CacheRepacker.jar

You can also run it via java -jar CacheRepacker.jar

Cache completion can be checked with java -jar CacheChecker.jar

Sources
Some code is derived from Tom's Cache Suite for archive repacking.
Index/buffer/bzip classes are from major's renamed 317 client.
JagBZip2OutputStream is a modified version of Itadaki Jbzip2 to skip the first 4 bytes/header.

Other alternatives were:
Tom's Cache Suite
https://github.com/Rune-Status/nshusa-rsam
https://github.com/scape-tools/scape-editor/blob/master/fs/src/main/java/scape/editor/fs/RSArchive.java
https://github.com/Displee/rs-cache-library/blob/master/src/main/kotlin/com/displee/cache/index/archive/Archive317.kt