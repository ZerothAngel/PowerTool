package org.tyrannyofheaven.bukkit.PowerTool;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.registerEvent;

import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityListener;

public class PowerToolEntityListener extends EntityListener {

    private final PowerToolPlugin plugin;
    
    PowerToolEntityListener(PowerToolPlugin plugin) {
        this.plugin = plugin;
    }

    void registerEvents() {
        registerEvent("ENTITY_DAMAGE", this, Priority.Normal, plugin);
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
            if (e.getDamager() instanceof Player) {
                Player attacker = (Player)e.getDamager();
                
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
                            // TODO Refactor me
                            debug(plugin, "Executing command: %s", commandString);
                            try {
                                plugin.getServer().dispatchCommand(attacker, commandString);
                            }
                            catch (CommandException ex) {
                                error(plugin, "Execution failed: %s", commandString, ex);
                            }
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

}
