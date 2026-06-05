package com.knowledgebase;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.gui.MainFrame;

import javax.swing.*;
import java.io.FileInputStream;
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
                MainFrame frame = new MainFrame(dbManager);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка запуска: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}