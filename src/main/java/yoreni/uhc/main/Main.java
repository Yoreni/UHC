package yoreni.uhc.main;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;
import yoreni.uhc.main.listeners.DuringGame;
import yoreni.uhc.main.listeners.InLobby;
import yoreni.uhc.main.listeners.InfoboardUpdate;

public class Main extends JavaPlugin implements Listener
{
    private static Main instance;

    /**
     * this is true if the name space for the loot table definded in the config exists
     */
    static boolean validDropLootTable = false;
    ArrayList<String> ready = new ArrayList<String>();

    private boolean eventsRegistered = false;

    // Item varibles
    ItemStack nr = null;
    ItemStack r = null;
    ItemStack playerView = null;

    private static UHC game = new UHC();

    //config files
    ConfigFile config;

    public void onEnable()
    {
        instance = this;

        //sets up the congi file
        config = new ConfigFile(this);
        config.setup("config");

        if(!config.getBoolean("enabled"))
        {
            Bukkit.getLogger().info("Plugin has been disabled cos it says so in the plugin.yml to change it change the boolean value in the plugin.yml");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        //gets some of the values in the config
        game.setTeamSizeFromConfig();
        String loottable = config.getString("lootEventTable");
        if(!loottable.equals(""))
        {
            validDropLootTable =
                    Utils.isValidLootTable(new NamespacedKey(this, loottable));
        }
        else
        {
            validDropLootTable = false;
        }

        //hook into PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
        {
            new PlaceHolders(this, "uhc", this);// .hook();
        }

    	/*for(Player player : Bukkit.getOnlinePlayers())
    	{
    		BukkitTask scoreBoard = new InfoboardUpdate(this, player).runTaskTimer(this, 5L, 5L);
    	}*/
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
         */
    }



    public void onDisable()
    {

    }

    public boolean onCommand(CommandSender sender
            , Command command, String label, String[] args)
    {
        /*
            we are registering events here cos if we do it in the on enable we get an error
            saying org.bukkit.plugin.IllegalPluginAccessException: Plugin attempted to register events
            while not enabled which is a bit odd
         */
        registerEvents();

        //TODO put these commands in thier own classes
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

                    if(game.status == GameStatus.PLAYING)
                    {
                        sender.sendMessage("Config cant be reloaded during a game!");
                        return false;
                    }
                    else
                    {
                        config.reload();

                        game.setTeamSizeFromConfig();
                        validDropLootTable =
                                Utils.isValidLootTable(new NamespacedKey(this, config.getString("lootEventTable")));

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
            if(game.status == GameStatus.OVER)
            {
                game = new UHC();
            }


            if (game.status == GameStatus.OFF)
            {
                game.setupGame();
            }
            else if (game.status == GameStatus.WAITING)
            {
                game.startGame();
            }
            return true;
        }
        return false;
    }

    public void debug(String message)
    {
        if(config.getBoolean("debugMessages"))
        {
            Bukkit.getLogger().info(message);
        }
    }

    private void registerEvents()
    {
        if(!eventsRegistered)
        {
            //Bukkit.getPluginManager().registerEvents(this, this);
            Bukkit.getPluginManager().registerEvents(new InLobby(this), this);
            Bukkit.getPluginManager().registerEvents(new DuringGame(this), this);
            Bukkit.getPluginManager().registerEvents(new InfoboardUpdate(this), this);

            //setup the scoreboard for players
            BukkitTask loop = new UHCLoop(this, game, config).runTaskTimer(this, 1L, 1L);

            eventsRegistered = true;
        }
    }

    public static Main getInstance()
    {
        return instance;
    }

    public static UHC getGame()
    {
        return game;
    }
}