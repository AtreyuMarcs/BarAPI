package me.confuser.barapi;

import java.util.HashMap;

import me.confuser.barapi.nms.FakeDragon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Allows plugins to safely set a health bar message.
 * 
 * @author James Mortemore
 */

public class BarAPI extends JavaPlugin implements Listener {
	private static HashMap<String, FakeDragon> players = new HashMap<String, FakeDragon>();
	private static HashMap<String, Integer> timers = new HashMap<String, Integer>();

	private static BarAPI plugin;

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		getLogger().info("Loaded");

		plugin = this;

		/* Test
		plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

			@Override
			public void run() {
				for (Player player : plugin.getServer().getOnlinePlayers()) {
					BarAPI.setMessage(player, ChatColor.AQUA + "Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI", 10);
				}
			}

		}, 30L, 300L);*/
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PlayerLoggout(PlayerQuitEvent event) {
		quit(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerKick(PlayerKickEvent event) {
		quit(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent event) {
		handleTeleport(event.getPlayer(), event.getTo().clone());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerRespawnEvent event) {
		handleTeleport(event.getPlayer(), event.getRespawnLocation().clone());
	}

	private void handleTeleport(final Player player, final Location loc) {

		if (!hasBar(player))
			return;

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			@Override
			public void run() {
				FakeDragon oldDragon = getDragon(player, "");

				float health = oldDragon.health;
				String message = oldDragon.name;

				Util.sendPacket(player, getDragon(player, "").getDestroyPacket());

				players.remove(player.getName());

				FakeDragon dragon = addDragon(player, loc, message);
				dragon.health = health;

				sendDragon(dragon, player);
			}

		}, 2L);
	}

	private void quit(Player player) {
		removeBar(player);
	}

	public static void setMessage(Player player, String message) {
		FakeDragon dragon = getDragon(player, message);

		dragon.name = cleanMessage(message);
		dragon.health = FakeDragon.MAX_HEALTH;

		cancelTimer(player);

		sendDragon(dragon, player);

	}

	public static void setMessage(Player player, String message, float percent) {
		FakeDragon dragon = getDragon(player, message);

		dragon.name = cleanMessage(message);
		dragon.health = (percent / 100f) * FakeDragon.MAX_HEALTH;

		cancelTimer(player);

		sendDragon(dragon, player);
	}

	public static void setMessage(final Player player, String message, int seconds) {
		FakeDragon dragon = getDragon(player, message);

		dragon.name = cleanMessage(message);
		dragon.health = FakeDragon.MAX_HEALTH;

		final int dragonHealthMinus = FakeDragon.MAX_HEALTH / seconds;

		cancelTimer(player);

		timers.put(player.getName(), Bukkit.getScheduler().runTaskTimer(plugin, new BukkitRunnable() {

			@Override
			public void run() {
				FakeDragon drag = getDragon(player, "");
				drag.health -= dragonHealthMinus;

				if (drag.health <= 0) {
					removeBar(player);
					cancelTimer(player);
				} else {
					sendDragon(drag, player);
				}
			}

		}, 20L, 20L).getTaskId());

		sendDragon(dragon, player);
	}

	public static boolean hasBar(Player player) {
		return players.get(player.getName()) != null;
	}

	public static void removeBar(Player player) {
		if (!hasBar(player))
			return;

		Util.sendPacket(player, getDragon(player, "").getDestroyPacket());

		players.remove(player.getName());

		cancelTimer(player);
	}

	public static void setHealth(Player player, float percent) {
		if (!hasBar(player))
			return;

		FakeDragon dragon = getDragon(player, "");
		dragon.health = (percent / 100f) * FakeDragon.MAX_HEALTH;

		cancelTimer(player);

		sendDragon(dragon, player);
	}

	public static float getHealth(Player player) {
		if (!hasBar(player))
			return -1;

		return getDragon(player, "").health;
	}

	public static String getMessage(Player player) {
		if (!hasBar(player))
			return "";

		return getDragon(player, "").name;
	}

	private static String cleanMessage(String message) {
		if (message.length() > 64)
			message = message.substring(0, 63);

		return message;
	}

	private static void cancelTimer(Player player) {
		Integer timerID = timers.remove(player.getName());

		if (timerID != null) {
			Bukkit.getScheduler().cancelTask(timerID);
		}
	}

	private static void sendDragon(FakeDragon dragon, Player player) {
		Util.sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));
		Util.sendPacket(player, dragon.getTeleportPacket(player.getLocation().add(0, -200, 0)));
	}

	private static FakeDragon getDragon(Player player, String message) {
		if (hasBar(player)) {
			return players.get(player.getName());
		} else
			return addDragon(player, cleanMessage(message));
	}

	private static FakeDragon addDragon(Player player, String message) {
		FakeDragon dragon = Util.newDragon(message, player.getLocation().add(0, -200, 0));

		Util.sendPacket(player, dragon.getSpawnPacket());

		players.put(player.getName(), dragon);

		return dragon;
	}

	private static FakeDragon addDragon(Player player, Location loc, String message) {
		FakeDragon dragon = Util.newDragon(message, loc.add(0, -200, 0));

		Util.sendPacket(player, dragon.getSpawnPacket());

		players.put(player.getName(), dragon);

		return dragon;
	}
}
