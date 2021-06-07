package yoreni.uhc.main.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;
import yoreni.uhc.main.GameStatus;
import yoreni.uhc.main.Main;

import java.util.HashMap;
import java.util.UUID;

public class InLobby implements Listener
{
    Main main;

    public InLobby(Main main)
    {
        this.main = main;
    }

    @EventHandler
    public void stopHungerInLobby(FoodLevelChangeEvent event)
    {
        if(Main.getGame().getStatus() == GameStatus.WAITING)
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

        //we want control of the players gamemode and mutiverse messes with it
        HashMap<UUID, PermissionAttachment> perms = new HashMap<UUID, PermissionAttachment>();
        if(!player.hasPermission("mv.bypass.gamemode.*"))
        {
            PermissionAttachment attachment = player.addAttachment(Main.getInstance());
            perms.put(player.getUniqueId(), attachment);
            PermissionAttachment pperms = perms.get(player.getUniqueId());
            pperms.setPermission("mv.bypass.gamemode.*", true);
        }


        if(Main.getGame().getStatus() == GameStatus.WAITING)
        {
            //tp the player to the lobby area if a player joins
            World world = Bukkit.getWorld("world");
            Location loc = new Location(world, 0, 201, 0);

            player.getInventory().clear();
            //player.getInventory().setItem(0, main.nr);
            //player.getInventory().setItem(1, main.playerView);
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(loc);
        }
    }

    	/*
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
					if(isReady)
					{
						lore.add(ChatColor.GREEN + "Ready");
					}
					else
					{
						lore.add(ChatColor.RED + "Not Ready");
					}
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
	}*/

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
