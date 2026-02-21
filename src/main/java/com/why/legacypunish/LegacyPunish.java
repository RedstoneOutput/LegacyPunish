package com.yourname.legacypunish;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LegacyPunish extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> tormented = new HashSet<>();
    private final String[] creepypastas = {"Herobrine", "Entity_303", "Null", "Null_User", "The_Watcher", "Red_Eagle"};

    @Override
    public void onEnable() {
        getCommand("shadow").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (UUID uuid : tormented) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (!online.getUniqueId().equals(uuid)) {
                                p.hidePlayer(online);
                            }
                        }
                        p.setPlayerListName("§4" + creepypastas[(int)(Math.random() * creepypastas.length)]);
                        
                        // NEW: snap them back to the shadow world if they somehow leave it
                        if (!p.getWorld().getName().equalsIgnoreCase("shadow")) {
                            sendToShadow(p);
                        }
                    }
                }
            }
        }, 100L, 100L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("shadow")) {
            if (args.length != 1) {
                sender.sendMessage("§cUsage: /shadow <username>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            if (tormented.contains(target.getUniqueId())) {
                tormented.remove(target.getUniqueId());
                for (Player online : Bukkit.getOnlinePlayers()) {
                    target.showPlayer(online);
                }
                target.setPlayerListName(target.getName());
                sender.sendMessage("§a" + target.getName() + " has returned to reality.");
            } else {
                tormented.add(target.getUniqueId());
                degradeInventory(target);
                target.setHealth(1.0); 
                sendToShadow(target); // Force them into the world
                sender.sendMessage("§4[LEGACY] §c" + target.getName() + " is now haunted in the shadow world.");
            }
            return true;
        }
        return false;
    }

    // NEW: Helper function to send player to the "shadow" world
    private void sendToShadow(Player p) {
        World shadowWorld = Bukkit.getWorld("shadow");
        if (shadowWorld != null) {
            p.teleport(shadowWorld.getSpawnLocation());
        } else {
            // Log error if world doesn't exist so you know to create it
            Bukkit.getLogger().warning("[LEGACY] WORLD 'shadow' NOT FOUND! Please create it.");
        }
    }

    // NEW: Block all teleport attempts out of the shadow world
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (tormented.contains(event.getPlayer().getUniqueId())) {
            if (!event.getTo().getWorld().getName().equalsIgnoreCase("shadow")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§4§lHerobrine: §cThere is no escape.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (tormented.contains(p.getUniqueId())) {
            p.sendMessage("§4§lHerobrine: §cYou can't leave yet.");
            p.playSound(p.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandProcess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (tormented.contains(event.getPlayer().getUniqueId())) {
            if (msg.startsWith("/list") || msg.startsWith("/who") || msg.startsWith("/online")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§fThere are 0/100 players online:");
                event.getPlayer().sendMessage("§7(None)");
            }
        }
    }

    private void degradeInventory(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            Material type = item.getType();
            if (type == Material.DIAMOND) item.setType(Material.CLAY_BRICK);
            else if (type == Material.DIAMOND_HELMET) item.setType(Material.LEATHER_HELMET);
            else if (type == Material.DIAMOND_CHESTPLATE) item.setType(Material.LEATHER_CHESTPLATE);
            else if (type == Material.DIAMOND_LEGGINGS) item.setType(Material.LEATHER_LEGGINGS);
            else if (type == Material.DIAMOND_BOOTS) item.setType(Material.LEATHER_BOOTS);
            else if (type == Material.DIAMOND_SWORD) item.setType(Material.WOOD_SWORD);
            else if (type == Material.DIAMOND_PICKAXE) item.setType(Material.WOOD_PICKAXE);
            else if (type == Material.DIAMOND_AXE) item.setType(Material.WOOD_AXE);
            else if (type == Material.DIAMOND_SPADE) item.setType(Material.WOOD_SPADE);
        }
        p.getInventory().setContents(contents);
        p.updateInventory();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (UUID uuid : tormented) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.hidePlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (tormented.contains(event.getPlayer().getUniqueId())) {
            event.getRecipients().clear();
            event.getRecipients().add(event.getPlayer());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (tormented.contains(event.getPlayer().getUniqueId())) {
            Material type = event.getBlock().getType();
            if (type == Material.DIAMOND_ORE || type == Material.GOLD_ORE || type == Material.IRON_ORE) {
                event.setExpToDrop(0);
                event.getBlock().setType(Material.AIR);
                event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.COBBLESTONE));
                event.setCancelled(true);
            }
        }
    }
}