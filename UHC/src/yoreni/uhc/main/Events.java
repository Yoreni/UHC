package yoreni.uhc.main;

import java.util.ArrayList;

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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

public class Events implements Listener
{

	Main main;
	public Events(Main main) 
	{
		this.main = main;
	}

	@EventHandler
	public void whenPlayerDiesDuringTheGame(PlayerDeathEvent event) 
	{
		try
		{
			Player def = event.getEntity().getPlayer();
			Player atk = event.getEntity().getKiller();

			def.setGameMode(GameMode.SPECTATOR); //when players die thier gamemode gets changes to specteer
			
			Main.playersAlive.remove(def.getUniqueId());
			UHCTeam defTeam = main.teams.get(main.getTeam(def) - 1);
			defTeam.updateAliveStatus();		
		
			if(atk != null)
			{
				UHCTeam atkTeam = main.teams.get(main.getTeam(atk) - 1);
				for(Player player : atkTeam.getPlayers()) //the killer and hishers team gets reneration I for 15 seconds
					player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,15,1));
			}
		}
		catch(NullPointerException NullPointerException)
		{
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		
		if(main.status == GameStatus.WAITING)
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
		else if(main.status == GameStatus.PLAYING)
		{
			if(main.disconnectedPlayers.containsKey(player.getUniqueId()))
			{
				main.disconnectedPlayers.remove(player.getUniqueId());
			}
			//add the boss bars if still counting down
			if(System.currentTimeMillis() - main.shrink < 0)
			{
				main.borderTimer.addPlayer(player);
			}
			if(System.currentTimeMillis() - main.grace < 0)
			{
				main.graceTimer.addPlayer(player);
			}
			
			//set the players gamemode to specteter if the player isnt playing in the game
			if(!Main.playersAlive.contains(player.getUniqueId()))
			{
				player.setGameMode(GameMode.SPECTATOR);
			}
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if(main.status == GameStatus.PLAYING)
		{
			if(Main.playersAlive.contains(player.getUniqueId()))
			{
				main.disconnectedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
			}
		}
	}

	@EventHandler
	public void friendlyFire(EntityDamageByEntityEvent event) 
	{
		if(main.status == GameStatus.PLAYING && Main.teamSize >= 2)
		{
			if(event.getEntityType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER)
			{
				Player atk =  (Player) event.getDamager();
				Player def = (Player) event.getEntity();
				if(main.getTeam(atk) == main.getTeam(def))
				{
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void playerCominication(AsyncPlayerChatEvent event)
	{
		if(main.status == GameStatus.PLAYING)
		{
			Player player = event.getPlayer();
			//only bother when its not a solo game cos otherwise theres no point
			if(Main.teamSize >= 2)
			{
				if(event.getMessage().charAt(0) == '!')
				{
					//if the message starts with an "!" send the message globaly
					event.setMessage(event.getMessage().substring(1));
				}
				else
				{
					//if not send it to the players team only
					UHCTeam team = main.teams.get(main.getTeam(player) - 1);
					for(Player play : team.getPlayers())
					{
						play.sendMessage(ChatColor.AQUA + "{Team Chat}" + ChatColor.WHITE + " <" + player.getName() + "> " + event.getMessage());
					}
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void ready(PlayerInteractEvent event) 
	{
		if(main.status == GameStatus.WAITING)
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
					int a = 0;
					for(String string : main.ready)
					{
						if(main.ready.get(a).equals(player.getName())) break;
						else a++;
					}
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