package org.tyrannyofheaven.bukkit.PowerTool;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PowerToolEntityListener implements Listener {

    private final PowerToolPlugin plugin;
    
    PowerToolEntityListener(PowerToolPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
            if (e.getDamager() instanceof Player) {
                Player attacker = (Player)e.getDamager();
                
                if (!attacker.hasPermission("powertool.use")) return;

                int itemId = attacker.getItemInHand().getTypeId();
                
                if (itemId == Material.AIR.getId()) return;
                
                PowerTool pt = plugin.getPowerTool(attacker, itemId, false);
                if (pt != null) {
                    PowerTool.Command command = pt.getCommand(PowerToolAction.LEFT_CLICK);
                    if (command != null) {
                        String commandString = null;
                        
                        if (command.hasPlayerToken()) {
                            // Player target required
                            if (e.getEntity() instanceof Player) {
                                Player victim = (Player)e.getEntity();
                                debug(plugin, "%s left-clicked %s", attacker.getName(), victim.getName());
                                
                                commandString = command.getCommand().replace(plugin.getPlayerToken(), victim.getName());
                            }
                        }
                        else {
                            // A left-click is a left-click
                            commandString = command.getCommand();
                        }
                        
                        if (commandString != null) {
                            if (command.hasLocationToken()) {
                                commandString = plugin.substituteLocation(attacker, null, commandString, command.hasAirToken());
                            }
                            if (commandString != null) {
                                plugin.execute(attacker, commandString);
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

}
