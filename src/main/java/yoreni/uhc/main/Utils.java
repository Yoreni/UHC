package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public class Utils
{
	final public static long ONE_MIN = 60000;
	final public static long ONE_HOUR = 3600000;

	/**
	 *
	 * @param player
	 * @param teams the list of teams in the game
	 * @return the team the player is in (null if it could not find the player in the teams list)
	 */
	public static UHCTeam getPlayersTeam(Player player, ArrayList<UHCTeam> teams) 
	{
		UUID uuid = player.getUniqueId();
		return getPlayersTeam(uuid, teams);
	}

	/**
	 *
	 * @param uuid the uuid of the player
	 * @param teams the list of teams in the game
	 * @return the team the player is in (null if it could not find the player in the teams list)
	 */
	public static UHCTeam getPlayersTeam(UUID uuid, ArrayList<UHCTeam> teams) 
	{	
		for (UHCTeam team : teams)
		{
			if (team.isInTeam(uuid))
			{
				return team;
			}
		}
		return null; //if the player isnt in a team null gets returned
	}
	
	public static int getPlayersTeamNumber(Player player, ArrayList<UHCTeam> teams) 
	{
		UUID uuid = player.getUniqueId();
		return getPlayersTeamNumber(uuid, teams);
	}

	/**
	 * gets the players team number if the player is in a team according to the teams list
	 *
	 * @param uuid the uuid of the player
	 * @param teams the list of teams in the game
	 * @return
	 */
	public static int getPlayersTeamNumber(UUID uuid, ArrayList<UHCTeam> teams) 
	{
		UHCTeam team = getPlayersTeam(uuid, teams);
		if(team == null)
		{
			return -1;
		}
		else
		{
			return team.getNumber();
		}
	}

	/**
	 * returns a random int between certian values
	 *
	 * @param min the lowest you want the int to be
	 * @param max the highest you want the int to be
	 * @return
	 */
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

	/**
	 * this gets a random safe* location in a world within the range
	 * *safe meaning that the player wont spawn on water or lava
	 *
	 * @param range max blocks away from 0 0
	 * @return
	 */
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

	/**
	 * formats time into human readable format (m:ss)
	 *
	 * @param x the time in milliseconds
	 * @return
	 */
	public static String shortTime(long x)
	{
		int time = (int) x / 1000;
		int m = (int) Math.floor(time / 60);
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return m + ":" + format.format(s);
	}

	/**
	 * formats time into human readable format (h:mm:ss)
	 *
	 * @param x the time in milliseconds
	 * @return
	 */
	public static String longTime(long x)
	{
		int time = (int) x / 1000;
		int h = (int) Math.floor(time / 3600);
		int m = (int) Math.floor(time / 60) % 60;
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return h + ":" + format.format(m) + ":" + format.format(s);
	}

	/**
	 * this gets the ordnial form of a number for example
	 * 3 -> 3rd, 17 -> 17th, 51 -> 51st
	 *
	 * @param x the number to be converted
	 * @return
	 */
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

	/**
	 * this gets the distance between the given location and the given world bored
	 *
	 * @param loc the location you want to check the distance from it as well as it can also get the world and then world border from this too
	 * @return the distance in blocks
	 */
	public static int getDistancefromBorder(Location loc)
	{
		/*
			we can get the world border instance from the given location
			so we are doing that
		 */
		WorldBorder border = loc.getWorld().getWorldBorder();

		double borderSize = border.getSize() / 2;
		double xPos = Math.abs(loc.getX());
		double zPos = Math.abs(loc.getZ());
		
		double closestToBorder = Math.max(xPos, zPos);
		
		return (int) (borderSize - closestToBorder);
	}

	/**
	 * this gets the direction you would need to go with the given location
	 * to get to the closest edge of the world border
	 *
	 * @param loc the location you want to check the distance from it as well as it can also get the world and then world border from this too
	 * @return a number in degrees thats 0<=x<360
	 */
	public static double getDirectionFromBorder(Location loc)
	{
		WorldBorder border = loc.getWorld().getWorldBorder();

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

		//we add 180 so its a number between 0 and 360
		return yaw + 180;
	}

	/**
	 * turns a direction in degrees into an arrow pointing N,NE,E,SE,S,SW,W or NW
	 *
	 * @param direction
	 * @return
	 */
	public static String directionToArrow(double direction)
	{		
		int dir = (int) ((direction / 45)) % 8;
		if(dir < 0)
		{
			//makes sures no index out of bounds error happnen
			dir += 8;
		}
		
		final String[] directionalArrows = {"↑","↖","←","↙","↓","↘","→","↗"};
		return directionalArrows[dir];
	}

	/**
	 *
	 * checks if a given loot table exists or not
	 *
	 * @param loottable namespace of a loot table
	 * @return
	 */
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

	/**
	 * sets up a world for an UHC enviroment (and creates it if it doesnt exist)
	 *
	 * @param worldName
	 * @param dimension
	 */
	public static void createAndSetupWorld(String worldName, World.Environment dimension)
	{
		boolean worldExists = Bukkit.getWorld(worldName) != null;
		World world = null;

		//lets create the world if it doesnt exist
		if(!worldExists)
		{
			WorldCreator creater = new WorldCreator(worldName);
			creater.type(WorldType.NORMAL);
			creater.environment(dimension);
			creater.hardcore(true);

			if(dimension  == World.Environment.NORMAL)
			{
				creater.generatorSettings("{\"structures\": {\"structures\": {\"village\": {\"salt\": 8015723, \"spacing\": 32, \"separation\": 8}}}, \"layers\": [{\"block\": \"stone\", \"height\": 1}, {\"block\": \"grass\", \"height\": 1}], \"biome\":\"plains\"");
			}

			Main.getInstance().debug(worldName + "s settings: " + creater.generatorSettings());

			world = Bukkit.createWorld(creater);
		}

		if(world == null)
		{
			world = Bukkit.getWorld(worldName);
		}

		world.setDifficulty(Difficulty.HARD);
		world.setGameRuleValue("naturalRegeneration", "false");
		world.setThundering(false);
		world.setPVP(false);
		WorldBorder border = world.getWorldBorder();
		border.setWarningDistance(100);
		border.setDamageBuffer(1);
		border.setCenter(0, 0);
	}

	/**
	 * makes the lobby for the players that are waitng for the game to start
	 *
	 * @param worldName the world you want the lobby to be in
	 */
	public static void createLobby(String worldName)
	{
		World world = Bukkit.getWorld(worldName);
		world.setPVP(false);

		// creating the lobby
		world.loadChunk(0, 0);
		world.loadChunk(0, -1);
		world.loadChunk(-1, 0);
		world.loadChunk(-1, -1);

		//building the lobby
		// floor
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 -11 11 200 11 minecraft:glass");
		// walls
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 11 200 -11 -11 205 -11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 -11 -11 205 11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 11 11 205 11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 11 200 11 11 205 -11 minecraft:glass");
		// celing
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 206 -11 11 206 11 minecraft:glass");

		world.unloadChunk(0, 0);
		world.unloadChunk(0, -1);
		world.unloadChunk(-1, 0);
		world.unloadChunk(-1, -1);
	}

	public static int clamp(int number, int min, int max)
	{
		if(number > max)
		{
			number = max;
		}
		else if(number < min)
		{
			number = min;
		}

		return number;
	}

	/**
	 * gets a list to display like it would in natrual langauge
	 * eg
	 * ["Bob","Tom","Jess"]
	 * becomes
	 * Bob, Tom and Jess
	 *
	 * @param list
	 * @return
	 */
	public static String prettyList(List list)
	{
		String out = "";

		for(int i = 0; i < list.size(); i++)
		{
			if(i == 0)
			{
				out += list.get(i).toString();
			}
			else if(i == list.size() - 1)
			{
				out += " and " + list.get(i).toString();
			}
			else
			{
				out += ", " + list.get(i).toString();
			}
		}

		return out;
	}
}
