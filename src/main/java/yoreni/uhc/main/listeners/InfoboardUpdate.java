package yoreni.uhc.main.listeners;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import yoreni.uhc.main.GameStatus;
import yoreni.uhc.main.Infoboard;
import yoreni.uhc.main.Main;
import yoreni.uhc.main.Utils;

public class InfoboardUpdate implements Listener
{
	Main main;
	
	public InfoboardUpdate(Main main)
	{
		this.main = main;
	}

	/**
	 * this generats what the scoreboard will display
	 *
	 * @param player the player of where the infomation will be relevant to
	 * @return
	 */
    private ArrayList<String> makeLines(Player player)
    {
		ArrayList<String> scoreboardLines = new ArrayList<String>();
		scoreboardLines.add("&7Remaining");
		scoreboardLines.add(Main.getGame().getAlivePlayers().size() + " players");
		if(Main.getGame().getTeamSize() >= 2)
		{
			scoreboardLines.add(Main.getGame().getTeamsAlive() + " teams");
		}
		scoreboardLines.add("");
		
		scoreboardLines.add("&7Border");
		scoreboardLines.add("±" + ((int) player.getWorld().getWorldBorder().getSize() / 2));

		int distanceFromBorder = Utils.getDistancefromBorder(player.getLocation());
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
			
			String direction = Utils.directionToArrow(Utils.getDirectionFromBorder(player.getLocation()));
			
			scoreboardLines.add(direction + " " + colour + distanceFromBorder);
		}
		return scoreboardLines;
    }
    
    @EventHandler
    public void addPlayerToScoreboard(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        Infoboard board = new Infoboard();
        board.setTitle("UHC");
        player.setScoreboard(board.getScoreboard());
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.scheduleSyncRepeatingTask(main, () ->
		{
			if(Main.getGame().getStatus() == GameStatus.PLAYING)
			{
				board.updateScoreBoard(makeLines(player));
			}
			else
			{
				//basicly hides the scoreboard when not in a game
				ArrayList<String> scoreboardLines = new ArrayList<String>();
				board.updateScoreBoard(scoreboardLines);
			}
		}, 0L, 5L);

    }
   /* 
    @EventHandler
    public void cancelSchudler(PlayerQuitEvent event)
    {
    	this.cancel();
    }*/
}
