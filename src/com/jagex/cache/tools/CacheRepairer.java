package com.jagex.cache.tools;

import com.jagex.cache.Cache;

public class CacheRepairer {

	public static void main(String[] args) throws Exception {
		Cache cache = new Cache("./dest/");
		
		cache.clean("./clean/");
	}
}
