package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Main extends JavaPlugin implements Listener 
{
	//game settings
	int borderShrinkDelay = 0;
	int gracePeriod = 0;	
	final int CONNECTION_TIMEOUT = 60000; //this is the max amount of time a player can disconnect from a game without being disqualified. 
	static int teamSize = 1;
	// Time varibles
	long grace = 0;
	long shrink = 0;
	long countdown = 6000;
	static long start = 0;
	// Player varibles
	final ChatColor[] TEAM_COLOURS = { ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.GOLD,
			ChatColor.LIGHT_PURPLE, ChatColor.AQUA, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
			ChatColor.DARK_RED, ChatColor.DARK_AQUA, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.WHITE,
			ChatColor.BLACK };

	ArrayList<String> ready = new ArrayList<String>();

	GameStatus status = GameStatus.OFF; // this is just the stage of the game is in.
	double borderSize = 0;

	static List<UUID> playersAlive = new ArrayList<UUID>();
	HashMap<UUID, Long> disconnectedPlayers = new HashMap<UUID, Long>();
	ArrayList<UHCTeam> teams = new ArrayList<UHCTeam>();
	static int teamsAlive = 0;

	// Item varibles
	ItemStack nr = null;
	ItemStack r = null;
	ItemStack playerView = null;

	BossBar borderTimer = Bukkit.createBossBar("a", BarColor.GREEN, BarStyle.SOLID);
	BossBar graceTimer = Bukkit.createBossBar("a", BarColor.GREEN, BarStyle.SOLID);

	//config files
	ConfigFile config;

	public String shortTime(long x) {
		int time = (int) x / 1000;
		int m = (int) Math.floor(time / 60);
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return m + ":" + format.format(s);
	}

	public void onEnable() 
	{
		config = new ConfigFile(this);
		config.setup("config");

		if(!config.getBoolean("enabled"))
		{
			Bukkit.getLogger().info("Plugin has been disabled cos it says so in the plugin.yml to change it change the boolean value in the plugin.yml");
			Bukkit.getPluginManager().disablePlugin(this);
		}

		borderShrinkDelay = config.getInt("borderShrinksAfter");
		gracePeriod = config.getInt("gracePeriodEndsAfter");
		teamSize = config.getInt("teamSize");

		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new Events(this), this);
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) 
		{
			new PlaceHolders(this, "uhc");// .hook();
		}
		/*
		 * //creates the not ready item ItemEditer a = new ItemEditer();
		 * a.setID(Material.RED_WOOL); a.setName("&cNot Ready &7(Right click)"); nr =
		 * a.get(); //creates the ready item ItemEditer b = new ItemEditer();
		 * b.setID(Material.LIME_WOOL); b.setName("&aReady &7(Right click)"); r =
		 * b.get(); //creates the item which lets players to view whos playing
		 * ItemEditer c = new ItemEditer(); c.setID(Material.COMPASS);
		 * c.setName("&eView Players &7(Right click)"); playerView = c.get(); int id =
		 * Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new
		 * Runnable() { public void run() { if(status == GameStatus.WAITING) {
		 * if(ready.size() == Bukkit.getOnlinePlayers().size()) { if(countdown <
		 * System.currentTimeMillis()) { //this starts the game
		 * 
		 * } } else { countdown = System.currentTimeMillis() + 6000; } }
		 * 
		 * BOSS BARS
		 */
		// world border
		int id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() 
		{
			public void run() 
			{
				if (status == GameStatus.PLAYING) 
				{
					//timeout players if theyve dissconected for too long
					for(UUID uuid : disconnectedPlayers.keySet())
					{
						int index = getPlayerAliveIndex(uuid.toString());

						boolean stillAlive = index != -1;
						if(System.currentTimeMillis() - disconnectedPlayers.get(uuid) > CONNECTION_TIMEOUT && stillAlive)
						{
							UHCTeam team = teams.get(getTeam(uuid) - 1);

							playersAlive.remove(index);
							team.updateAliveStatus();
							disconnectedPlayers.remove(uuid);
						}
					}
					long time = System.currentTimeMillis() - shrink;
					if (time < 0 && time > -50) 
					{
						borderTimer.removeAll();
						Bukkit.broadcastMessage("The Border has started to shrink");

						World world = Bukkit.getWorld("world");
						WorldBorder border = world.getWorldBorder();
						border.setSize(32, (long) ((borderSize / 2) * 1.2) + 3600); // this shrinks the world border

						world = Bukkit.getWorld("world_nether");
						border = world.getWorldBorder();
						border.setSize(4, (long) ((borderSize / 2) * 1.2) + 3600);

						world = Bukkit.getWorld("world_the_end");
						border = world.getWorldBorder();
						border.setSize(4, (long) ((borderSize / 2) * 1.2) + 3600);
					} 
					else if (time < 0) 
					{
						borderTimer.setTitle("Border starts to shrink in " + shortTime(time * -1));
						borderTimer.setProgress((double) (time * -1) / borderShrinkDelay);
					}
					// grace period bar
					time = System.currentTimeMillis() - grace;
					if (time < 0 && time > -50) 
					{
						graceTimer.removeAll();
						World world = Bukkit.getWorld("world");
						world.setPVP(true);

						world = Bukkit.getWorld("world_nether");
						world.setPVP(true);

						world = Bukkit.getWorld("world_the_end");
						world.setPVP(true);

						Bukkit.broadcastMessage("Grace period has ended");
					} 
					else if (time < 0) 
					{
						graceTimer.setTitle("Grace period ends in " + shortTime(time * -1));
						graceTimer.setProgress((double) (time * -1) / gracePeriod);
					}
					//sends an action bar to every play tell info about the game
					for(Player player : Bukkit.getOnlinePlayers())
					{
						World world = player.getWorld();
						String message;
						if(teamSize == 1)
							message = ChatColor.GREEN + "Border: ±" + ((int) world.getWorldBorder().getSize() / 2) + " Players alive: " + teamsAlive;
						else
							message = ChatColor.GREEN + "Border: ±" + ((int) world.getWorldBorder().getSize() / 2) + " Teams alive: " + teamsAlive + " Players alive: " + playersAlive.size();
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
					}
					//checks if anyone won the game
					if(teamsAlive == 1 && false)
					{
						//stops the shrinking border
						World world = Bukkit.getWorld("world");
						WorldBorder border = world.getWorldBorder();
						border.setSize(world.getWorldBorder().getSize());

						world = Bukkit.getWorld("world_nether");
						border = world.getWorldBorder();
						border.setSize(world.getWorldBorder().getSize());

						world = Bukkit.getWorld("world_the_end");
						border = world.getWorldBorder();
						border.setSize(world.getWorldBorder().getSize());

						UHCTeam team = null;
						for(UHCTeam currTeam : teams)
						{
							if(currTeam.isAlive())
							{
								team = currTeam;
								break;
							}
						}
						//removes the [] in the string and anounces the winner
						String teamMembers = team.toString().substring(1, team.toString().length() - 1);
						if(teamSize == 1)
							Bukkit.broadcastMessage(String.format("%s has won this UHC Game!", teamMembers));
						else
						{
							Bukkit.broadcastMessage(String.format("%s have won this UHC Game!", teamMembers));
						}

						status = GameStatus.OFF;
					}
				}
			}
		},20L, 1L);
	}



	public void onDisable() 
	{

	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{
		//Player player = (Player) sender;
		if (label.equalsIgnoreCase("uhc")) 
		{
			if(args.length > 0)
			{
				if(args[0].equals("reload"))
				{
					if(!sender.hasPermission("uhc.reload"))
					{
						sender.sendMessage("No perms!");
						return false;
					}

					if(status == GameStatus.PLAYING)
					{
						sender.sendMessage("Config cant be reloaded during a game!");
						return false;
					}
					else
					{
						config.reload();

						borderShrinkDelay = config.getInt("borderShrinksAfter");
						gracePeriod = config.getInt("gracePeriodEndsAfter");
						teamSize = config.getInt("teamSize");

						sender.sendMessage("Config reloaded");
						return true;
					}
				}
			}
			
			if(!sender.hasPermission("uhc.start"))
			{
				sender.sendMessage("No perms!");
				return false;
			}
			// starts the options
			/*
			 * Inventory inv = Bukkit.createInventory(null,27,"UHC options");
			 * 
			 * ItemEditer one = new ItemEditer(); one.setID(Material.PLAYER_HEAD);
			 * one.setName(ChatColor.WHITE + "UHC Solo");
			 * 
			 * ItemEditer two = new ItemEditer(); two.setID(Material.PLAYER_HEAD);
			 * two.setName(ChatColor.WHITE + "UHC Pairs");
			 * 
			 * ItemEditer three = new ItemEditer(); three.setID(Material.PLAYER_HEAD);
			 * three.setName(ChatColor.WHITE + "UHC Teams of 3s");
			 * 
			 * ItemEditer four = new ItemEditer(); four.setID(Material.PLAYER_HEAD);
			 * four.setName(ChatColor.WHITE + "UHC Teams of 4s");
			 * 
			 * inv.setItem(10,one.get()); inv.setItem(12,two.get());
			 * inv.setItem(14,three.get()); inv.setItem(16,four.get());
			 * 
			 * player.openInventory(inv);
			 */

			if (status == GameStatus.OFF)
				setupGame();
			else if (status == GameStatus.WAITING)
				startGame();

			return true;
		}
		return false;
	}

	public int randint(int min, int max) 
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
	
	public List<Player> shuffle(List<Player> input)
	{
		List<Player> output = new ArrayList<Player>();
		//goes through every elemet of input and places it in a random position of output
		for(Player player : input)
		{
			output.add(randint(0, output.size()),player);
		}
		return output;
	}

	public void startGame() 
	{
		borderSize = (Bukkit.getOnlinePlayers().size() * 40) + 2000;
		World world = Bukkit.getWorld("world");
		world.setDifficulty(Difficulty.HARD);
		world.setPVP(false);
		world.setGameRuleValue("naturalRegeneration", "false");
		world.setFullTime(0);
		world.setThundering(false);
		WorldBorder border = world.getWorldBorder();
		border.setSize(borderSize); // world border scales to how many players there are
		border.setCenter(0, 0);

		world = Bukkit.getWorld("world_nether");
		world.setDifficulty(Difficulty.HARD);
		world.setPVP(false);
		world.setGameRuleValue("naturalRegeneration", "false");
		border = world.getWorldBorder();
		border.setSize(borderSize / 8);
		border.setCenter(0, 0);

		world = Bukkit.getWorld("world_the_end");
		world.setDifficulty(Difficulty.HARD);
		world.setPVP(false);
		world.setGameRuleValue("naturalRegeneration", "false");
		border = world.getWorldBorder();
		border.setSize(borderSize);
		border.setCenter(0, 0);

		start = System.currentTimeMillis();
		// sets the countdowns
		shrink = System.currentTimeMillis() + borderShrinkDelay;
		grace = System.currentTimeMillis() + gracePeriod;

		world = Bukkit.getWorld("world");

		// seting up the teams

		//this calculates how many teams there should be at inits them
		//we still do this for solo games just so its eaier to manage the players
		int noTeams = (int) Math.ceil(Bukkit.getOnlinePlayers().size() / (double) teamSize);;

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
			Bukkit.broadcastMessage(chosenTeam + "");
			// asigns the player to a team
			teams.get(chosenTeam).addPlayer(play);
			team++;

			//this adds a prefix to thier name so people know whos in what team
			//we dont bother with this if its a solo game
			if(teamSize >= 2)
			{
				if(noTeams <= 16) 
				{ 
					play.setDisplayName(TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET); 
					play.setCustomName(TEAM_COLOURS[chosenTeam] + play.getName() + ChatColor.RESET); 
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

		for (UHCTeam uhcteam : teams) 
		{
			int a = 0;
			double x = (borderSize * Math.random()) - (borderSize / 2);
			double z = (borderSize * Math.random()) - (borderSize / 2);
			double y = world.getHighestBlockYAt((int) x, (int) z) + 3; //adds 3 just so the play doesnt sufficate in any blocks on intainal spawn
			// make sure its a decent spawn
			//TODO rtp but make more efficent or atleast doesnt pause the server tbh im commenting this out
			/*while (true) 
			{
				a++;
				x = (borderSize * Math.random()) - (borderSize / 2);
				z = (borderSize * Math.random()) - (borderSize / 2);
				y = world.getHighestBlockYAt((int) x, (int) z);
				if (!world.getBlockAt((int) x, (int) y - 1, (int) z).isLiquid())
					break;
			}*/
			Location rtp = new Location(world, x, y, z);

			for(Player play : uhcteam.getPlayers())
			{
				Bukkit.broadcastMessage("a");
				// resets the plays stats
				borderTimer.addPlayer(play);
				graceTimer.addPlayer(play);
				play.setGameMode(GameMode.SURVIVAL); // sets the gamemode to survival and sets the bars to the same as if
				playersAlive.add(play.getUniqueId());
				// you spawned into a new world
				play.setHealth(play.getMaxHealth());
				play.setFoodLevel(20);
				play.setSaturation(20);
				play.getInventory().clear();
				// random teleport the player somewhere in the world
				play.teleport(rtp);
			}
		}

		status = GameStatus.PLAYING;
	}

	public void setupGame() 
	{

		World world = Bukkit.getWorld("world");
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
			play.getInventory().setItem(0, nr);
			play.getInventory().setItem(1, playerView);
			play.setGameMode(GameMode.ADVENTURE); // sets the gaming to adventure so players can break any blocks
			play.teleport(loc); // teleports the player in to the lobby
		}

		world.unloadChunk(0, 0);
		world.unloadChunk(0, -1);
		world.unloadChunk(-1, 0);
		world.unloadChunk(-1, -1);

		status = GameStatus.WAITING;
	}

	public int getPlayerAliveIndex(String uuid)
	{
		int out = 0;
		for(UUID otherUuid : playersAlive)
		{
			if(otherUuid.toString().equals(uuid))
			{
				return out;
			}
			out++;
		}

		return -1;
	}

	public int getTeam(Player player) 
	{
		return getTeam(player.getUniqueId());
	}

	public int getTeam(UUID uuid) 
	{
		for(UHCTeam team : teams)
		{
			if(team.isInTeam(uuid))
			{
				return team.getNumber();
			}
		}
		return -1; //if the player isnt in a team -1 gets returned
	}
}