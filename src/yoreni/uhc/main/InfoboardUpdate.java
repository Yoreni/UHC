package yoreni.uhc.main;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class InfoboardUpdate extends BukkitRunnable implements Listener
{
	Main main;
	Infoboard board;
	Player player;
	
	public InfoboardUpdate(JavaPlugin plugin, Player player)
	{
		main = (Main) plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
		
		board = new Infoboard();
		board.setTitle("UHC");
		this.player = player;
    	//for(Player player : Bukkit.getOnlinePlayers())
    	//{
    		player.setScoreboard(board.getScoreboard());
    	//}
	}
	
    @Override
    public void run() 
    {
    	if(main.game.status == GameStatus.PLAYING)
    	{
    		//for(Player player : Bukkit.getOnlinePlayers())
    		//{
    			board.updateScoreBoard(makeLines(player));
    		//}
    	}
    	else
    	{
    		//basicly hides the scoreboard when not in a game
    		ArrayList<String> scoreboardLines = new ArrayList<String>();
    		board.updateScoreBoard(scoreboardLines);
    	}
    }
    
    private ArrayList<String> makeLines(Player player)
    {
		ArrayList<String> scoreboardLines = new ArrayList<String>();
		scoreboardLines.add("&7Remaining");
		scoreboardLines.add(main.game.playersAlive.size() + " players");
		if(main.game.teamSize >= 2)
		{
			scoreboardLines.add(main.game.teamsAlive + " teams");
		}
		scoreboardLines.add("");
		
		scoreboardLines.add("&7Border");
		scoreboardLines.add("±" + ((int) player.getWorld().getWorldBorder().getSize() / 2));

		int distanceFromBorder = Utils.getDistancefromBorder(player.getLocation(), player.getWorld().getWorldBorder());
		if(distanceFromBorder <= 100)
		{
			String colour = "&e";
			if(distanceFromBorder <= 0)
			{
				colour = "&4";
			}
			else if(distanceFromBorder <= 10)
			{
				colour = "&c";
			}
			else if(distanceFromBorder <= 50)
			{
				colour = "&6";
			}
			
			String direction = Utils.directionToArrow(
					Utils.getDirectionFromBorder(player.getLocation(), player.getWorld().getWorldBorder()));
			
			scoreboardLines.add(direction + " " + colour + distanceFromBorder);
		}
		return scoreboardLines;
    }
    
    /*@EventHandler
    public void addPlayerToScoreboard(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        player.setScoreboard(board.getScoreboard());
    }*/
    
    @EventHandler
    public void cancelSchudler(PlayerQuitEvent event)
    {
    	this.cancel();
    }
}
