package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class PlaceHolders extends PlaceholderExpansion
{
	private Plugin uhcPlugin;
	Main main = null;

	public PlaceHolders(Plugin plugin, String identifier,Main main) 
	{
		this.uhcPlugin = plugin;
		this.main = main;
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier)
	{
		if (identifier.equals("border"))
		{
			World world = player.getWorld();
			return ((int) world.getWorldBorder().getSize() / 2) + "";
		}
		if (identifier.equals("time"))
		{
			return Utils.longTime(Main.getGame().getGameTime());
		}
		return null;
	}

	@Override
	public String getIdentifier()
	{
		return uhcPlugin.getName();
	}
	
    @Override
    public String getVersion()
    {
        return uhcPlugin.getDescription().getVersion();
    }
    
    @Override
    public String getAuthor()
    {
    	List<String> authors = uhcPlugin.getDescription().getAuthors();
        return Utils.prettyList(authors);
    }
    
    @Override
    public boolean canRegister()
    {
        return true;
    }
}