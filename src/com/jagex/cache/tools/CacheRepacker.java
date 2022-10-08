package com.jagex.cache.tools;

import com.jagex.cache.Cache;

/*
 * Notes:
 * 
 * 317 used its own animation structure. this was shared with 319. this was not used in 308 or 321.
 */
public class CacheRepacker {

	/* After packing 319
	 * 
None of the models missing are female item related.

TODO: Find what the models are used for and try packing 377 to see if they work with existing anims.


Missing model: 138-npc girl chathead
Missing model: 255-some type of legs
Missing model: 272-some type of legs
Missing model: 300-some type of chest with a gut, no pauldrons, iron color
Missing model: 304-male model no beard, hard leather
Missing model: 333-male arms, no shoulders
Missing model: 340-male arms
Missing model: 349-red demon arms
Missing model: 461-some type of chest with a gut, no pauldrons, iron color
Missing model: 463-some type of chest with a gut, no pauldrons, steel color
Missing model: 476-some type of plate chest with beer gut
Missing model: 498-looks like a horizontal shark fin
Missing model: 2859-some type of fairy
Missing model: 2895-some type of female upper body
Missing model: 2912-some type of legs
Missing model: 2954-some npc chest? Goblin diplomacy?
Missing model: 3008-some type of cat head
Missing model: 3331-some type of shoes with spurs
Missing model: 4382
Missing model: 4605
Missing model: 4606
Missing model: 4926
Missing model: 4985
Missing model: 5021
Missing model: 5217
Missing model: 5414
Missing model: 5483
Missing model: 6037
Missing model: 6038
Missing model: 6129
Missing model: 6132
Missing model: 6145
Missing model: 6213
Missing model: 6216
Missing model: 6309
Missing model: 6310
Missing model: 6466
Missing model: 6471
Missing model: 6649
Missing model: 6846
Missing model: 6847
Missing model: 6849
Missing model: 6851
Missing model: 6975
Missing model: 6976
Missing model: 6978
Missing model: 6979
Missing model: 6981
Missing model: 7049
Missing model: 7237
Missing model: 7248
Missing midi: 375
Missing midi: 401
Missing midi: 415
Missing midi: 419
Missing midi: 434
Total missing: 56


	 */
	public static void main(String[] args) throws Exception {
		Cache cache = new Cache("./dest/");
	//	Cache source = new Cache("./source/");
		
	//	Cache source = new Cache("./data/321/cache/");
		Cache source2 = new Cache("./data/fs319/");
		
	//	cache.update(source);
		cache.update(source2);
		cache.rebuild();
	}

}
