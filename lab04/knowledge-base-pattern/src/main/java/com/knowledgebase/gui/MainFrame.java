package com.knowledgebase.gui;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;
import com.knowledgebase.model.Tag;
import com.knowledgebase.plugin.Plugin;
import com.knowledgebase.plugin.PluginContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements PluginContext {
    private DatabaseManager dbManager;
    private DefaultListModel<Note> noteListModel;
    private JList<Note> noteList;
    private List<Plugin> plugins = new ArrayList<>();

    public MainFrame(DatabaseManager dbManager, List<Plugin> plugins) {
        this.dbManager = dbManager;
        this.plugins = plugins != null ? plugins : new ArrayList<>();
        setTitle("Персональная база знаний");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        initUI();
    }

    private void initUI() {
        noteListModel = new DefaultListModel<>();
        noteList = new JList<>(noteListModel);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedNote();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(noteList);

        // Панель кнопок
        JButton newNoteBtn = new JButton("Новая заметка");
        JButton deleteBtn = new JButton("Удалить");
        JButton editTagsBtn = new JButton("Управление тегами");
        JTextField searchField = new JTextField(15);
        JButton searchBtn = new JButton("Поиск");
        JButton refreshBtn = new JButton("Обновить");

        JPanel topPanel = new JPanel();
        topPanel.add(newNoteBtn);
        topPanel.add(deleteBtn);
        topPanel.add(editTagsBtn);
        topPanel.add(new JLabel("Поиск:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);

        // Меню плагинов
        JMenuBar menuBar = new JMenuBar();
        JMenu pluginsMenu = new JMenu("Плагины");
        for (Plugin plugin : plugins) {
            List<Action> actions = plugin.getActions(this);
            if (actions.isEmpty()) continue;
            JMenu subMenu = new JMenu(plugin.getName());
            for (Action action : actions) {
                subMenu.add(action);
            }
            pluginsMenu.add(subMenu);
        }
        menuBar.add(pluginsMenu);
        setJMenuBar(menuBar);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Обработчики
        newNoteBtn.addActionListener(e -> createNewNote());
        deleteBtn.addActionListener(e -> deleteSelectedNote());
        editTagsBtn.addActionListener(e -> manageTags());
        searchBtn.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) refreshNotesList();
            else searchNotes(q);
        });
        refreshBtn.addActionListener(e -> refreshNotesList());

        refreshNotesList();
    }

    private void createNewNote() {
        NoteDialog dialog = new NoteDialog(this, null, dbManager);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshNotesList();
        }
    }

    private void editSelectedNote() {
        Note selected = noteList.getSelectedValue();
        if (selected == null) return;
        NoteDialog dialog = new NoteDialog(this, selected, dbManager);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshNotesList();
        }
    }

    private void deleteSelectedNote() {
        Note selected = noteList.getSelectedValue();
        if (selected == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить заметку «" + selected.getTitle() + "»?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dbManager.deleteNote(selected.getId());
            refreshNotesList();
        }
    }

    private void manageTags() {
        // Простое управление: создание тегов и привязка к выделенной заметке
        Note selected = noteList.getSelectedValue();
        JDialog dialog = new JDialog(this, "Управление тегами", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<Tag> allTagsModel = new DefaultListModel<>();
        JList<Tag> allTagsList = new JList<>(allTagsModel);
        List<Tag> allTags = dbManager.getAllTags();
        for (Tag t : allTags) allTagsModel.addElement(t);

        JButton addTagBtn = new JButton("Создать тег");
        JButton assignBtn = new JButton("Прикрепить к заметке");
        JButton removeBtn = new JButton("Открепить");
        JPanel btnPanel = new JPanel();
        btnPanel.add(addTagBtn);
        if (selected != null) {
            btnPanel.add(assignBtn);
            btnPanel.add(removeBtn);
        }

        dialog.add(new JScrollPane(allTagsList), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        addTagBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Название тега:");
            if (name != null && !name.isBlank()) {
                Tag tag = dbManager.createTag(name.trim());
                allTagsModel.addElement(tag);
            }
        });

        assignBtn.addActionListener(e -> {
            Tag selectedTag = allTagsList.getSelectedValue();
            if (selected != null && selectedTag != null) {
                dbManager.addTagToNote(selected.getId(), selectedTag.getId());
            }
        });

        removeBtn.addActionListener(e -> {
            Tag selectedTag = allTagsList.getSelectedValue();
            if (selected != null && selectedTag != null) {
                dbManager.removeTagFromNote(selected.getId(), selectedTag.getId());
            }
        });

        dialog.setVisible(true);
    }

    private void searchNotes(String query) {
        List<Note> notes = dbManager.searchNotes(query);
        noteListModel.clear();
        for (Note n : notes) noteListModel.addElement(n);
    }

    @Override
    public void refreshNotesList() {
        List<Note> notes = dbManager.getAllNotes();
        noteListModel.clear();
        for (Note n : notes) noteListModel.addElement(n);
    }

    // Методы PluginContext
    @Override
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    @Override
    public JFrame getMainWindow() {
        return this;
    }

    @Override
    public List<Note> getSelectedNotes() {
        List<Note> selected = new ArrayList<>();
        Note n = noteList.getSelectedValue();
        if (n != null) selected.add(n);
        return selected;
    }
}