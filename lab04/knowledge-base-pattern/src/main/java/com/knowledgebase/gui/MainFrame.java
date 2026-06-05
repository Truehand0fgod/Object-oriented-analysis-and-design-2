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
    private JComboBox<Tag> tagFilterCombo;   // добавим поле

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

        // Кнопки
        JButton newNoteBtn = new JButton("Новая заметка");
        JButton deleteBtn = new JButton("Удалить");
        JButton editTagsBtn = new JButton("Управление тегами");
        JTextField searchField = new JTextField(15);
        JButton searchBtn = new JButton("Поиск");
        JButton refreshBtn = new JButton("Обновить");

        // Комбобокс для фильтрации по тегу
        tagFilterCombo = new JComboBox<>();
        tagFilterCombo.setPrototypeDisplayValue(new Tag("", "Длинный тег для размера"));
        tagFilterCombo.addItem(null);
        JButton filterByTagBtn = new JButton("Фильтр");

        JPanel topPanel = new JPanel();
        topPanel.add(newNoteBtn);
        topPanel.add(deleteBtn);
        topPanel.add(editTagsBtn);
        topPanel.add(new JLabel("Поиск:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);
        topPanel.add(new JLabel("Тег:"));
        topPanel.add(tagFilterCombo);
        topPanel.add(filterByTagBtn);

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

        filterByTagBtn.addActionListener(e -> {
            Tag selectedTag = (Tag) tagFilterCombo.getSelectedItem();
            if (selectedTag == null) {
                refreshNotesList();
            } else {
                List<Note> filteredNotes = dbManager.getNotesByTag(selectedTag.getId());
                noteListModel.clear();
                for (Note n : filteredNotes) {
                    noteListModel.addElement(n);
                }
            }
        });

        refreshNotesList();  // первый вызов – обновит и комбобокс
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
        Note selected = noteList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Выберите заметку для управления тегами.");
            return;
        }

        JDialog dialog = new JDialog(this, "Управление тегами для заметки: " + selected.getTitle(), true);
        dialog.setSize(550, 400);
        dialog.setLayout(new BorderLayout(10, 10));

        // Модели списков
        DefaultListModel<Tag> allTagsModel = new DefaultListModel<>();
        DefaultListModel<Tag> noteTagsModel = new DefaultListModel<>();

        // Загружаем все теги
        List<Tag> allTags = dbManager.getAllTags();
        for (Tag t : allTags) allTagsModel.addElement(t);

        // Загружаем теги, привязанные к заметке
        List<Tag> noteTags = dbManager.getTagsForNote(selected.getId());
        for (Tag t : noteTags) noteTagsModel.addElement(t);

        // Компоненты списков
        JList<Tag> allTagsList = new JList<>(allTagsModel);
        JList<Tag> noteTagsList = new JList<>(noteTagsModel);

        JScrollPane leftScroll = new JScrollPane(allTagsList);
        JScrollPane rightScroll = new JScrollPane(noteTagsList);

        // Кнопки перемещения тегов
        JButton addBtn = new JButton("→");
        JButton removeBtn = new JButton("←");

        JPanel movePanel = new JPanel();
        movePanel.setLayout(new BoxLayout(movePanel, BoxLayout.Y_AXIS));
        movePanel.add(Box.createVerticalGlue());
        movePanel.add(addBtn);
        movePanel.add(Box.createVerticalStrut(10));
        movePanel.add(removeBtn);
        movePanel.add(Box.createVerticalGlue());

        // Кнопки создания и удаления тегов
        JButton createTagBtn = new JButton("Создать тег");
        JButton deleteTagBtn = new JButton("Удалить тег");

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(createTagBtn);
        controlPanel.add(deleteTagBtn);

        // Сборка диалога
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(new JLabel("Все теги"), BorderLayout.WEST);
        centerPanel.add(new JLabel("Теги заметки"), BorderLayout.EAST);
        centerPanel.add(leftScroll, BorderLayout.WEST);
        centerPanel.add(rightScroll, BorderLayout.EAST);
        centerPanel.add(movePanel, BorderLayout.CENTER);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.SOUTH);

        // Обработчики кнопок
        addBtn.addActionListener(e -> {
            Tag selectedTag = allTagsList.getSelectedValue();
            if (selectedTag != null) {
                dbManager.addTagToNote(selected.getId(), selectedTag.getId());
                // Обновляем правый список
                noteTagsModel.clear();
                for (Tag t : dbManager.getTagsForNote(selected.getId())) {
                    noteTagsModel.addElement(t);
                }
            }
        });

        removeBtn.addActionListener(e -> {
            Tag selectedTag = noteTagsList.getSelectedValue();
            if (selectedTag != null) {
                dbManager.removeTagFromNote(selected.getId(), selectedTag.getId());
                noteTagsModel.clear();
                for (Tag t : dbManager.getTagsForNote(selected.getId())) {
                    noteTagsModel.addElement(t);
                }
            }
        });

        createTagBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Название нового тега:");
            if (name != null && !name.isBlank()) {
                Tag newTag = dbManager.createTag(name.trim());
                allTagsModel.addElement(newTag);
            }
        });

        deleteTagBtn.addActionListener(e -> {
            Tag selectedTag = allTagsList.getSelectedValue();
            if (selectedTag == null) {
                JOptionPane.showMessageDialog(dialog, "Выберите тег для удаления из левого списка.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Удалить тег «" + selectedTag.getName() + "»?\n" +
                    "Он будет откреплён от всех заметок.",
                    "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dbManager.deleteTag(selectedTag.getId());
                allTagsModel.removeElement(selectedTag);
                // Удаляем также из правого списка, если был там
                noteTagsModel.removeElement(selectedTag);
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
        // Обновим список тегов в комбобоксе
        updateTagFilterCombo();
    }


    private void updateTagFilterCombo() {
        Tag selected = (Tag) tagFilterCombo.getSelectedItem();
        tagFilterCombo.removeAllItems();
        tagFilterCombo.addItem(null);   // пункт "Все заметки"
        for (Tag t : dbManager.getAllTags()) {
            tagFilterCombo.addItem(t);
        }
        // Восстановить предыдущий выбор, если тег ещё существует
        if (selected != null) {
            for (int i = 0; i < tagFilterCombo.getItemCount(); i++) {
                if (selected.equals(tagFilterCombo.getItemAt(i))) {
                    tagFilterCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
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