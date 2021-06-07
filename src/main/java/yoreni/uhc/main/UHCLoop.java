package yoreni.uhc.main;

import java.text.DecimalFormat;
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
					//UHCTeam team = game.teams.get(Utils.getTeam(uuid, game.teams) - 1);
					UHCTeam team = Utils.getPlayersTeam(uuid, game.teams);
					
					game.playersAlive.remove(index);
					game.updateAliveStatus(team);
					game.disconnectedPlayers.remove(uuid);
				}
			}

			//sends an action bar to every play tell info about the game
			for(Player player : Bukkit.getOnlinePlayers())
			{
				World world = player.getWorld();
				String message = ChatColor.GREEN + Utils.longTime(game.getGameTime());
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
			}

			//checks if anyone won the game as anouces the winner
			if(game.teamsAlive == 1 && settings.getBoolean("checkForWin"))
			{
				//stops the shrinking border
				game.setBorderShrinkSpeed(0);

				UHCTeam team = null;
				// going through all of the games to see which one is alive
				for(UHCTeam currTeam : game.teams)
				{
					if(currTeam.isAlive())
					{
						team = currTeam;
						break;
					}
				}

				String teamMembers = Utils.prettyList(team.getPlayers());
				if(game.getTeamSize() == 1)
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
			if(eventCooldown == 0) // 0 cos one hasnt been established yet
			{
				eventCooldown = game.getStartTimestamp() + settings.getInt("gracePeriodEndsAfter");
				setupForNextEvent(Utils.randint((int) Utils.ONE_MIN * 10 ,(int) Utils.ONE_MIN * 20));
			}
			if(System.currentTimeMillis() > eventCooldown
					&& game.status == GameStatus.PLAYING)
			{
				switch(nextEvent)
				{
					case HYPER_SHRINK:
						DecimalFormat format = new DecimalFormat("0.##");

						// getting the mutiplyer from the config and applaying the faster hyper shrink
						double shrinkMuti = settings.getDouble("hyperShrinkMuti");
						game.setBorderShrinkSpeed(settings.getDouble("borderShrinkSpeed") * shrinkMuti);
						
						Bukkit.broadcastMessage(String.format(
								"The border has started hyper shrinking. This means that the border is shrinking %s times faster"
									, format.format(shrinkMuti)));

						//displays a timer until it stops
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
	
	private void setupForNextEvent(int delay)
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

	/**
	 * decideds randomly which event will happen next
	 * some events have certain conditions for them to happen
	 *
	 * @return
	 */
	private EventType pickEvent()
	{
		ArrayList<EventType> pool = new ArrayList<EventType>();
		pool.add(EventType.HYPER_SHRINK);
		
		if(eventCooldown - game.getStartTimestamp() > 30 * Utils.ONE_MIN && Main.validDropLootTable)
		{
			pool.add(EventType.LOOT);
		}
		
		return pool.get(Utils.randint(0, pool.size() - 1));
	}
}

enum EventType
{
	HYPER_SHRINK, LOOT;
}
