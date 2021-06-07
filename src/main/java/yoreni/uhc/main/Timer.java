package yoreni.uhc.main;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Timer extends BukkitRunnable implements Listener
{
    private final JavaPlugin plugin;

    /**
     *  the UHC game going on
     */
    private UHC game;

    /**
     *  the title that will be displayed with the bossbar
     */
    private String title;
    private BossBar bar;

    /**
     *  the timestamp of where the time will expire
     */
    private long expires = 0;

    /**
     *  the length of the timer in milliseconds
     */
    private long duration = 0;

    /**
     * the type of timer (ceritan things happen when the timer runs out depending on the timer type)
     */
    private TimerType type;
    private Main main;

    public Timer(JavaPlugin plugin, UHC game, String title
    		, BarColor colour, long expires, TimerType type) 
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.game = game;
        this.title = title;
        this.expires = System.currentTimeMillis() + expires;
        this.duration = expires;
        this.type = type;
        this.main = (Main) plugin;
        
        this.bar = Bukkit.createBossBar("", colour, BarStyle.SOLID);
        for(Player player : Bukkit.getOnlinePlayers())
        {
        	this.bar.addPlayer(player);
        }
        
    }

    @Override
    public void run() 
    {
    	if(!hasExpired())
    	{
    		//keeps on updating the boss bar for all of the players
    		long timeLeft = expires - System.currentTimeMillis(); 
    		this.bar.setTitle(String.format(title, Utils.shortTime(timeLeft)));
    		this.bar.setProgress((double) (timeLeft) / this.duration);
    	}
    	else
    	{
    		//after the timer has ran out it exicutes the things
    		this.bar.removeAll();
    		switch(type)
    		{
    			case BORDER_SHRINK_START:
    				Bukkit.broadcastMessage("The Border has started to shrink");
    				game.setBorderShrinkSpeed(main.config.getDouble("borderShrinkSpeed"));
    				break;
    			case GRACE_END:
    				game.setPVP(true);
    				Bukkit.broadcastMessage("The Grace period has ended");
    				break;
    			case HYPER_SHRINK_END:
    				Bukkit.broadcastMessage("The Border has stopped hyper shrinking");
    				game.setBorderShrinkSpeed(main.config.getDouble("borderShrinkSpeed"));
    				break;
    		}
    		this.cancel();
    	}
    }

    // making sure we are showing the time to players who just joined
    @EventHandler
    public void showTheBarToThePlayer(PlayerJoinEvent event)
    {
        if(!hasExpired())
        {
        	this.bar.addPlayer(event.getPlayer());
        }
    }
    
    private boolean hasExpired()
    {
    	return System.currentTimeMillis() >  expires;
    }
}
