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

	public PlaceHolders(Plugin plugin, String identifier) 
	{
		this.uhcPlugin = plugin;
	}

	public String longTime(long x)
	{
		int time = (int) x / 1000;
		int h = (int) Math.floor(time / 3600);
		int m = (int) Math.floor(time / 60) % 60;
		int s = time % 60;
		NumberFormat format = new DecimalFormat("00");
		return h + ":" + format.format(m) + ":" + format.format(s);
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
			return longTime(System.currentTimeMillis() - Main.start);
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