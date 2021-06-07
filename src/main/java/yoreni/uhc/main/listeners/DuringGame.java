package yoreni.uhc.main.listeners;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yoreni.uhc.main.GameStatus;
import yoreni.uhc.main.Main;
import yoreni.uhc.main.UHCTeam;
import yoreni.uhc.main.Utils;

import java.util.*;

public class DuringGame implements Listener
{
    private static final int PLAYER_IMMUNE_PERIOD = 5000;
    private HashMap<UUID, Location> lastDeathLocations = new HashMap<UUID, Location>();

    Main main;

    public DuringGame(Main main)
    {
        this.main = main;
    }

    @EventHandler
    public void whenPlayerDiesDuringTheGame(PlayerDeathEvent event)
    {
        if (Main.getGame().getStatus() == GameStatus.PLAYING)
        {
            // removed try catch statment
            Player def = event.getEntity().getPlayer();
            Player atk = event.getEntity().getKiller();

            Main.getGame().eliminate(def);

            if (atk != null)
            {
                UHCTeam atkTeam = Utils.getPlayersTeam(atk, main.getGame().getTeams());
                for (Player player : atkTeam.getPlayers()) //the killer and hishers team gets reneration I for 15 seconds
                {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 15, 1));
                }
            }
        }
    }

    //this cancelles any unfair damage that a player revices with in the first few seconds of rtp
    @EventHandler
    public void stopUnfairRTPDmg(EntityDamageEvent event)
    {
        if(Main.getGame().getStatus() != GameStatus.PLAYING)
        {
            return;
        }

        if(event.getEntity() instanceof Player)
        {
            Player player = (Player) event.getEntity();
            if(Main.getGame().getGameTime() < PLAYER_IMMUNE_PERIOD)
            {
                if(event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION)
                {
                    //teleports the player 1 block higher so the player wont receive the same damage again
                    Location loc = player.getLocation();
                    loc.setY(loc.getY() + 1);
                    player.teleport(loc);

                    event.setCancelled(true);
                    return;
                }
                if(event.getCause() == EntityDamageEvent.DamageCause.FALL)
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event)
    {
        if(Main.getGame().getStatus() != GameStatus.PLAYING)
        {
            return;
        }

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

    @EventHandler
    public void friendlyFire(EntityDamageByEntityEvent event)
    {
        if(Main.getGame().getStatus() == GameStatus.PLAYING)
        {
            //if they are both players
            if(event.getEntityType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER)
            {
                final ArrayList<UHCTeam> teams = Main.getGame().getTeams();
                Player atk =  (Player) event.getDamager();
                Player def = (Player) event.getEntity();

                //if they are both on the same team
                if(Utils.getPlayersTeamNumber(atk, teams) == Utils.getPlayersTeamNumber(def, teams))
                {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void playerCominication(AsyncPlayerChatEvent event)
    {
        if(Main.getGame().getStatus() == GameStatus.PLAYING)
        {
            Player player = event.getPlayer();
            //only bother when its not a solo game cos otherwise theres no point
            if(Main.getGame().getTeamSize() >= 2)
            {
                //if the message starts with an "!" send the message globaly
                if(event.getMessage().charAt(0) == '!')
                {
                    event.setMessage(event.getMessage().substring(1));
                }
                else
                {
                    //if not send it to the players team only
                    UHCTeam team = Utils.getPlayersTeam(player, Main.getGame().getTeams());
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
    public void dobbleAppleDrops(BlockBreakEvent event)
    {
        String[] worlds = {Main.getGame().UHC_WORLD_NAME
                , Main.getGame().UHC_WORLD_NAME + "_nether"
                , Main.getGame().UHC_WORLD_NAME + "_the_end"};
        List<String> allowedWorlds = new ArrayList(Arrays.asList(worlds));

        //get varibles that will be useful
        Location loc = event.getBlock().getLocation();
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        //use normal rates if in worng world
        if(!allowedWorlds.contains(loc.getWorld().getName()))
        {
            return;
        }

        //ignore this if its not a oask or dark oak leaf
        if(type != Material.OAK_LEAVES && type != Material.DARK_OAK_LEAVES)
        {
            return;
        }

        //remove the block
        event.setCancelled(true);
        event.getBlock().setType(Material.AIR);

        //this is really janky to code and the api should let you damage tools by minecrafts
        //api so guess what free duability when you break any kind of oak leaves with a tool

        if(itemInHand.containsEnchantment(Enchantment.SILK_TOUCH) || itemInHand.getType() == Material.SHEARS)
        {
            //itemInHand.getType().getMaxDurability()
            loc.getWorld().dropItemNaturally(loc,new ItemStack(type));
        }
        else
        {
            //get fortune level also make sure its not a number where an error could happen
            int fortuneLevel = itemInHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
            fortuneLevel = Utils.clamp(fortuneLevel, 0, 4);

            //speficaes the rates
            double[] saplingRate = {0.05, 0.0625, 0.083333336, 0.1, 0.133333333};
            double[] stickRate = {0.02, 0.022222223, 0.025, 0.033333335};
            double[] appleRate = {0.01, 0.0111111112, 0.0125, 0.016666667, 0.05};

            //drop the items
            if(Math.random() < saplingRate[fortuneLevel])
            {
                if(type == Material.DARK_OAK_LEAVES)
                {
                    loc.getWorld().dropItemNaturally(loc,new ItemStack(Material.DARK_OAK_SAPLING));
                }
                else
                {
                    loc.getWorld().dropItemNaturally(loc,new ItemStack(Material.OAK_SAPLING));
                }
            }

            if(Math.random() < stickRate[fortuneLevel])
            {
                loc.getWorld().dropItemNaturally(loc,new ItemStack(Material.STICK, (new Random().nextInt(1)) + 1));
            }

            if(Math.random() < appleRate[fortuneLevel])
            {
                loc.getWorld().dropItemNaturally(loc,new ItemStack(Material.APPLE));
            }
        }
    }

    @EventHandler
    public void disablePhantoms(CreatureSpawnEvent event)
    {
        if(Main.getGame().getStatus() != GameStatus.PLAYING)
        {
            return;
        }

        //if its not a phantom we dont care
        if(event.getEntityType() != EntityType.PHANTOM)
        {
            return;
        }

        if(event.getLocation().getWorld().getName().equals(Main.getGame().UHC_WORLD_NAME))
        {
            //we will disable phantoms for the first hour of the game
            //after that we will hope that players are geared up enough
            if(Main.getGame().getGameTime() < Utils.ONE_HOUR)
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        if(Main.getGame().getStatus() == GameStatus.PLAYING)
        {
            if(Main.getGame().getDisconnectedPlayers().containsKey(player.getUniqueId()))
            {
                Main.getGame().removeFromDisconnectedPlayers(player);
            }

            //set the players gamemode to specteter if the player isnt playing in the game
            if(!Main.getGame().getAlivePlayers().contains(player.getUniqueId()))
            {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        if(Main.getGame().getStatus() == GameStatus.PLAYING)
        {
            if(Main.getGame().getAlivePlayers().contains(player.getUniqueId()))
            {
                Main.getGame().addToDisconnectedPlayers(player);
            }
        }
    }
}
