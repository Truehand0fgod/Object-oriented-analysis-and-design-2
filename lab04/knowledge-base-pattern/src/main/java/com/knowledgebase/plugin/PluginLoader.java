package com.knowledgebase.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class PluginLoader {

    public List<Plugin> loadPlugins(Path pluginsDir) {
        List<Plugin> plugins = new ArrayList<>();
        File dir = pluginsDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) return plugins;

        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null) return plugins;

        for (File jarFile : jarFiles) {
            try {
                URL jarUrl = jarFile.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
                ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
                for (Plugin plugin : serviceLoader) {
                    plugins.add(plugin);
                }
            } catch (Exception e) {
                System.err.println("Failed to load plugin from " + jarFile.getName() + ": " + e.getMessage());
            }
        }
        return plugins;
    }
}