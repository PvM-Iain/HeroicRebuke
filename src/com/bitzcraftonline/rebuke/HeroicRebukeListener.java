package com.bitzcraftonline.rebuke;

import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class HeroicRebukeListener
  implements Listener
{
  private final HeroicRebuke plugin;
  public static HashMap<Player, Location> rootLocations = new HashMap();
  private static HashMap<Player, Long> nextinform = new HashMap();

  public HeroicRebukeListener(HeroicRebuke instance) {
    this.plugin = instance;
  }

  public void rootPlayer(Player p) {
    rootLocations.put(p, p.getLocation());
  }

  @EventHandler(priority=EventPriority.LOW)
  public void onPlayerMove(PlayerMoveEvent event) {
    if ((this.plugin.blockMove) && 
      (rootLocations.containsKey(event.getPlayer()))) {
      Location from = (Location)rootLocations.get(event.getPlayer());
      if (event.getTo() != from) {
        event.setTo(from);
        warnMove(event.getPlayer());
      }
    }
  }

  @EventHandler(priority=EventPriority.LOW)
  public void onPlayerTeleport(PlayerTeleportEvent event)
  {
    if ((this.plugin.blockMove) && 
      (rootLocations.containsKey(event.getPlayer()))) {
      Location from = (Location)rootLocations.get(event.getPlayer());
      double deltaX = Math.abs(from.getX() - event.getTo().getX());
      double deltaY = Math.abs(from.getY() - event.getTo().getY());
      double deltaZ = Math.abs(from.getZ() - event.getTo().getZ());
      if ((deltaX > 1.5D) || (deltaY > 1.5D) || (deltaZ > 1.5D)) {
        HeroicRebuke.debug("From: " + from.toString() + " To: " + event.getTo().toString());
        event.setTo(from);
        warnMove(event.getPlayer());
      }
    }
  }

  @EventHandler(priority=EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Player p = event.getPlayer();
    if (HeroicRebuke.warnings.containsKey(p.getName().toLowerCase())) {
      if (!rootLocations.containsKey(p)) {
        rootPlayer(p);
      }
      this.plugin.sendWarning(p, (Warning)HeroicRebuke.warnings.get(p.getName().toLowerCase()));
    }
  }

  @EventHandler(priority=EventPriority.LOW)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if ((this.plugin.blockMove) && 
      (rootLocations.containsKey(event.getPlayer()))) {
      event.setCancelled(true);
      warnMove(event.getPlayer());
    }
  }

  private void warnMove(Player player)
  {
    if (nextinform.containsKey(player)) {
      if (System.currentTimeMillis() < ((Long)nextinform.get(player)).longValue() * 1000L) {
        return;
      }
      nextinform.remove(player);
    }

    nextinform.put(player, Long.valueOf(System.currentTimeMillis() / 1000L + 15L));
    player.sendMessage("Movement disabled! Say /warn list");
  }
}