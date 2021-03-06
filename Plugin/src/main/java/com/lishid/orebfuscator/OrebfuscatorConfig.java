/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.cache.ObfuscatedDataCache;

public class OrebfuscatorConfig {
	private static class MaterialResult {
		public int id;
		public String name;
		
		public MaterialResult(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
    // Constant/persistent data
    private static final int CONFIG_VERSION = 12;
    private static Random random = new Random();

    // Main engine config
    public static boolean Enabled = true;
    public static boolean UpdateOnDamage = true;
    public static int EngineMode = 2;
    public static int InitialRadius = 1;
    public static int UpdateRadius = 2;
    public static boolean UseWorldsAsBlacklist = true;
    public static boolean NoObfuscationForMetadata = true;
    public static String NoObfuscationForMetadataTagName = "NPC";

    // Darkness
    public static boolean DarknessHideBlocks = false;

    // Caching
    public static boolean UseCache = true;
    public static int MaxLoadedCacheFiles = 64;
    public static String CacheLocation = "orebfuscator_cache";
    public static File CacheFolder = new File(Bukkit.getServer().getWorldContainer(), CacheLocation);
    public static int DeleteCacheFilesAfterDays = 0;

    // ProximityHider
    public static int ProximityHiderRate = 500;
    public static int ProximityHiderDistance = 8;
    public static int ProximityHiderID = 1;
    public static int ProximityHiderEnd = 255;
    public static boolean UseProximityHider = true;
    public static boolean UseSpecialBlockForProximityHider = true;
    public static boolean UseYLocationProximity = false;

    // AntiTexturePackAndFreecam
    public static boolean AntiTexturePackAndFreecam = true;
    public static int AirGeneratorMaxChance = 43;

    // Misc
    public static boolean NoObfuscationForOps = false;
    public static boolean NoObfuscationForPermission = false;
    public static boolean LoginNotification = true;

    // Anti Hit Hack
    public static int AntiHitHackDecrementFactor = 1000;
    public static int AntiHitHackMaxViolation = 15;

    // Utilities
    private static boolean[] ObfuscateBlocks = new boolean[256];
    private static boolean[] NetherObfuscateBlocks = new boolean[256];
    private static boolean[] DarknessBlocks = new boolean[256];
    private static boolean[] ProximityHiderBlocks = new boolean[256];
    private static Integer[] RandomBlocks = new Integer[]{1, 4, 5, 14, 15, 16, 21, 46, 48, 49, 56, 73, 82, 129};
    private static Integer[] NetherRandomBlocks = new Integer[]{13, 87, 88, 112, 153};
    private static Integer[] RandomBlocks2 = RandomBlocks;
    private static List<String> Worlds = new ArrayList<String>();

    // Palette
    public static int[] NetherPaletteBlocks;
    public static int[] NormalPaletteBlocks;
    
    // ChunkReloader
    public static int ChunkReloaderRate = 500;
    public static boolean UseChunkReloader = false;

    // CacheCleaner
    public static long CacheCleanRate = 60 * 60 * 20;//once per hour

    public static File getCacheFolder() {
        // Try to make the folder
        if (!CacheFolder.exists()) {
            CacheFolder.mkdirs();
        }
        // Can't make folder? Use default
        if (!CacheFolder.exists()) {
            CacheFolder = new File("orebfuscator_cache");
        }
        return CacheFolder;
    }

    private static boolean[] TransparentBlocks;
    private static boolean[] TransparentBlocksMode1;
    private static boolean[] TransparentBlocksMode2;

    public static boolean isBlockTransparent(int id) {
        if (id < 0)
            id += 256;

        if (id >= 256) {
            return false;
        }

        return TransparentBlocks[id];
    }
    
    private static void generateTransparentBlocks() {
    	if(TransparentBlocks == null) {
    		readInitialTransparentBlocks();
    	}
    	
    	boolean[] transparentBlocks = EngineMode == 1
    			? TransparentBlocksMode1
    			: TransparentBlocksMode2;
    	
    	System.arraycopy(transparentBlocks, 0, TransparentBlocks, 0, TransparentBlocks.length);
    	
    	List<Integer> customTransparentBlocks = getMaterialIdsByPath("Lists.TransparentBlocks", new Integer[]{ });
    	
    	for(int blockId : customTransparentBlocks) {
    		if(blockId >= 0 && blockId <= 255) {
    			TransparentBlocks[blockId] = true;
    		}
    	}
    	
    	List<Integer> customNonTransparentBlocks = getMaterialIdsByPath("Lists.NonTransparentBlocks", new Integer[]{ });

    	for(int blockId : customNonTransparentBlocks) {
    		if(blockId >= 0 && blockId <= 255) {
    			TransparentBlocks[blockId] = false;
    		}
    	}
    }
    
    private static void readInitialTransparentBlocks() {
    	TransparentBlocks = new boolean[256];
    	Arrays.fill(TransparentBlocks, false);
    	
		InputStream mainStream = Orebfuscator.class.getResourceAsStream("/resources/transparent_blocks.txt");
		readTransparentBlocks(TransparentBlocks, mainStream);
    	
		TransparentBlocksMode1 = new boolean[256];
		System.arraycopy(TransparentBlocks, 0, TransparentBlocksMode1, 0, TransparentBlocksMode1.length);
		InputStream mode1Stream = Orebfuscator.class.getResourceAsStream("/resources/transparent_blocks_mode1.txt");
		if(mode1Stream != null) readTransparentBlocks(TransparentBlocksMode1, mode1Stream);

		TransparentBlocksMode2 = new boolean[256];
		System.arraycopy(TransparentBlocks, 0, TransparentBlocksMode2, 0, TransparentBlocksMode2.length);
		InputStream mode2Stream = Orebfuscator.class.getResourceAsStream("/resources/transparent_blocks_mode2.txt");
		if(mode2Stream != null) readTransparentBlocks(TransparentBlocksMode2, mode2Stream);
    }
    
    private static void readTransparentBlocks(boolean[] transparentBlocks, InputStream stream) {
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    		String line;
    		
            while ((line = reader.readLine()) != null) { 
            	int index1 = line.indexOf(":");
            	int index2 = line.indexOf(" ", index1);
            	int blockId = Integer.parseInt(line.substring(0,  index1));
            	boolean isTransparent = line.substring(index1 + 1, index2).equals("true");
            	
            	transparentBlocks[blockId] = isTransparent;
            }
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }

    public static boolean isObfuscated(int id, World.Environment environment) {
        if (id < 0)
            id += 256;

        // Nether case
        if (environment == World.Environment.NETHER) {
            return id == 87 || NetherObfuscateBlocks[id];
        }

        // Normal case
        return id == 1 || ObfuscateBlocks[id];
    }

    public static boolean isDarknessObfuscated(int id) {
        if (id < 0)
            id += 256;

        return DarknessBlocks[id];
    }

    public static boolean isProximityObfuscated(int y, int id) {
        if (id < 0)
            id += 256;

        return ProximityHiderBlocks[id];
    }

    public static boolean isProximityHiderOn(int y, int id) {
        return (UseYLocationProximity && y >= ProximityHiderEnd) ||
                (!UseYLocationProximity && y <= ProximityHiderEnd);
    }

    public static boolean skipProximityHiderCheck(int y) {
        return UseYLocationProximity && y < ProximityHiderEnd;
    }

    public static boolean proximityHiderDeobfuscate() {
        return UseYLocationProximity;
    }

    public static boolean isWorldDisabled(String name) {
        for (String world : Worlds) {
            if (world.equalsIgnoreCase(name)) {
                return UseWorldsAsBlacklist;
            }
        }
        return !UseWorldsAsBlacklist;
    }

    public static String getWorlds() {
        String retval = "";
        for (String world : Worlds) {
            retval += world + ", ";
        }
        return retval.length() > 1 ? retval.substring(0, retval.length() - 2) : retval;
    }

    public static int getRandomBlock(int index, boolean alternate, World.Environment environment) {
        if (environment == World.Environment.NETHER)
            return (int) (NetherRandomBlocks[index]);
        return (int) (alternate ? RandomBlocks2[index] : RandomBlocks[index]);
    }

    public static Integer[] getRandomBlocks(boolean alternate, World.Environment environment) {
        if (environment == World.Environment.NETHER)
            return NetherRandomBlocks;
        return (alternate ? RandomBlocks2 : RandomBlocks);
    }

    public static void shuffleRandomBlocks() {
        synchronized (RandomBlocks) {
            Collections.shuffle(Arrays.asList(RandomBlocks));
            Collections.shuffle(Arrays.asList(RandomBlocks2));
        }
    }

    public static int random(int max) {
        return random.nextInt(max);
    }

    // Set

    public static void setEngineMode(int data) {
        setData("Integers.EngineMode", data);
        EngineMode = data;
    }

    public static void setUpdateRadius(int data) {
        setData("Integers.UpdateRadius", data);
        UpdateRadius = data;
    }

    public static void setInitialRadius(int data) {
        setData("Integers.InitialRadius", data);
        InitialRadius = data;
    }

    public static void setProximityHiderDistance(int data) {
        setData("Integers.ProximityHiderDistance", data);
        ProximityHiderDistance = data;
    }

    public static void setAirGeneratorMaxChance(int data) {
        setData("Integers.AirGeneratorMaxChance", data);
        AirGeneratorMaxChance = data;
    }

    public static void setUseProximityHider(boolean data) {
        setData("Booleans.UseProximityHider", data);
        UseProximityHider = data;
    }

    public static void setDarknessHideBlocks(boolean data) {
        setData("Booleans.DarknessHideBlocks", data);
        DarknessHideBlocks = data;
    }

    public static void setNoObfuscationForOps(boolean data) {
        setData("Booleans.NoObfuscationForOps", data);
        NoObfuscationForOps = data;
    }

    public static void setNoObfuscationForPermission(boolean data) {
        setData("Booleans.NoObfuscationForPermission", data);
        NoObfuscationForPermission = data;
    }

    public static void setLoginNotification(boolean data) {
        setData("Booleans.LoginNotification", data);
        LoginNotification = data;
    }

    public static void setAntiTexturePackAndFreecam(boolean data) {
        setData("Booleans.AntiTexturePackAndFreecam", data);
        AntiTexturePackAndFreecam = data;
    }

    public static void setUseCache(boolean data) {
        setData("Booleans.UseCache", data);
        UseCache = data;
    }

    public static void setEnabled(boolean data) {
        setData("Booleans.Enabled", data);
        Enabled = data;
    }

    public static void setUseWorldsAsBlacklist(boolean data) {
        setData("Booleans.UseWorldsAsBlacklist", data);
        UseWorldsAsBlacklist = data;
    }

    public static void setWorldEnabled(String name, boolean enabled) {
    	if (enabled && !UseWorldsAsBlacklist || !enabled && UseWorldsAsBlacklist) {
            Worlds.add(name);
        } else {
            Worlds.remove(name);
        }
        setData("Lists.Worlds", Worlds);
    }

    private static boolean getBoolean(String path, boolean defaultData) {
        if (getConfig().get(path) == null)
            setData(path, defaultData);
        return getConfig().getBoolean(path, defaultData);
    }

    private static String getString(String path, String defaultData) {
        if (getConfig().get(path) == null)
            setData(path, defaultData);
        return getConfig().getString(path, defaultData);
    }

    private static int getInt(String path, int defaultData) {
        if (getConfig().get(path) == null)
            setData(path, defaultData);
        return getConfig().getInt(path, defaultData);
    }

    private static List<String> getStringList(String path, List<String> defaultData) {
        if (getConfig().get(path) == null)
            setData(path, defaultData);
        return getConfig().getStringList(path);
    }

    private static void setData(String path, Object data) {
        try {
            getConfig().set(path, data);
            save();
        } catch (Exception e) {
            Orebfuscator.log(e);
        }
    }

    private static void setBlockValues(boolean[] boolArray, List<Integer> blocks, boolean transparent) {
        for (int i = 0; i < boolArray.length; i++) {
            boolArray[i] = blocks.contains(i);

            // If block is transparent while we don't want them to, or the other way around
            if (transparent != isBlockTransparent((short) i)) {
                // Remove it
                boolArray[i] = false;
            }
        }
    }

    private static void setBlockValues(boolean[] boolArray, List<Integer> blocks) {
        for (int i = 0; i < boolArray.length; i++) {
            boolArray[i] = blocks.contains(i);
        }
    }

    public static void load() {

        // Version check
        int version = getInt("ConfigVersion", CONFIG_VERSION);
        if (version < CONFIG_VERSION) {
            setData("ConfigVersion", CONFIG_VERSION);
        }

        EngineMode = getInt("Integers.EngineMode", EngineMode);
        if (EngineMode != 1 && EngineMode != 2) {
            EngineMode = 2;
            Orebfuscator.log("EngineMode must be 1 or 2.");
        }

        InitialRadius = clamp(getInt("Integers.InitialRadius", InitialRadius), 0, 2);
        if (InitialRadius == 0) {
            Orebfuscator.log("Warning, InitialRadius is 0. This will cause all exposed blocks to be obfuscated.");
        }

        UpdateRadius = clamp(getInt("Integers.UpdateRadius", UpdateRadius), 1, 5);
        MaxLoadedCacheFiles = clamp(getInt("Integers.MaxLoadedCacheFiles", MaxLoadedCacheFiles), 16, 128);
        ProximityHiderDistance = clamp(getInt("Integers.ProximityHiderDistance", ProximityHiderDistance), 2, 64);

        ProximityHiderID = getMaterialIdByPath("Integers.ProximityHiderID", ProximityHiderID);
        ProximityHiderEnd = clamp(getInt("Integers.ProximityHiderEnd", ProximityHiderEnd), 0, 255);
        AirGeneratorMaxChance = clamp(getInt("Integers.AirGeneratorMaxChance", AirGeneratorMaxChance), 40, 100);
        UseProximityHider = getBoolean("Booleans.UseProximityHider", UseProximityHider);
        UseSpecialBlockForProximityHider = getBoolean("Booleans.UseSpecialBlockForProximityHider", UseSpecialBlockForProximityHider);
        UseYLocationProximity = getBoolean("Booleans.UseYLocationProximity", UseYLocationProximity);
        UpdateOnDamage = getBoolean("Booleans.UpdateOnDamage", UpdateOnDamage);
        DarknessHideBlocks = getBoolean("Booleans.DarknessHideBlocks", DarknessHideBlocks);
        NoObfuscationForOps = getBoolean("Booleans.NoObfuscationForOps", NoObfuscationForOps);
        NoObfuscationForPermission = getBoolean("Booleans.NoObfuscationForPermission", NoObfuscationForPermission);
        UseCache = getBoolean("Booleans.UseCache", UseCache);
        LoginNotification = getBoolean("Booleans.LoginNotification", LoginNotification);
        AntiTexturePackAndFreecam = getBoolean("Booleans.AntiTexturePackAndFreecam", AntiTexturePackAndFreecam);
        UseWorldsAsBlacklist = getBoolean("Booleans.UseWorldsAsBlacklist", UseWorldsAsBlacklist);
        Enabled = getBoolean("Booleans.Enabled", Enabled);
        NoObfuscationForMetadata = getBoolean("Booleans.NoObfuscationForMetadata", NoObfuscationForMetadata);
        NoObfuscationForMetadataTagName = getString("Strings.NoObfuscationForMetadataTagName", NoObfuscationForMetadataTagName);
        DeleteCacheFilesAfterDays = getInt("Integers.DeleteCacheFilesAfterDays", DeleteCacheFilesAfterDays);
        //UseChunkReloader = getBoolean("Booleans.UseChunkReloader", UseChunkReloader);

        generateTransparentBlocks();

        // Read block lists
        setBlockValues(ObfuscateBlocks, getMaterialIdsByPath("Lists.ObfuscateBlocks", new Integer[]{14, 15, 16, 21, 54, 56, 73, 74, 129, 130}), false);
        setBlockValues(NetherObfuscateBlocks, getMaterialIdsByPath("Lists.NetherObfuscateBlocks", new Integer[]{87, 153}), false);
        setBlockValues(DarknessBlocks, getMaterialIdsByPath("Lists.DarknessBlocks", new Integer[]{52, 54}));
        setBlockValues(ProximityHiderBlocks, getMaterialIdsByPath("Lists.ProximityHiderBlocks", new Integer[]{23, 52, 54, 56, 58, 61, 62, 116, 129, 130, 145, 146}));

        //Support old DisabledWorlds value
        if(getConfig().get("Lists.DisabledWorlds") != null) {
        	Worlds = getConfig().getStringList("Lists.DisabledWorlds");
        	getConfig().set("Lists.DisabledWorlds", null);
        }

        // List of worlds (either disabled or enabled depending on WorldsAsBlacklist value).
        Worlds = getStringList("Lists.Worlds", Worlds);
        
        // Read the cache location
        CacheLocation = getString("Strings.CacheLocation", CacheLocation);
        CacheFolder = new File(CacheLocation);

        RandomBlocks = getMaterialIdsByPath("Lists.RandomBlocks", RandomBlocks).toArray(new Integer[0]);
        NetherRandomBlocks = getMaterialIdsByPath("Lists.NetherRandomBlocks", NetherRandomBlocks).toArray(new Integer[0]);

        // Validate RandomBlocks
        for (int i = 0; i < RandomBlocks.length; i++) {
            // Don't want people to put chests and other stuff that lags the hell out of players.
            if (RandomBlocks[i] == null || OrebfuscatorConfig.isBlockTransparent((short) (int) RandomBlocks[i])) {
                RandomBlocks[i] = 1;
            }
        }
        RandomBlocks2 = RandomBlocks;

        save();
        
        createPaletteBlocks();
        
        Orebfuscator.nms.setMaxLoadedCacheFiles(MaxLoadedCacheFiles);
        
        //Make sure cache is cleared if config was changed since last start
        try {
			ObfuscatedDataCache.checkCacheAndConfigSynchronized();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private static int getMaterialIdByPath(String path, int defaultMaterialId) {
    	String materialName = getConfig().get(path) != null ? getConfig().getString(path): Integer.toString(defaultMaterialId);
    	MaterialResult material = getMaterial(materialName, defaultMaterialId);
    	
    	setData(path, material.name);
		
		return material.id;
    }
    
    private static List<Integer> getMaterialIdsByPath(String path, Integer[] defaultMaterials) {
    	List<String> list;
    	
    	if(getConfig().get(path) != null) {
    		list = getConfig().getStringList(path);
    	} else {
    		list = new ArrayList<String>();
    		
    		for(int materialId : defaultMaterials) {
    			list.add(DeprecatedMethods.getMaterial(materialId).name());
    		}
    	}
    	
    	List<Integer> result = new ArrayList<Integer>();
    	
    	for(int i = 0; i < list.size(); i++) {
    		MaterialResult material = getMaterial(list.get(i), null);
    		
    		if(material != null) {
    			list.set(i, material.name);
    			result.add(material.id);
    		}
    	}
    	
    	setData(path, list);
    	
    	return result;
    }
    
    private static MaterialResult getMaterial(String materialName, Integer defaultMaterialId) {
    	Integer materialId;
    	String defaultMaterialName = defaultMaterialId != null ? DeprecatedMethods.getMaterial(defaultMaterialId).name(): null;
    	
		try {
    		if(Character.isDigit(materialName.charAt(0))) {
    			materialId = Integer.parseInt(materialName);
    			
    			Material obj = DeprecatedMethods.getMaterial(materialId);
    			
    			if(obj != null) {
    				materialName = obj.name();
    			} else {
    				if(defaultMaterialId != null) {
	    				Orebfuscator.log("Material with ID = " + materialId + " is not found. Will be used default material: " + defaultMaterialName);
	    				materialId = defaultMaterialId;
	    				materialName = defaultMaterialName;
    				} else {
    					Orebfuscator.log("Material with ID = " + materialId + " is not found. Skipped.");
    					materialId = null;
    				}
    			}
    		} else {
    			Material obj = Material.getMaterial(materialName.toUpperCase());
    			
    			if(obj != null) {
    				materialId = DeprecatedMethods.getMaterialId(obj);
    			} else {
    				if(defaultMaterialId != null) {
	    				Orebfuscator.log("Material " + materialName + " is not found. Will be used default material: " + defaultMaterialName);
	    				materialId = defaultMaterialId;
	    				materialName = defaultMaterialName;
    				} else {
    					Orebfuscator.log("Material " + materialName + " is not found. Skipped.");
    					materialId = null;
    				}
    			}
    		}
		} catch (Exception e) {
			if(defaultMaterialId != null) {
				Orebfuscator.log("Invalid material ID or name: " + materialName + ".  Will be used default material: " + defaultMaterialName);
				materialId = defaultMaterialId;
				materialName = defaultMaterialName;
			} else {
				Orebfuscator.log("Invalid material ID or name: " + materialName + ". Skipped.");
				materialId = null;
			}
		}
		
		return materialId != null ? new MaterialResult(materialId, materialName): null;
    }
    
    private static void createPaletteBlocks() {
    	//Nether
    	ArrayList<Integer> nether = new ArrayList<Integer>();
    	
    	nether.add(0);
    	nether.add(87);
    	
    	if(ProximityHiderID != 0 && ProximityHiderID != 87) {
    		nether.add(ProximityHiderID);
    	}
    	
    	for(Integer id : NetherRandomBlocks) {
    		if(id != null && nether.indexOf(id) < 0) {
    			nether.add(id);
    		}
    	}
    	
    	NetherPaletteBlocks = new int[nether.size()];
    	for(int i = 0; i < NetherPaletteBlocks.length; i++) NetherPaletteBlocks[i] = nether.get(i);

    	//Normal
    	ArrayList<Integer> normal = new ArrayList<Integer>();
    	
    	normal.add(0);
    	normal.add(1);
    	
    	if(ProximityHiderID != 0 && ProximityHiderID != 1) {
    		normal.add(ProximityHiderID);
    	}
    	
    	for(Integer id : RandomBlocks) {
    		if(id != null && normal.indexOf(id) < 0) {
    			normal.add(id);
    		}
    	}
    	
    	for(Integer id : RandomBlocks2) {
    		if(id != null && normal.indexOf(id) < 0) {
    			normal.add(id);
    		}
    	}

    	NormalPaletteBlocks = new int[normal.size()];
    	for(int i = 0; i < NormalPaletteBlocks.length; i++) NormalPaletteBlocks[i] = normal.get(i);
    }

    public static void reload() {
        Orebfuscator.instance.reloadConfig();
        load();
    }

    public static void save() {
        Orebfuscator.instance.saveConfig();
    }

    public static boolean obfuscateForPlayer(Player player) {
        return !(playerBypassOp(player) || playerBypassPerms(player) || playerBypassMetadata(player));
    }

    public static boolean playerBypassOp(Player player) {
        boolean ret = false;
        try {
            ret = OrebfuscatorConfig.NoObfuscationForOps && player.isOp();
        } catch (Exception e) {
            Orebfuscator.log("Error while obtaining Operator status for player" + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean playerBypassPerms(Player player) {
        boolean ret = false;
        try {
            ret = OrebfuscatorConfig.NoObfuscationForPermission && player.hasPermission("Orebfuscator.deobfuscate");
        } catch (Exception e) {
            Orebfuscator.log("Error while obtaining permissions for player" + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean playerBypassMetadata(Player player) {
        boolean ret = false;
        try {
            ret = OrebfuscatorConfig.NoObfuscationForMetadata
            		&& player.hasMetadata(OrebfuscatorConfig.NoObfuscationForMetadataTagName)
            		&& player.getMetadata(OrebfuscatorConfig.NoObfuscationForMetadataTagName).get(0).asBoolean();
        } catch (Exception e) {
            Orebfuscator.log("Error while obtaining metadata for player" + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return ret;
    }

    private static FileConfiguration getConfig() {
        return Orebfuscator.instance.getConfig();
    }

    private static int clamp(int value, int min, int max) {
        if (value < min)
            value = min;
        if (value > max)
            value = max;
        return value;
    }
}
