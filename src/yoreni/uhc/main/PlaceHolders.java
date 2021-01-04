package yoreni.uhc.main;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

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
			return Utils.longTime(main.game.getGameTime());
		}
		return null;
	}

	@Override
	public String getIdentifier()
	{
		return "uhc"; 
	}
	
    @Override
    public String getVersion()
    {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor()
    {
        return "Yoreni";
    }
    
    @Override
    public boolean canRegister()
    {
        return true;
    }
}