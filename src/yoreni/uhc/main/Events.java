package yoreni.uhc.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;

public class Events implements Listener
{
	final int PLAYER_IMMUNE_PERIOD = 5000;
	HashMap<UUID, Location> lastDeathLocations = new HashMap<UUID, Location>();

	Main main;
	public Events(Main main) 
	{
		this.main = main;
	}

	@EventHandler
	public void whenPlayerDiesDuringTheGame(PlayerDeathEvent event) 
	{
		if(main.game.status == GameStatus.PLAYING)
		{
			try
			{
				Player def = event.getEntity().getPlayer();
				Player atk = event.getEntity().getKiller();

				def.setGameMode(GameMode.SPECTATOR); //when players die thier gamemode gets changes to specteer

				def.sendTitle(String.format("You came %s", 
						Utils.ordinalNumber(main.game.playersAlive.size()))
						, "",5, 20, 5);
				main.game.playersAlive.remove(def.getUniqueId());
				UHCTeam defTeam = main.game.teams.get(Utils.getTeam(def, main.game.teams) - 1);
				main.game.updateAliveStatus(defTeam);

				//code just here for debug just so i know whos being taking off when a player dies
				ArrayList<String> debug = new ArrayList<String>();
				for(UUID uuid : main.game.playersAlive)
				{
					debug.add(Bukkit.getPlayer(uuid).getName());
				}
				main.debug(debug.toString());

				if(atk != null)
				{
					UHCTeam atkTeam = main.game.teams.get(Utils.getTeam(atk, main.game.teams) - 1);
					for(Player player : atkTeam.getPlayers()) //the killer and hishers team gets reneration I for 15 seconds
					{
						player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,15,1));
					}
				}
			}
			catch(NullPointerException NullPointerException)
			{
			}
		}
	}

	@EventHandler
	public void playerRespawn(PlayerRespawnEvent event)
	{
		Player player = event.getPlayer();
		Location newRespawn = lastDeathLocations.get(player.getUniqueId());
		if(newRespawn != null)
		{
			event.setRespawnLocation(newRespawn);
		}
	}

	@EventHandler
	public void playerMove(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		if(event.getFrom() == null)
		{
			return;
		}

		//if the player is in the end then we wont log the last move cos the player will be stuck 
		//when heshe tries to leave the end
		if(event.getFrom().getWorld().getName().contains("_the_end"))
		{
			return;
		}

		if(lastDeathLocations.containsKey(player.getUniqueId()))
		{
			lastDeathLocations.replace(player.getUniqueId(), event.getFrom());
		}
		else
		{
			lastDeathLocations.put(player.getUniqueId(), event.getFrom());
		}
	}

	//this cancelles any unfair damage that a player revices with in the first few seconds of rtp
	@EventHandler
	public void stopUnfairRTPDmg(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			if(System.currentTimeMillis() - main.game.start < PLAYER_IMMUNE_PERIOD)
			{
				if(event.getCause() == DamageCause.SUFFOCATION)
				{
					//teleports the player 1 block higher
					Location loc = player.getLocation();
					loc.setY(loc.getY() + 1);
					player.teleport(loc);
					event.setCancelled(true);
					return;
				}
				if(event.getCause() == DamageCause.FALL)
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler
	public void stopHungerInLobby(FoodLevelChangeEvent event)
	{
		if(main.game.status == GameStatus.WAITING)
		{
			Player player = (Player) event.getEntity();
			player.setFoodLevel(20);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		BukkitTask scoreBoard = new InfoboardUpdate(main, player).runTaskTimer(main, 5L, 5L);
		
		//we want control of the players gamemode and mutiverse messes with it
		HashMap<UUID, PermissionAttachment> perms = new HashMap<UUID, PermissionAttachment>();
		if(!player.hasPermission("mv.bypass.gamemode.*"))
		{
			PermissionAttachment attachment = player.addAttachment(main);
			perms.put(player.getUniqueId(), attachment);
			PermissionAttachment pperms = perms.get(player.getUniqueId());
			pperms.setPermission("mv.bypass.gamemode.*", true);
		}


		if(main.game.status == GameStatus.WAITING)
		{
			//tp the player to the lobby area if a player joins
			World world = Bukkit.getWorld("world");
			Location loc = new Location(world, 0, 201, 0);

			player.getInventory().clear();
			player.getInventory().setItem(0, main.nr);
			player.getInventory().setItem(1, main.playerView);
			player.setGameMode(GameMode.ADVENTURE); 
			player.teleport(loc);
		}
		else if(main.game.status == GameStatus.PLAYING)
		{
			if(main.game.disconnectedPlayers.containsKey(player.getUniqueId()))
			{
				main.game.disconnectedPlayers.remove(player.getUniqueId());
			}

			//set the players gamemode to specteter if the player isnt playing in the game
			if(!main.game.playersAlive.contains(player.getUniqueId()))
			{
				player.setGameMode(GameMode.SPECTATOR);
			}
		}
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if(main.game.status == GameStatus.PLAYING)
		{
			if(main.game.playersAlive.contains(player.getUniqueId()))
			{
				main.game.disconnectedPlayers.put(
						player.getUniqueId(), System.currentTimeMillis());
			}
		}
	}

	@EventHandler
	public void friendlyFire(EntityDamageByEntityEvent event) 
	{
		if(main.game.status == GameStatus.PLAYING && main.game.teamSize >= 2)
		{
			if(event.getEntityType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER)
			{
				Player atk =  (Player) event.getDamager();
				Player def = (Player) event.getEntity();
				if(Utils.getTeam(atk, main.game.teams) == Utils.getTeam(def, main.game.teams))
				{
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void playerCominication(AsyncPlayerChatEvent event)
	{
		if(main.game.status == GameStatus.PLAYING)
		{
			Player player = event.getPlayer();
			//only bother when its not a solo game cos otherwise theres no point
			if(main.game.teamSize >= 2)
			{
				if(event.getMessage().charAt(0) == '!')
				{
					//if the message starts with an "!" send the message globaly
					event.setMessage(event.getMessage().substring(1));
				}
				else
				{
					//if not send it to the players team only
					UHCTeam team = main.game.teams.get(Utils.getTeam(player, main.game.teams) - 1);
					for(Player play : team.getPlayers())
					{
						play.sendMessage(ChatColor.AQUA + "{Team Chat}" +
								ChatColor.WHITE + " <" + player.getName() 
								+ "> " + event.getMessage());
					}
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void ready(PlayerInteractEvent event) 
	{
		if(main.game.status == GameStatus.WAITING)
		{
			Player player = event.getPlayer();
			ItemStack hand = player.getInventory().getItemInMainHand();
			if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
			{
				//ready pice of wool
				if(hand.equals(main.nr))
				{
					player.getInventory().setItemInMainHand(main.r);
					main.ready.add(player.getName());
				}
				if(hand.equals(main.r))
				{
					player.getInventory().setItemInMainHand(main.nr);
					int a = main.ready.indexOf(player.getName());
					main.ready.remove(a);
				}
			}
			//view playing
			if(hand.equals(main.playerView))
			{
				ItemEditer refresh = new ItemEditer();
				refresh.setID(Material.SUNFLOWER);
				refresh.setName("&eRefresh");

				ItemEditer none = new ItemEditer();
				none.setID(Material.BLACK_STAINED_GLASS_PANE);
				none.setName(" ");

				ItemEditer np = new ItemEditer();
				np.setID(Material.ARROW);
				np.setName("&eNext Page");

				ItemEditer pp = new ItemEditer();
				pp.setID(Material.ARROW);
				pp.setName("&ePrev Page");

				Inventory inv = Bukkit.createInventory(null,54,"View Players");

				int a = 0;
				int max = 45;
				if(Bukkit.getOnlinePlayers().size() < 45) max = Bukkit.getOnlinePlayers().size();
				while(a < max)
				{
					ArrayList<Player> players = new ArrayList<Player>();
					for(Player play : Bukkit.getOnlinePlayers())
					{
						players.add(play);
					}

					Player play = players.get(a);

					SkullMeta  skull = (SkullMeta) Bukkit.getItemFactory().getItemMeta(Material.PLAYER_HEAD);
					skull.setOwner(play.getName());
					skull.setDisplayName(ChatColor.RESET + play.getName());
					boolean isReady = main.ready.contains(play.getName());
					ArrayList<String> lore = new ArrayList<String>();
					if(isReady) lore.add(ChatColor.GREEN + "Ready");
					else lore.add(ChatColor.RED + "Not Ready");	
					skull.setLore(lore);

					ItemStack item = new ItemStack(Material.PLAYER_HEAD,1);
					item.setItemMeta(skull);

					inv.setItem(a,item);

					a++;
				}

				inv.setItem(45,none.get()); inv.setItem(46,none.get()); inv.setItem(47,none.get()); inv.setItem(48,none.get());
				inv.setItem(49,refresh.get());
				inv.setItem(50,none.get()); inv.setItem(51,none.get()); inv.setItem(52,none.get()); inv.setItem(53,none.get());

				player.openInventory(inv);
			}
		}
	}

	/*@EventHandler
	public void uhcSetup(InventoryClickEvent event)
	{
		Player player = (Player) event.getWhoClicked();
		if(event.getInventory().get.equals("UHC options"))
		{
			if(event.getSlot() == 10)
			{
				main.teamSize = 1;
				main.done();
			}
			else if(event.getSlot() == 12)
			{
				main.teamSize = 2;
				main.done();
			}
			else if(event.getSlot() == 14)
			{
				main.teamSize = 3;
				main.done();
			}
			else if(event.getSlot() == 16)
			{
				main.teamSize = 4;
				main.done();
			}
		}
	}*/
}