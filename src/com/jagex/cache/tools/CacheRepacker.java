package com.jagex.cache.tools;

import com.jagex.cache.Cache;

public class CacheRepacker {

	public static void main(String[] args) throws Exception {
		Cache cache = new Cache("./dest/");
		Cache source = new Cache("./source/");
		
		cache.update(source);
		cache.rebuild();
	}

}
