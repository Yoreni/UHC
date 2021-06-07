package yoreni.uhc.main;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemEditer 
{
	ItemStack item = new ItemStack(Material.STONE);
	ItemMeta meta = item.getItemMeta();
	
	//Main main;
	public ItemEditer()
	{
		//this.main = main;
	}
	
	private void getMeta()
	{
		meta = item.getItemMeta();
	}
	
	private void setMeta()
	{
		item.setItemMeta(meta);
	}
	
	public void setID(Material material)
	{
		item.setType(material);
	}
	
	public void setAmount(int amount)
	{
		item.setAmount(amount);
	}
	
	public ItemStack get()
	{
		return item;
	}
	
	public void setName(String name)
	{
		getMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',name));
		setMeta();
	}
	
	public void setLore(ArrayList<String> lore)
	{
		for(String string : lore)
		{
			string = ChatColor.translateAlternateColorCodes('&', string);
		}
		getMeta();
		meta.setLore(lore);
		setMeta();
	}
	
	public void addLoreLine(String line)
	{
		getMeta();
		meta.getLore().add(ChatColor.translateAlternateColorCodes('&',line));
		setMeta();
	}
	
	public void setSkullHead(String UUID)
	{
		if(!item.getType().equals(Material.PLAYER_HEAD)) return;
		SkullMeta smeta = (SkullMeta) meta;
		smeta.setOwner(UUID);
		setMeta();
	}

}