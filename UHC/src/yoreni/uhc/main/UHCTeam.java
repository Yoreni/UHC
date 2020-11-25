package yoreni.uhc.main;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class UHCTeam 
{
	//it gets stored as a UUID type just cos for some reason minecraft doesnt recinoise it when they relog
	private List<UUID> players = new ArrayList<UUID>();
	private int number;
	private boolean alive = true;
	
	public UHCTeam(int number)
	{
		this.number = number;
	}
	public List<Player> getPlayers()
	{
		List<Player> out = new ArrayList<Player>();
		for(UUID player : players)
		{
			UUID uuid = UUID.fromString(player.toString());
			out.add(Bukkit.getPlayer(uuid));
		}
		return out;
	}
	
	public boolean addPlayer(Player player)
	{
		if(players.size() < Main.teamSize && !players.contains(player.getUniqueId()))
		{
			players.add(player.getUniqueId());
			return true;
		}
		else
			return false;
	}
	
	public boolean updateAliveStatus()
	{
		int count = 0;
		
		//an alive team is wether if a team member is still in survival mode
		for(UUID uuid : players)
		{
			if(Main.playersAlive.contains(Bukkit.getPlayer(uuid)))
			{
				count++;
			}
		}
		//if we are going to set the alive boolean to false we knock one of from the teams alive counter
		if(alive && count == 0)
		{
			Main.teamsAlive--;
		}
		alive = count > 0;
		return alive;
	}
	
	public boolean isAlive()
	{
		return alive;
	}
	
	public boolean isInTeam(UUID player)
	{
		return players.contains(player);
	}
	
	public boolean isInTeam(Player player)
	{
		return this.isInTeam(player.getUniqueId());
	}
	
	public int getNumber()
	{
		return number;
	}
	
	@Override
	public String toString()
	{
		List<String> playerNames = new ArrayList<String>();
		for(UUID uuid : players)
		{
			playerNames.add(Bukkit.getPlayer(uuid).getName());
		}
		return playerNames.toString();
	}
}
