package top.sleepingbed.smartmetro.listeners;

import top.sleepingbed.smartmetro.SmartMetro;
import top.sleepingbed.smartmetro.models.Ticket;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class TicketListener implements Listener {
    
    private final SmartMetro plugin;
    
    public TicketListener(SmartMetro plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check if it's a right-click on a block with a ticket
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || 
            event.getHand() != EquipmentSlot.HAND || 
            event.getClickedBlock() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        String locale = plugin.getLocaleManager().getPlayerLocale(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        // Check if the player is holding a ticket
        if (!Ticket.isTicket(plugin, heldItem)) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        
        // Check if the clicked block is a rail
        if (!isRail(clickedBlock.getType())) {
            return;
        }
        
        // Get ticket destination
        String destination = Ticket.getDestination(plugin, heldItem);
        if (destination == null) {
            String invalidMsg = plugin.getLocaleManager().getMessage(locale, "ticket.invalid");
            player.sendMessage("§c" + invalidMsg);
            return;
        }
        
        // Spawn a minecart and set its destination
        event.setCancelled(true);
        
        // Remove one ticket from the player's hand
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        // Spawn the minecart
        Minecart minecart = clickedBlock.getWorld().spawn(clickedBlock.getLocation().add(0.5, 0.5, 0.5), Minecart.class);
        
        // Mark this minecart as a metro minecart (for collision handling)
        minecart.setMetadata("metro_minecart", new FixedMetadataValue(plugin, true));
        
        // Set metadata for the destination
        minecart.setMetadata("metro_destination", new FixedMetadataValue(plugin, destination));
        
        // Add the player to the minecart
        minecart.addPassenger(player);
        
        String headingMsg = plugin.getLocaleManager().getMessage(locale, "minecart.heading");
        player.sendMessage("§a" + String.format(headingMsg, 
                plugin.getStationManager().getStation(destination).getName()));
    }
    
    private boolean isRail(Material material) {
        return material == Material.RAIL || 
               material == Material.POWERED_RAIL || 
               material == Material.DETECTOR_RAIL || 
               material == Material.ACTIVATOR_RAIL;
    }
}

