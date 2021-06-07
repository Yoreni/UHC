package yoreni.uhc.main;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class UHCTeam
{
	/**
	 * The colour of the teams.
	 * These colours will help display to players which team everyone is on
	 *
	 * Team 1 is always red
	 * Team 2 is always blue
	 * Team 3 is always green
	 * etc
	 */
	public static final ChatColor[] TEAM_COLOURS = { ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.GOLD,
			ChatColor.LIGHT_PURPLE, ChatColor.AQUA, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
			ChatColor.DARK_RED, ChatColor.DARK_AQUA, ChatColor.GRAY, ChatColor.DARK_GRAY, ChatColor.WHITE,
			ChatColor.BLACK};


	//it gets stored as a UUID type just cos for some reason minecraft doesnt recinoise it when they relog
	private List<UUID> players = new ArrayList<UUID>();
	private int id;

	/**
	 * this is true if there exists a player that is not dead
	 */
	private boolean alive = true;
	
	public UHCTeam(int number)
	{
		this.id = number;
	}

	/**
	 * @return the players in this team
	 */
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

	/**
	 * @return the players in this team but as UUID objects
	 */
	public List<UUID> getUUIDs()
	{
		return players;
	}

	/**
	 * adds a player to the team
	 *
	 * @param player
	 * @return whether this function was sucessful
	 */
	public boolean addPlayer(Player player)
	{
		if(players.size() < Main.getGame().getTeamSize() && !players.contains(player.getUniqueId()))
		{
			players.add(player.getUniqueId());
			return true;
		}
		else
		{
			return false;
		}
	}

	public void markDead()
	{
		alive = false;
	}
	
	public boolean isAlive()
	{
		return alive;
	}

	/**
	 * gets if a player is in this team
	 *
	 * @param player
	 * @return
	 */
	public boolean isInTeam(UUID player)
	{
		return players.contains(player);
	}
	
	public boolean isInTeam(Player player)
	{
		return this.isInTeam(player.getUniqueId());
	}

	/**
	 * gets which number the team is
	 *
	 * @return
	 */
	public int getNumber()
	{
		return id;
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
