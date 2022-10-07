package com.jagex.cache.configs;

import java.io.PrintWriter;

import com.jagex.cache.Archive;
import com.jagex.cache.Cache;
import com.jagex.cache.util.Buffer;

/**
 * Ref:
 * 
 * https://www.rune-server.ee/runescape-development/rs2-server/tutorials/343317-testing-player-appearance-design-flaw-3.html#post2857911
 * https://github.com/Pazaz/RS2-225/blob/main/runetek3/src/main/java/com/jagex/game/runetek3/config/IdkType.java
 * https://github.com/Pazaz/RS2-225/blob/5196c2a15fe03ab507ec81804fddd01e1f5a3e3c/server/src/main/java/com/itspazaz/lostcity/tools/IdkDump.java
 * 
 * @author Advocatus, Pazaz
 *
 */
public class IdentityKit {

    public static void main(String[] args) {
        try {
    		Cache cache = new Cache("./dest/");
    		byte[] orig = cache.indices[0].decompress(2);    	
    		
    		Archive configs = new Archive(orig);
    		
    		init(configs);

            try {
                PrintWriter out = new PrintWriter("./dump/idk.def");

                for (int i = 0; i < IdentityKit.count; ++i) {
                    out.println(IdentityKit.kits[i].toJagConfig());
                }

                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
	/*
	 * Males are type 0-6
	 * Females are type 7-13
	 * 
	 * types are head-jaw-torso-arms-hands-legs-feet
	 */
	public static int count;
	public static IdentityKit[] kits;

	/*
            StreamLoader streamLoader = streamLoaderForName(2, "config", "config", expectedCRCs[2], 30);
            StreamLoader streamLoader_1 = streamLoaderForName(3, "interface", "interface", expectedCRCs[3], 35);
            StreamLoader streamLoader_2 = streamLoaderForName(4, "2d graphics", "media", expectedCRCs[4], 40);
            StreamLoader streamLoader_6 = streamLoaderForName(5, "update list", "versionlist", expectedCRCs[5], 60);
            StreamLoader streamLoader_3 = streamLoaderForName(6, "textures", "textures", expectedCRCs[6], 45);
            StreamLoader streamLoader_4 = streamLoaderForName(7, "chat system", "wordenc", expectedCRCs[7], 50);
            StreamLoader streamLoader_5 = streamLoaderForName(8, "sound effects", "sounds", expectedCRCs[8], 55);
	 */
	public static void init(Archive archive) {
		Buffer buffer = new Buffer(archive.getEntry("idk.dat"));
		count = buffer.readUShort();
		if (kits == null) {
			kits = new IdentityKit[count];
		}

		for (int id = 0; id < count; id++) {
			if (kits[id] == null) {
				kits[id] = new IdentityKit();
			}
			kits[id].id = id;
			kits[id].decode(buffer);
		}
	}

	private int[] models;
	private int[] headModels = { -1, -1, -1, -1, -1 };
	private int[] recol_s = new int[6];
	private int type = -1;
	private int[] recol_d = new int[6];
	private boolean disable = false;
	private int id;

//	public boolean bodyLoaded() {
//		if (models == null) {
//			return true;
//		}
//
//		for (int part = 0; part < models.length; part++) {
//			if (!Model.loaded(models[part])) {
//				return false;
//			}
//		}
//
//		return true;
//	}
//
//	public Model bodyModel() {
//		if (models == null) {
//			return null;
//		}
//
//		Model[] models = new Model[models.length];
//		for (int part = 0; part < models.length; part++) {
//			models[part] = Model.lookup(models[part]);
//		}
//
//		Model model;
//		if (models.length == 1) {
//			model = models[0];
//		} else {
//			model = new Model(models.length, models);
//		}
//
//		for (int part = 0; part < 6; part++) {
//			if (recol_s[part] == 0) {
//				break;
//			}
//
//			model.recolour(recol_s[part], recol_d[part]);
//		}
//
//		return model;
//	}

	public void decode(Buffer buffer) {
		do {
			int opcode = buffer.readUByte();
			if (opcode == 0) {
				return;
			}

			if (opcode == 1) {
				type = buffer.readUByte();
			} else if (opcode == 2) {
				int count = buffer.readUByte();
				models = new int[count];
				for (int part = 0; part < count; part++) {
					models[part] = buffer.readUShort();
				}
			} else if (opcode == 3) {
				disable = true;
			} else if (opcode >= 40 && opcode < 50) {
				recol_s[opcode - 40] = buffer.readUShort();
			} else if (opcode >= 50 && opcode < 60) {
				recol_d[opcode - 50] = buffer.readUShort();
			} else if (opcode >= 60 && opcode < 70) {
				headModels[opcode - 60] = buffer.readUShort();
			} else {
				System.out.println("Error unrecognised config code: " + opcode);
			}
		} while (true);
	}

	
	public String toJagConfig() {
		StringBuilder builder = new StringBuilder();

		builder.append("[idk_").append(this.id).append("]\n");

		if (this.type != -1) {
			String typeName = Integer.toString(this.type);
			switch (this.type) {
				case 0:
					typeName = "BODYPART_MALE_HAIR";
					break;
				case 1:
					typeName = "BODYPART_MALE_JAW";
					break;
				case 2:
					typeName = "BODYPART_MALE_TORSO";
					break;
				case 3:
					typeName = "BODYPART_MALE_ARMS";
					break;
				case 4:
					typeName = "BODYPART_MALE_HANDS";
					break;
				case 5:
					typeName = "BODYPART_MALE_LEGS";
					break;
				case 6:
					typeName = "BODYPART_MALE_FEET";
					break;
				case 7:
					typeName = "BODYPART_FEMALE_HAIR";
					break;
				// 8 is probably FEMALE_JAW but it isn't used
				case 9:
					typeName = "BODYPART_FEMALE_TORSO";
					break;
				case 10:
					typeName = "BODYPART_FEMALE_ARMS";
					break;
				case 11:
					typeName = "BODYPART_FEMALE_HANDS";
					break;
				case 12:
					typeName = "BODYPART_FEMALE_LEGS";
					break;
				case 13:
					typeName = "BODYPART_FEMALE_FEET";
					break;
			}
			builder.append("bodypart=^").append(typeName).append("\n");
		}

		if (this.disable) {
			builder.append("disable=yes\n");
		}

		if (this.models != null) {
			for (int i = 0; i < this.models.length; ++i) {
				builder.append("model").append(i + 1).append("=model_").append(this.models[i]).append("\n");
			}
		}

		for (int i = 0; i < this.headModels.length; ++i) {
			if (this.headModels[i] == -1) {
				continue;
			}

			builder.append("head").append(i + 1).append("=model_").append(this.headModels[i]).append("\n");
		}

		for (int i = 0; i < this.recol_s.length; ++i) {
			if (this.recol_s[i] == 0) {
				continue;
			}

			builder.append("recol").append(i + 1).append("s=").append(this.recol_s[i]).append("\n");
			builder.append("recol").append(i + 1).append("d=").append(this.recol_d[i]).append("\n");
		}

		return builder.toString();
	}
	
//	public int[] getBodyModels() {
//		return models;
//	}
//
//	public int[] getHeadModels() {
//		return headModels;
//	}
//
//	public int[] getOriginalColours() {
//		return recol_s;
//	}
//
//	public int getPart() {
//		return type;
//	}
//
//	public int[] getReplacementColours() {
//		return recol_d;
//	}
//
//	public Model headModel() {
//		Model[] models = new Model[5];
//		int count = 0;
//		for (int part = 0; part < 5; part++) {
//			if (headModels[part] != -1) {
//				models[count++] = Model.lookup(headModels[part]);
//			}
//		}
//
//		Model model = new Model(count, models);
//		for (int part = 0; part < 6; part++) {
//			if (recol_s[part] == 0) {
//				break;
//			}
//
//			model.recolour(recol_s[part], recol_d[part]);
//		}
//
//		return model;
//	}
//
//	public boolean isValidStyle() {
//		return disable;
//	}
//
//	public boolean loaded() {
//		for (int part = 0; part < 5; part++) {
//			if (headModels[part] != -1 && !Model.loaded(headModels[part])) {
//				return false;
//			}
//		}
//
//		return true;
//	}
//
//	public void setBodyModels(int[] bodyModels) {
//		this.models = bodyModels;
//	}
//
//	public void setHeadModels(int[] headModels) {
//		this.headModels = headModels;
//	}
//
//	public void setOriginalColours(int[] originalColours) {
//		this.recol_s = originalColours;
//	}
//
//	public void setPart(int part) {
//		this.type = part;
//	}
//
//	public void setReplacementColours(int[] replacementColours) {
//		this.recol_d = replacementColours;
//	}
//
//	public void setValidStyle(boolean validStyle) {
//		this.disable = validStyle;
//	}
}