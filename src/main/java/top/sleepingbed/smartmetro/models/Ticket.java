package top.sleepingbed.smartmetro.models;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import top.sleepingbed.smartmetro.SmartMetro;

import java.util.ArrayList;
import java.util.List;

public class Ticket {
  
  private static final NamespacedKey DESTINATION_KEY = new NamespacedKey("smartmetro", "destination");
  private static final NamespacedKey FROM_KEY = new NamespacedKey("smartmetro", "from");
  
  public static ItemStack createTicket(SmartMetro plugin, Player player, String from, String destination, String fromName, String destinationName) {
      Material ticketMaterial = Material.PAPER;
      ItemStack ticket = new ItemStack(ticketMaterial);
      ItemMeta meta = ticket.getItemMeta();
      
      if (meta != null) {
          String locale = plugin.getLocaleManager().getPlayerLocale(player);
          
          meta.setDisplayName("§6" + plugin.getLocaleManager().getMessage(locale, "ticket.name"));
          
          List<String> lore = new ArrayList<>();
          lore.add("§7" + String.format(plugin.getLocaleManager().getMessage(locale, "ticket.from"), fromName));
          lore.add("§7" + String.format(plugin.getLocaleManager().getMessage(locale, "ticket.to"), destinationName));
          lore.add("");
          lore.add("§7" + plugin.getLocaleManager().getMessage(locale, "ticket.usage"));
          meta.setLore(lore);
          
          PersistentDataContainer container = meta.getPersistentDataContainer();
          container.set(new NamespacedKey(plugin, "ticket"), PersistentDataType.BYTE, (byte) 1);
          container.set(new NamespacedKey(plugin, "destination"), PersistentDataType.STRING, destination);
          container.set(new NamespacedKey(plugin, "from"), PersistentDataType.STRING, from);
          
          ticket.setItemMeta(meta);
      }
      
      return ticket;
  }
  
  public static boolean isTicket(Plugin plugin, ItemStack item) {
      if (item == null || !item.hasItemMeta()) {
          return false;
      }
      
      ItemMeta meta = item.getItemMeta();
      PersistentDataContainer container = meta.getPersistentDataContainer();
      return container.has(new NamespacedKey(plugin, "ticket"), PersistentDataType.BYTE);
  }
  
  public static String getDestination(Plugin plugin, ItemStack ticket) {
      if (!isTicket(plugin, ticket)) {
          return null;
      }
      
      ItemMeta meta = ticket.getItemMeta();
      PersistentDataContainer container = meta.getPersistentDataContainer();
      return container.get(new NamespacedKey(plugin, "destination"), PersistentDataType.STRING);
  }
  
  public static String getOrigin(Plugin plugin, ItemStack ticket) {
      if (!isTicket(plugin, ticket)) {
          return null;
      }
      
      ItemMeta meta = ticket.getItemMeta();
      PersistentDataContainer container = meta.getPersistentDataContainer();
      return container.get(new NamespacedKey(plugin, "from"), PersistentDataType.STRING);
  }
}

