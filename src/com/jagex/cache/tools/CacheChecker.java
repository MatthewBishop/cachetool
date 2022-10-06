package com.jagex.cache.tools;

import com.jagex.cache.Cache;

public class CacheChecker {

	public static void main(String[] args) throws Exception {
		Cache cache = new Cache("./dest/");
		
		cache.check();
	}

}
