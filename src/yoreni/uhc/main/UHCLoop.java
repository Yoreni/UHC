package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class UHCLoop extends BukkitRunnable
{
	private JavaPlugin plugin;
	private UHC game;
	private long eventCooldown = 0;
	private EventType nextEvent;
	private ConfigFile settings;
	
	public UHCLoop(JavaPlugin plugin,UHC game,ConfigFile settings)
	{
		this.plugin = plugin;
		this.game = game;
		this.settings = settings;
	}
	
	@Override
	public void run()
	{
		if (game.status == GameStatus.PLAYING) 
		{
			//timeout players if theyve dissconected for too long
			for(UUID uuid : game.disconnectedPlayers.keySet())
			{
				int index = game.playersAlive.indexOf(uuid);
				boolean stillAlive = index != -1;
				if(System.currentTimeMillis() - game.disconnectedPlayers.get(uuid) > game.CONNECTION_TIMEOUT && stillAlive)
				{
					UHCTeam team = game.teams.get(Utils.getTeam(uuid, game.teams) - 1);

					game.playersAlive.remove(index);
					game.updateAliveStatus(team);
					game.disconnectedPlayers.remove(uuid);
				}
			}
			//sends an action bar to every play tell info about the game
			for(Player player : Bukkit.getOnlinePlayers())
			{
				World world = player.getWorld();
				String message = ChatColor.GREEN + Utils.longTime(System.currentTimeMillis() - game.start);
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			}
			//checks if anyone won the game
			if(game.teamsAlive == 1 && 
					settings.getBoolean("checkForWin"))
			{
				//stops the shrinking border
				game.setBorderShrinkSpeed(0);

				UHCTeam team = null;
				for(UHCTeam currTeam : game.teams)
				{
					if(currTeam.isAlive())
					{
						team = currTeam;
						break;
					}
				}
				//removes the [] in the string and anounces the winner
				String teamMembers = team.toString().substring(1, team.toString().length() - 1);
				if(game.teamSize == 1)
				{
					Bukkit.broadcastMessage(String.format("%s has won this UHC Game!", teamMembers));
				}
				else
				{
					Bukkit.broadcastMessage(String.format("%s have won this UHC Game!", teamMembers));
				}

				game.status = GameStatus.OVER;
			}
			//adds events eg hyper shrinking
			if(eventCooldown == 0)
			{
				eventCooldown = game.start + settings.getInt("gracePeriodEndsAfter");
				setupForNextEvent(100000);
				//setupForNextEvent(Utils.randint((int) Utils.ONE_MIN * 10 ,(int) Utils.ONE_MIN * 20));
			}
			if(System.currentTimeMillis() > eventCooldown 
					&& game.status == GameStatus.PLAYING)
			{
				switch(nextEvent)
				{
					case HYPER_SHRINK:
						//hyper shrinking
						double shrinkMuti = settings.getDouble("hyperShrinkMuti");
						DecimalFormat format = new DecimalFormat("0.##");
						
						
						game.setBorderShrinkSpeed(settings.getDouble("borderShrinkSpeed") * shrinkMuti);
						
						Bukkit.broadcastMessage(String.format(
								"The border has started hyper shrinking. This means that the border is shrinking %s times faster"
									, format.format(shrinkMuti)));
						//sets a timer until it stops
						BukkitTask graceTimer = new Timer(plugin, game, "The border will stop hyper shrinking in %s"
								, BarColor.RED, settings.getLong("hyperShrinkDuration")
								, TimerType.HYPER_SHRINK_END)
								.runTaskTimer(plugin, 1L, 1L);
						break;
					case LOOT:
						World world = Bukkit.getWorld("uhc");
						Location loc = Utils.rtp((int) world.getWorldBorder().getSize());
						
						world.getBlockAt(loc).setType(Material.CHEST);
						Block block = (Block) world.getBlockAt(loc);
						BlockState blockstate = block.getState();
						Chest chest = (Chest) blockstate;
						
						//TODO maybe add somthing to check if it exists or not
						LootTable loot = Bukkit.getLootTable(new NamespacedKey(plugin, settings.getString("lootEventTable")));
						LootContext context = new LootContext.Builder(loc).build();
						loot.fillInventory(chest.getInventory(), new Random(), context);
						
						Bukkit.broadcastMessage(String.format("Some loot has appered at %d %d %d in the overworld"
								, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
						break;
				}
				//the next event will happend in 15mins-25mins
				setupForNextEvent(Utils.randint((int) Utils.ONE_MIN * 15 ,(int) Utils.ONE_MIN * 25));
			}
		}
	}
	
	public void setupForNextEvent(int delay)
	{
		eventCooldown += delay;
		nextEvent = pickEvent();
		
		//warns the player if a hyper shrink is about to happen
		if(nextEvent == EventType.HYPER_SHRINK)
		{
			long timeLeft = eventCooldown - System.currentTimeMillis();
			timeLeft -= Utils.ONE_MIN; //we take away how ever much we want the warning to be
			timeLeft /= 50; //converting it to minecraft ticks (each tick is 50ms long and there should be 20 in a second) 
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
			{
				public void run()
				{
					BukkitTask hyperShrinkWarn = new Timer(plugin, game, "The Border will start to hyper shrink in %s"
							, BarColor.YELLOW, Utils.ONE_MIN
							, TimerType.WARNING)
							.runTaskTimer(plugin, 1L, 1L);
				}
			}, timeLeft);
		}
	}
	
	public EventType pickEvent()
	{
		ArrayList<EventType> pool = new ArrayList<EventType>();
		pool.add(EventType.HYPER_SHRINK);
		
		//if(eventCooldown - game.start > 40 * Utils.ONE_MIN && Main.validDropLoottable)
		//{
		//	pool.add(EventType.LOOT);
		//}
		
		return pool.get(Utils.randint(0, pool.size() - 1));
	}
}

enum EventType
{
	HYPER_SHRINK, LOOT;
}
