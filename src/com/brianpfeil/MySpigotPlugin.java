package com.brianpfeil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Logger;

/**
 * Created by pfeilbr on 8/31/15.
 */
public class MySpigotPlugin extends JavaPlugin implements Listener {

    public class ReloadOnPluginChanges implements Runnable {
        private Path path;
        private Plugin plugin;
        private Logger log;

        public ReloadOnPluginChanges(Path path, Plugin plugin) {
            this.path = path;
            this.plugin = plugin;
            this.log = plugin.getLogger();
        }

        public void run() {
            watchAndReloadIfFileChanges(this.path);
        }

        public void watchAndReloadIfFileChanges(Path dir) {

            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

                log.info("reloader watching for changes to .jar file in " + dir.toAbsolutePath());

                while (true) {
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException ex) {
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();

                        Boolean isJar = fileName.toString().endsWith(".jar");

                        if (isJar && (kind == ENTRY_MODIFY || kind == ENTRY_CREATE || kind == ENTRY_DELETE)) {
                            log.info("change detected for " + fileName + ". reloading");
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "reload");
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }

            } catch (IOException ex) {
                System.err.println(ex);
            }
        }

    }

    public Logger log = this.getLogger();

    @Override
    public void onEnable() {
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(this, this);

        // reload the server if there are any changes to a .jar file in the plugins/ directory
        File dataFolder = this.getDataFolder();
        watchAndReloadIfFileChanges(dataFolder.toPath().getParent());
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killedE = event.getEntity();
        if (killedE instanceof Player) {
            Player killed = (Player) killedE;
            String dead = killed.getDisplayName();
            //String killer = killed.getKiller().getDisplayName();
            Bukkit.broadcastMessage(ChatColor.GOLD + " " + dead + " " + ChatColor.GREEN + "was killed");
            //killed.kickPlayer("You have been killed and you cannot rejoin untill the current game is over.");
        }
    }

    @EventHandler
    public void onEvent(BlockBreakEvent e) {

    }

    public void watchAndReloadIfFileChanges(Path dir) {
        Thread t = new Thread(new ReloadOnPluginChanges(dir, this));
        t.start();
    }

}
