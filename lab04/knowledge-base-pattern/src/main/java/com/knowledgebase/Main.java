package com.knowledgebase;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.gui.MainFrame;
import com.knowledgebase.plugin.Plugin;
import com.knowledgebase.plugin.PluginLoader;

import javax.swing.*;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream("config.properties"));
                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");

                DatabaseManager dbManager = new DatabaseManager(url, user, password);

                // Загрузка плагинов из папки plugins
                Path pluginsDir = Paths.get("plugins");
                PluginLoader loader = new PluginLoader();
                List<Plugin> plugins = loader.loadPlugins(pluginsDir);

                System.out.println("Загружено плагинов: " + plugins.size());

                MainFrame frame = new MainFrame(dbManager, plugins);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Ошибка запуска: " + e.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}