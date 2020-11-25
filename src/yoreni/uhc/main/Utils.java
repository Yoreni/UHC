package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public class Utils
{
	final static long ONE_MIN = 60000;
	final static long ONE_HOUR = 3600000;
	
	public static int getTeam(Player player, ArrayList<UHCTeam> teams) 
	{
		return getTeam(player.getUniqueId(), teams);
	}
	
	public static int getTeam(UUID uuid, ArrayList<UHCTeam> teams) 
	{
		for (UHCTeam team : teams)
		{
			if (team.isInTeam(uuid))
			{
				return team.getNumber();
			}
		}
		return -1; //if the player isnt in a team -1 gets returned
	}
	
	public static int randint(int min, int max) 
	{
		double output = 0;
		double random = Math.random();
		output = random * (max - min);
		output = output + min;

		if (Math.random() >= 0.5) 
		{
			return (int) Math.floor(output);
		} 
		else 
		{
			return (int) Math.ceil(output);
		}
	}
	
	public static Location rtp(int range)
	{
		World world = Bukkit.getWorld("uhc");
		
		double x = 0;
		double y = 0;
		double z = 0;
		
		do
		{
			x = randint(-range, range);
			z = randint(-range, range);
			y = world.getHighestBlockYAt((int) x, (int) z) + 1;
		}
		while(world.getBlockAt((int) x, (int) y - 1, (int) z).isLiquid());
		
		return new Location(world, x, y, z);
	}
	
	//formats time into human readable format (m:ss)
	public static String shortTime(long x)
	{
		int time = (int) x / 1000;
		int m = (int) Math.floor(time / 60);
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return m + ":" + format.format(s);
	}
	
	//formats time into human readable format (h:mm:ss)
	public static String longTime(long x)
	{
		int time = (int) x / 1000;
		int h = (int) Math.floor(time / 3600);
		int m = (int) Math.floor(time / 60) % 60;
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return h + ":" + format.format(m) + ":" + format.format(s);
	}
	
	//this gets the ordnial form of a number for example
	// 3 -> 3rd, 17 -> 17th, 51 -> 51st
	public static String ordinalNumber(int x)
	{
		if(x < 0)
		{
			return ordinalNumber(Math.abs(x));
		}
		
		if(x % 100 >= 10 && x % 100 <= 19)
		{
			return x + "th";
		}
		else if(x % 10 == 1)
		{
			return x + "st";
		}
		else if(x % 10 == 2)
		{
			return x + "nd";
		}
		else if(x % 10 == 3)
		{
			return x + "rd";
		}
		else
		{
			return x + "th";
		}
	}
	
	public static int getDistancefromBorder(Location loc, WorldBorder border)
	{
		double borderSize = border.getSize() / 2;
		double xPos = Math.abs(loc.getX());
		double zPos = Math.abs(loc.getZ());
		
		double closestToBorder = Math.max(xPos, zPos);
		
		return (int) (borderSize - closestToBorder);
	}
	
	public static double getDirectionFromBorder(Location loc, WorldBorder border)
	{
		double yaw = loc.getYaw() + 22.5;
		double x = loc.getX();
		double z = loc.getZ();
		
		if(x > z)
		{
			if(x > -z) //east section
			{
				yaw -= 90;
			}
			else  //north section
			{
				yaw -= 0;
			}
		}
		else
		{
			if(x > -z) //west section
			{
				yaw += 180;
			}
			else //south section
			{
				yaw += 90;
			}
		}
		
		//Bukkit.getLogger().info(yaw + "");
		//we add 180 so its a number between 0 and 360
		return yaw + 180;
	}
	
	public static String directionToArrow(double direction)
	{		
		int dir = (int) ((direction / 45)) % 8;
		if(dir < 0)
		{
			//makes sures no index out of bounds error happnen
			dir += 8;
		}
		
		String[] arrows = {"↑","↖","←","↙","↓","↘","→","↗"};
		return arrows[dir];
	}
	
	public static boolean isValidLootTable(NamespacedKey loottable)
	{
		boolean output = false;
		try
		{
			//we are going to fill a sample inventory just to see if the loot table in the config actualy exists
			LootTable loot = Bukkit.getLootTable(loottable);
			LootContext context = new LootContext.Builder(
					new Location(Bukkit.getWorlds().get(0),0 ,0 ,0)).build();
			Inventory testInv = Bukkit.createInventory(null, 27);
			
			loot.fillInventory(testInv, new Random(), context);
			Bukkit.getLogger().info("a valid loot table has been found");
			output = true;
		}
		catch(Exception exception)
		{
			output = false;
		}
		
		return output;
	}
}
