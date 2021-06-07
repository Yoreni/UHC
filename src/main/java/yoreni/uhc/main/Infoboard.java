package yoreni.uhc.main;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import net.md_5.bungee.api.ChatColor;

public class Infoboard
{
	private static ScoreboardManager manger = Bukkit.getScoreboardManager();
	private Scoreboard board;
	private Objective objective;
	private ArrayList<Team> lines = new ArrayList<Team>();
	
	public Infoboard()
	{
		this(manger.getNewScoreboard());
		//board = manger.getNewScoreboard();
		//objective =  board.registerNewObjective("obj", "dummy", "obj");
		//objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
	
	public Infoboard(Scoreboard scoreboard)
	{
		board = scoreboard;
		objective =  board.registerNewObjective("obj", "dummy", "obj");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
	
	public void setTitle(String title)
	{
		objective.setDisplayName(title);
	}
	
	public Scoreboard getScoreboard()
	{
		return board;
	}
	
	public void updateScoreBoard(ArrayList<String> text)
	{
		int index = 0;
		
		for(String line : text)
		{
			String id = "line" + index;
			line = ChatColor.translateAlternateColorCodes('&', line);
			if(index + 1 > lines.size())
			{
				Team team = board.registerNewTeam(id);
				team.addEntry(index + "");
				team.setPrefix("");
				team.setSuffix("");
				lines.add(team);
			}
			
			Team team = lines.get(index);
			team.setPrefix(line);
			objective.getScore(line).setScore(16 - index);
			
			index++;
		}
		
		
		//code yeeted from
		//https://github.com/WinterAlexander/EasyScoreboards/blob/master/src/me/winterguardian/easyscoreboards/ScoreboardUtil.java
		for(String entry : board.getEntries())
		{
			//we need to strip the colour for both just so when we are checking if the
			//strings we equal to each other it works probly
			String entryStriped = ChatColor.stripColor(entry);
			boolean toErase = true;
			
			for(String element : text)
			{
				String elementStripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', element));
				
				boolean sameScore = 
						board.getObjective("obj").getScore(entry).getScore() == 16 - text.indexOf(element);
				
				if(element != null && elementStripped.equals(entryStriped) && sameScore)
				{
					toErase = false;
					break;
				}
			}

			if(toErase)
			{
				board.resetScores(entry);
			}

		}
	}
}
