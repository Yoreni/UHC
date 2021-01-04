package yoreni.uhc.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.advancement.Advancement;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class UHC
{
	//this is what the uhc world will be called
	final String UHC_WORLD_NAME = "uhc";
	
	//this is the max amount of time a player can disconnect from a game without being disqualified.
	final int CONNECTION_TIMEOUT = 60000;
	
	//this is how many player there are in a team
	static int teamSize = 0;
	
	GameStatus status = GameStatus.OFF; // this is just the stage of the game is in.
	Main main = null;
	
	// Time varibles
	private long start = 0;
	//
	int startBorderSize = 0;
	//
	ArrayList<UHCTeam> teams = new ArrayList<UHCTeam>();
	int teamsAlive = 0;
	List<UUID> playersAlive = new ArrayList<UUID>();
	HashMap<UUID, Long> disconnectedPlayers = new HashMap<UUID, Long>();
	
	public UHC(Main main)
	{
		this.main = main;
	}
	
	public long getStartTimestamp()
	{
		return start;
	}
	
	public long getGameTime()
	{
		if(status == GameStatus.PLAYING)
		{
			return System.currentTimeMillis() - start;
		}
		else
		{
			return 0;
		}
	}
	
	public void setBorderShrinkSpeed(double blocksPerSecond)
	{
		if(blocksPerSecond == 0)
		{
			//seeing that 0 means the border isnt shrinking
			//at all we will just set it to its current size
			World world = Bukkit.getWorld(UHC_WORLD_NAME);
			WorldBorder border = world.getWorldBorder();
			border.setSize(world.getWorldBorder().getSize());

			world = Bukkit.getWorld(UHC_WORLD_NAME + "_nether");
			border = world.getWorldBorder();
			border.setSize(world.getWorldBorder().getSize());

			world = Bukkit.getWorld(UHC_WORLD_NAME + "_the_end");
			border = world.getWorldBorder();
			border.setSize(world.getWorldBorder().getSize());
		}
		else
		{
			World world = Bukkit.getWorld(UHC_WORLD_NAME);
			WorldBorder border = world.getWorldBorder();
			double blocksToGo = (world.getWorldBorder().getSize() - 32) / 2;
			border.setSize(32, (long) (blocksToGo / blocksPerSecond)); 
			
			//8 blocks in the nether = 1 block in the overworld
			world = Bukkit.getWorld(UHC_WORLD_NAME + "_nether");
			border = world.getWorldBorder();
			blocksToGo = (world.getWorldBorder().getSize() - 32) / 2;
			border.setSize(32, (long) (blocksToGo / blocksPerSecond)); 
			
			world = Bukkit.getWorld(UHC_WORLD_NAME + "_the_end");
			border = world.getWorldBorder();
			blocksToGo = (world.getWorldBorder().getSize() - 32) / 2;
			border.setSize(32,  (long) (blocksToGo / blocksPerSecond));
		}
	}
	
	//enables/disables pvp
	public void setPVP(boolean value)
	{
		World world = Bukkit.getWorld(UHC_WORLD_NAME);
		world.setPVP(value);
		world = Bukkit.getWorld(UHC_WORLD_NAME + "_nether");
		world.setPVP(value);
		world = Bukkit.getWorld(UHC_WORLD_NAME + "_the_end");
		world.setPVP(value);
	}
	
	public void createAndSetupWorld(String worldName, Environment dimension)
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
			
			if(dimension  == Environment.NORMAL)
			{
				creater.generatorSettings("{\"structures\": {\"structures\": {\"village\": {\"salt\": 8015723, \"spacing\": 32, \"separation\": 8}}}, \"layers\": [{\"block\": \"stone\", \"height\": 1}, {\"block\": \"grass\", \"height\": 1}], \"biome\":\"plains\"");
			}
			
			main.debug(worldName + "s settings: " + creater.generatorSettings());
			
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
	
	public void startGame() 
	{
		int playersPlaying = Bukkit.getOnlinePlayers().size();	
		startBorderSize = (playersPlaying * 40) + 2000;
		
		World world = Bukkit.getWorld(UHC_WORLD_NAME);
		world.setFullTime(0);
		WorldBorder border = world.getWorldBorder();
		border.setSize(startBorderSize);

		world = Bukkit.getWorld(UHC_WORLD_NAME + "_nether");
		border = world.getWorldBorder();
		border.setSize(startBorderSize);

		world = Bukkit.getWorld(UHC_WORLD_NAME + "_the_end");
		border = world.getWorldBorder();
		border.setSize(startBorderSize);

		start = System.currentTimeMillis();

		// seting up the teams

		//this calculates how many teams there should be at inits them
		//we still do this for solo games just so its eaier to manage the players
		int noTeams = (int) Math.ceil(playersPlaying / (double) teamSize);
		 
		for (int x = 1; x <= noTeams; x++) 
		{
			teams.add(new UHCTeam(x));
		}
			
		//convert a collection into a list
		List<Player> playing = new ArrayList<Player>();
		for(Player player : Bukkit.getOnlinePlayers())
		{
			playing.add(player);
		}
		
		Collections.shuffle(playing);
		int team = 0;

		for (Player play : playing) 
		{		
			int chosenTeam = team % noTeams;
			// asigns the player to a team
			teams.get(chosenTeam).addPlayer(play);
			team++;

			//this adds a prefix to thier name so people know whos in what team
			//we dont bother with this if its a solo game
			if(teamSize >= 2)
			{
				if(noTeams <= 16) 
				{ 
					play.setDisplayName(main.TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET); 
					play.setCustomName(main.TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET); 
				} 
				else 
				{
					play.setDisplayName(ChatColor.GRAY + ((chosenTeam + 1) + " ") + ChatColor.WHITE + play.getName() + ChatColor.RESET); 
					play.setCustomName(ChatColor.GRAY + ((chosenTeam + 1) + " ") + ChatColor.WHITE + play.getName() + ChatColor.RESET);
				}
				play.setCustomNameVisible(true); 
			}
			else
			{
				play.setDisplayName(ChatColor.RESET + play.getName());
				play.setCustomNameVisible(false);
			}
		}

		teamsAlive = noTeams;
		world = Bukkit.getWorld(UHC_WORLD_NAME);
		
		//to remove everyones advancements
		//janky i know but i couldnt find another way. theres litterly no way to remove advancements in the spigot API
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke @a everything");

		for (UHCTeam uhcteam : teams) 
		{
			Location loc = Utils.rtp(startBorderSize / 2);
			loc.setY(loc.getY() + 1); //adds 2 just so the play doesnt sufficate in any blocks on intainal spawn

			for(Player play : uhcteam.getPlayers())
			{
				//if(play == null)
				//{
				//	continue;
				//}
				
				// adds to player to players alive
				playersAlive.add(play.getUniqueId());
				// resets the plays stats
				play.setGameMode(GameMode.SURVIVAL);
				play.setHealth(play.getMaxHealth());
				play.setFoodLevel(20);
				//we set this to 0 so the phantoms scew players equaly when we allow them to spawn
				play.setStatistic(Statistic.TIME_SINCE_REST, 0);
				play.setSaturation(0);
				play.setExp(0);
				play.setLevel(0);
				play.getInventory().clear();
				
				// random teleport the player somewhere in the world
				play.teleport(loc);
			}
		}

		BukkitTask graceTimer = new Timer(main, this, "Grace Period ends in %s"
				, BarColor.GREEN, main.config.getInt("gracePeriodEndsAfter")
				, TimerType.GRACE_END)
				.runTaskTimer(main, 1L, 1L);
		
		BukkitTask borderTimer = new Timer(main, this, "Border starts to shrink in %s"
				, BarColor.GREEN, main.config.getInt("borderShrinksAfter")
				, TimerType.BORDER_SHRINK_START)
				.runTaskTimer(main, 1L, 1L);
		
		status = GameStatus.PLAYING;
	}
	
	public void setupGame() 
	{
		main.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable() {
			public void run()
			{
				createAndSetupWorld(UHC_WORLD_NAME, Environment.NORMAL);
				createAndSetupWorld(UHC_WORLD_NAME + "_nether", Environment.NETHER);
				createAndSetupWorld(UHC_WORLD_NAME + "_the_end", Environment.THE_END);
			}
		}, 0L);
		
		World world = Bukkit.getWorld("world");
		world.setPVP(false);
		
		// creating the lobby
		world.loadChunk(0, 0);
		world.loadChunk(0, -1);
		world.loadChunk(-1, 0);
		world.loadChunk(-1, -1);

		// floor
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 -11 11 200 11 minecraft:glass");
		// walls
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 11 200 -11 -11 205 -11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 -11 -11 205 11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 200 11 11 205 11 minecraft:glass");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill 11 200 11 11 205 -11 minecraft:glass");
		// celing
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill -11 206 -11 11 206 11 minecraft:glass");

		Location loc = new Location(world, 0, 201, 0);

		for (Player play : Bukkit.getOnlinePlayers()) 
		{
			play.getInventory().clear();
			//play.getInventory().setItem(0, nr);
			//play.getInventory().setItem(1, playerView);
			play.setGameMode(GameMode.ADVENTURE); // sets the gaming to adventure so players can break any blocks
			play.teleport(loc); // teleports the player in to the lobby
		}

		world.unloadChunk(0, 0);
		world.unloadChunk(0, -1);
		world.unloadChunk(-1, 0);
		world.unloadChunk(-1, -1);

		status = GameStatus.WAITING;
	}
	
	//TODO for some reason this method doesnt work probley
	public void updateAliveStatus(UHCTeam team)
	{
		int count = 0;

		//an alive team is wether if a team member is still in survival mode
		for(UUID uuid : team.getUUIDs())
		{
			if(playersAlive.contains(uuid))
			{
				count++;
			}
		}
		//if we are going to mark the team dead then we knock one of from the teams alive counter
		if(team.isAlive() && count == 0)
		{
			teamsAlive--;
		}
		
		if(count == 0)
		{
			team.markDead();
		}
	}
	
	public void setTeamSizeFromConfig()
	{
		teamSize = main.config.getInt("teamSize");
	}
}
