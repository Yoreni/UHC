package yoreni.uhc.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;

public class UHC
{
	/**
		this is what the uhc world will be called
	 */
	public static final String UHC_WORLD_NAME = "uhc";
	
	/**
		this is the max amount of time a player can disconnect from a game without being disqualified.
	 */
	public static final int CONNECTION_TIMEOUT = 60000;

	/**
	 * 	this is how many players there can be in a team
	 */
	private int teamSize;

	/**
	 *  this is just the stage or phase the game is in
	 */
	GameStatus status = GameStatus.OFF;
	
	/*
		the timestamp of which the game started
	 */
	private long start = 0;

	int startBorderSize = 0;

	/**
	 *  a list of all teams in the game
	 */
	ArrayList<UHCTeam> teams = new ArrayList<UHCTeam>();

	int teamsAlive = 0;

	/**
	 *  a list of the players that are still in the game
	 */
	List<UUID> playersAlive = new ArrayList<UUID>();

	/**
	 *  a hashmap of players that have disconnected from the server along with the timestamp of when
	 *  they disconnected
	 */
	HashMap<UUID, Long> disconnectedPlayers = new HashMap<UUID, Long>();

	public UHC()
	{

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

	public int getTeamSize()
	{
		return teamSize;
	}

	public int getTeamsAlive()
	{
		return teamsAlive;
	}

	/**
	 * changes the speed of the rate the border shrinks
	 *
	 * @param blocksPerSecond
	 */
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
					play.setDisplayName(UHCTeam.TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET);
					play.setCustomName(UHCTeam.TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET);
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
				play.setHealth(play.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
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

		BukkitTask graceTimer = new Timer(Main.getInstance(), this, "Grace Period ends in %s"
				, BarColor.GREEN, Main.getInstance().config.getInt("gracePeriodEndsAfter")
				, TimerType.GRACE_END)
				.runTaskTimer(Main.getInstance(), 1L, 1L);
		
		BukkitTask borderTimer = new Timer(Main.getInstance(), this, "Border starts to shrink in %s"
				, BarColor.GREEN, Main.getInstance().config.getInt("borderShrinksAfter")
				, TimerType.BORDER_SHRINK_START)
				.runTaskTimer(Main.getInstance(), 1L, 1L);
		
		status = GameStatus.PLAYING;
	}
	
	public void setupGame() 
	{
		Main.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
			public void run()
			{
				Utils.createAndSetupWorld(UHC_WORLD_NAME, Environment.NORMAL);
				Utils.createAndSetupWorld(UHC_WORLD_NAME + "_nether", Environment.NETHER);
				Utils.createAndSetupWorld(UHC_WORLD_NAME + "_the_end", Environment.THE_END);
			}
		}, 0L);

		Utils.createLobby("world");
		Location loc = new Location(Bukkit.getWorld("world"), 0, 201, 0);
		for (Player play : Bukkit.getOnlinePlayers())
		{
			play.getInventory().clear();
			play.setGameMode(GameMode.ADVENTURE); // sets the gaming to adventure so players can break any blocks
			play.teleport(loc);
		}

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
		teamSize = Main.getInstance().config.getInt("teamSize");
	}

	public GameStatus getStatus()
	{
		return status;
	}

	public List<UUID> getAlivePlayers()
	{
		return playersAlive;
	}

	public ArrayList<UHCTeam> getTeams()
	{
		return teams;
	}

	public HashMap<UUID, Long> getDisconnectedPlayers()
	{
		return disconnectedPlayers;
	}

	/**
	 *  eliminates a player from a game
	 *  players get eliminated from dying
	 */
	public void eliminate(Player player)
	{
		player.setGameMode(GameMode.SPECTATOR);

		player.sendTitle(String.format("You came %s",
				Utils.ordinalNumber(Main.getInstance().getGame().playersAlive.size()))
				, "",5, 20, 5);

		// marking the player as dead on other fileds of the class as well
		Main.getInstance().getGame().playersAlive.remove(player.getUniqueId());
		UHCTeam defTeam = Utils.getPlayersTeam(player, Main.getInstance().getGame().teams);
		Main.getInstance().getGame().updateAliveStatus(defTeam);

		//code just here for debug just so i know whos being taking off when a player dies
		ArrayList<String> debug = new ArrayList<String>();
		for(UUID uuid : playersAlive)
		{
			debug.add(Bukkit.getPlayer(uuid).getName());
		}
		Main.getInstance().debug(debug.toString());
	}

	public void removeFromDisconnectedPlayers(Player player)
	{
		disconnectedPlayers.remove(player.getUniqueId());
	}

	public void addToDisconnectedPlayers(Player player)
	{
		disconnectedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
	}

	public void disquify(Player player)
	{

	}
}
