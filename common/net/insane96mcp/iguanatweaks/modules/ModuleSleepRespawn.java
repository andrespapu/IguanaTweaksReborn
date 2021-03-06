package net.insane96mcp.iguanatweaks.modules;

import net.insane96mcp.iguanatweaks.IguanaTweaks;
import net.insane96mcp.iguanatweaks.lib.Properties;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;

public class ModuleSleepRespawn {

	public static void ProcessSpawn(EntityPlayer player) {
		if (!Properties.Global.sleepRespawn)
			return;
		
		if (Properties.SleepRespawn.spawnLocationRandomMax <= 0)
			return;
		
		NBTTagCompound tags = player.getEntityData();
		boolean hasAlreadySpawned = tags.getBoolean("IguanaTweaks:spawned");
        if (!hasAlreadySpawned)
        {
        	tags.setBoolean("IguanaTweaks:spawned", true);
			RespawnPlayer((EntityPlayerMP)player, Properties.SleepRespawn.spawnLocationRandomMin, Properties.SleepRespawn.spawnLocationRandomMax);
        }
	}
	
	public static void ProcessRespawn(EntityPlayer player) {
		if (!Properties.Global.sleepRespawn)
			return;
		
        RespawnPlayer((EntityPlayerMP)player, Properties.SleepRespawn.respawnLocationRandomMin, Properties.SleepRespawn.respawnLocationRandomMax);
        PlayerHealth((EntityPlayerMP)player);
        
        DestroyBed((EntityPlayerMP)player);
        
        if (Properties.SleepRespawn.respawnLocationRandomMax != 0)
        	player.sendMessage(new TextComponentString(I18n.format("sleep.random_respawn")));
	}
	
	private static void DestroyBed(EntityPlayerMP player) {
		if (!Properties.Global.sleepRespawn)
			return;
		
		if (!Properties.SleepRespawn.destroyBedOnRespawn)
			return;
		
		BlockPos bedPos = player.getBedLocation(player.dimension);
		
		if (bedPos == null)
			return;
		
		World world = player.getEntityWorld();
		
        if (!world.getBlockState(bedPos).getBlock().equals(Blocks.BED))
        	return;

        world.setBlockState(bedPos, Blocks.AIR.getDefaultState(), 3);
    	
		if (Properties.SleepRespawn.respawnLocationRandomMax == 0)
			player.sendMessage(new TextComponentString(I18n.format("sleep.bed_destroyed")));
	}

	private static void RespawnPlayer(EntityPlayerMP player, int minDistance, int maxDistance) {
		if (!Properties.Global.sleepRespawn)
			return;
		
		if (maxDistance <= 0)
			return;
		
		int x = (int)player.posX;
		if (x < 0) 
			--x;
		
		int z = (int)player.posZ;
		if (z < 0) 
			--z;
		
		World world = player.getEntityWorld();
		
		BlockPos newCoords = RandomiseCoordinates(world, x, z, minDistance, maxDistance);
		player.setLocationAndAngles(newCoords.getX() + .5f, newCoords.getY() + 1.1f, newCoords.getZ() + .5f, 0.0f, 0.0f);
		
        WorldServer worldserver = player.getServerWorld();
        worldserver.getChunkProvider().loadChunk((int)player.posX >> 4, (int)player.posZ >> 4);

        while (!worldserver.getCollisionBoxes(player, player.getEntityBoundingBox()).isEmpty())
        {
            player.setPosition(player.posX, player.posY + 1.0D, player.posZ);
        }

        player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
	}
	
	private static BlockPos RandomiseCoordinates(World world, int x, int z, int min, int max) {
		BlockPos newBlockPos = new BlockPos(x, 64, z);
	
		for (int attempt = 0; attempt < 50; attempt++)
		{
			int rngX, rngY, rngZ;
			
			rngX = MathHelper.getInt(world.rand, min, max);  
			if (world.rand.nextBoolean()) 
				rngX *= -1;
			newBlockPos = newBlockPos.add(rngX, 0, 0);

			rngZ = MathHelper.getInt(world.rand, min, max);  
			if (world.rand.nextBoolean()) 
				rngZ *= -1;
			newBlockPos = newBlockPos.add(0, 0, rngZ);
			
    		ResourceLocation actualBiome = world.getBiome(newBlockPos).getRegistryName();
    		
    		if (actualBiome.equals(Biomes.DEEP_OCEAN.getRegistryName()) 
        		|| actualBiome.equals(Biomes.OCEAN.getRegistryName())
        		|| actualBiome.equals(Biomes.RIVER.getRegistryName()))
    			continue;
			
			newBlockPos = world.getTopSolidOrLiquidBlock(newBlockPos);
			
			if (newBlockPos.getY() >= 0) 
			{
				IguanaTweaks.logger.info("Good spawn found at " + newBlockPos);
				break;
			}
		}

		return newBlockPos;
	}

	private static void PlayerHealth(EntityPlayerMP player) {
		if (!Properties.Global.sleepRespawn)
			return;
		
		int respawnHealth = Properties.SleepRespawn.respawnHealth;
		EnumDifficulty difficulty = player.getEntityWorld().getDifficulty();
		   
		if (Properties.SleepRespawn.respawnHealthDifficultyScaling) {
			if (difficulty == EnumDifficulty.HARD) 
			{
				respawnHealth = (int) Math.max(respawnHealth / 2f, 1);
			}
			else if (difficulty.getDifficultyId() <= EnumDifficulty.EASY.getDifficultyId()) 
			{
				respawnHealth = (int) Math.min(respawnHealth * 2f, 20);
			}
		}

		player.setHealth(respawnHealth);
	}

	public static void DisabledSpawnPoint(PlayerSleepInBedEvent event) {
		if (!Properties.Global.sleepRespawn)
			return;
		
		if (!Properties.SleepRespawn.disableSleeping) 
			return;
		
		EntityPlayer player = event.getEntityPlayer();
		
		if (player.world.isDaytime())
			return;
		
		event.setResult(SleepResult.OTHER_PROBLEM);
		
		if (Properties.SleepRespawn.disableSetRespawnPoint) {
			player.sendStatusMessage(new TextComponentString(I18n.format("sleep.bed_decoration")), true);
		}
		else {
			player.setSpawnChunk(event.getPos(), false, player.dimension);
			player.sendStatusMessage(new TextComponentString(I18n.format("sleep.enjoy_the_night")), true);
		}
	}
}
