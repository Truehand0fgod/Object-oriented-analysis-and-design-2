package com.knowledgebase.gui;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;
import com.knowledgebase.model.Tag;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private DatabaseManager dbManager;
    private DefaultListModel<Note> noteListModel;
    private JList<Note> noteList;
    private JComboBox<Tag> tagFilterCombo;

    public MainFrame(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setTitle("Персональная база знаний (Монолит)");
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

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(newNoteBtn);
        buttonsPanel.add(deleteBtn);
        buttonsPanel.add(editTagsBtn);
        buttonsPanel.add(new JLabel("Поиск:"));
        buttonsPanel.add(searchField);
        buttonsPanel.add(searchBtn);
        buttonsPanel.add(refreshBtn);

        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Тег:"));
        filterPanel.add(tagFilterCombo);
        filterPanel.add(filterByTagBtn);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(buttonsPanel);
        topPanel.add(filterPanel);

        // Меню со встроенными функциями вместо плагинов
        JMenuBar menuBar = new JMenuBar();
        JMenu functionsMenu = new JMenu("Функции");

        JMenuItem exportPdfItem = new JMenuItem("Экспортировать выделенную заметку в PDF");
        exportPdfItem.addActionListener(e -> exportToPdf());
        functionsMenu.add(exportPdfItem);

        JMenuItem importMdItem = new JMenuItem("Импортировать .md файлы из папки");
        importMdItem.addActionListener(e -> importFromMarkdown());
        functionsMenu.add(importMdItem);

        JMenuItem autoTagItem = new JMenuItem("Автотегировать выделенные заметки");
        autoTagItem.addActionListener(e -> autoTagSelected());
        functionsMenu.add(autoTagItem);

        menuBar.add(functionsMenu);
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

        refreshNotesList();
    }

    // === ВСТРОЕННАЯ ФУНКЦИЯ: Экспорт в PDF ===
    private void exportToPdf() {
        Note note = noteList.getSelectedValue();
        if (note == null) {
            JOptionPane.showMessageDialog(this, "Выберите заметку.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(note.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                try (PDDocument document = new PDDocument()) {
                    PDPage page = new PDPage();
                    document.addPage(page);
                    InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
                    if (fontStream == null) {
                        throw new RuntimeException("Шрифт не найден");
                    }
                    PDType0Font font = PDType0Font.load(document, fontStream);
                    try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                        cs.beginText();
                        cs.setFont(font, 14);
                        cs.newLineAtOffset(50, 750);
                        cs.showText(note.getTitle());
                        cs.endText();
                        cs.beginText();
                        cs.setFont(font, 10);
                        cs.newLineAtOffset(50, 720);
                        String text = note.getContent();
                        if (text != null) {
                            for (String line : text.split("\n")) {
                                cs.showText(line);
                                cs.newLineAtOffset(0, -12);
                            }
                        }
                        cs.endText();
                    }
                    document.save(chooser.getSelectedFile());
                }
                JOptionPane.showMessageDialog(this, "PDF сохранён.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            }
        }
    }

    // === ВСТРОЕННАЯ ФУНКЦИЯ: Импорт Markdown ===
    private void importFromMarkdown() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            File[] mdFiles = dir.listFiles(f -> f.getName().endsWith(".md"));
            if (mdFiles == null || mdFiles.length == 0) {
                JOptionPane.showMessageDialog(this, "Нет .md файлов.");
                return;
            }
            Parser parser = Parser.builder().build();
            int count = 0;
            for (File md : mdFiles) {
                try {
                    String content = Files.readString(md.toPath());
                    Node document = parser.parse(content);
                    final String[] titleHolder = {md.getName().replace(".md", "")};
                    AbstractVisitor visitor = new AbstractVisitor() {
                        @Override
                        public void visit(Heading heading) {
                            if (heading.getLevel() == 1 && titleHolder[0].equals(md.getName().replace(".md", ""))) {
                                Node firstChild = heading.getFirstChild();
                                if (firstChild instanceof Text) {
                                    titleHolder[0] = ((Text) firstChild).getLiteral();
                                }
                            }
                        }
                    };
                    document.accept(visitor);
                    Note note = new Note();
                    note.setTitle(titleHolder[0]);
                    note.setContent(content);
                    note.setCreatedAt(LocalDateTime.now());
                    note.setUpdatedAt(LocalDateTime.now());
                    dbManager.saveNote(note);
                    count++;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            refreshNotesList();
            JOptionPane.showMessageDialog(this, "Импортировано заметок: " + count);
        }
    }

    // === ВСТРОЕННАЯ ФУНКЦИЯ: Автотегирование ===
    private void autoTagSelected() {
        List<Note> selectedNotes = getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Выделите хотя бы одну заметку.");
            return;
        }
        List<Tag> existingTags = dbManager.getAllTags();
        int tagsAdded = 0;
        for (Note note : selectedNotes) {
            String text = (note.getTitle() + " " + note.getContent()).toLowerCase();
            for (Tag tag : existingTags) {
                if (text.contains(tag.getName().toLowerCase())) {
                    dbManager.addTagToNote(note.getId(), tag.getId());
                    tagsAdded++;
                }
            }
        }
        refreshNotesList();
        JOptionPane.showMessageDialog(this, "Проставлено тегов: " + tagsAdded);
    }

    private List<Note> getSelectedNotes() {
        List<Note> selected = new ArrayList<>();
        Note n = noteList.getSelectedValue();
        if (n != null) selected.add(n);
        return selected;
    }

    // === Основные операции с заметками ===

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

        DefaultListModel<Tag> allTagsModel = new DefaultListModel<>();
        DefaultListModel<Tag> noteTagsModel = new DefaultListModel<>();

        List<Tag> allTags = dbManager.getAllTags();
        for (Tag t : allTags) allTagsModel.addElement(t);

        List<Tag> noteTags = dbManager.getTagsForNote(selected.getId());
        for (Tag t : noteTags) noteTagsModel.addElement(t);

        JList<Tag> allTagsList = new JList<>(allTagsModel);
        JList<Tag> noteTagsList = new JList<>(noteTagsModel);

        JScrollPane leftScroll = new JScrollPane(allTagsList);
        JScrollPane rightScroll = new JScrollPane(noteTagsList);

        JButton addBtn = new JButton("→");
        JButton removeBtn = new JButton("←");

        JPanel movePanel = new JPanel();
        movePanel.setLayout(new BoxLayout(movePanel, BoxLayout.Y_AXIS));
        movePanel.add(Box.createVerticalGlue());
        movePanel.add(addBtn);
        movePanel.add(Box.createVerticalStrut(10));
        movePanel.add(removeBtn);
        movePanel.add(Box.createVerticalGlue());

        JButton createTagBtn = new JButton("Создать тег");
        JButton deleteTagBtn = new JButton("Удалить тег");

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(createTagBtn);
        controlPanel.add(deleteTagBtn);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.add(new JLabel("Все теги"), BorderLayout.WEST);
        centerPanel.add(new JLabel("Теги заметки"), BorderLayout.EAST);
        centerPanel.add(leftScroll, BorderLayout.WEST);
        centerPanel.add(rightScroll, BorderLayout.EAST);
        centerPanel.add(movePanel, BorderLayout.CENTER);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            Tag selectedTag = allTagsList.getSelectedValue();
            if (selectedTag != null) {
                dbManager.addTagToNote(selected.getId(), selectedTag.getId());
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

    private void refreshNotesList() {
        List<Note> notes = dbManager.getAllNotes();
        noteListModel.clear();
        for (Note n : notes) noteListModel.addElement(n);
        updateTagFilterCombo();
    }

    private void updateTagFilterCombo() {
        Tag selected = (Tag) tagFilterCombo.getSelectedItem();
        tagFilterCombo.removeAllItems();
        tagFilterCombo.addItem(null);
        for (Tag t : dbManager.getAllTags()) {
            tagFilterCombo.addItem(t);
        }
        if (selected != null) {
            for (int i = 0; i < tagFilterCombo.getItemCount(); i++) {
                if (selected.equals(tagFilterCombo.getItemAt(i))) {
                    tagFilterCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
}