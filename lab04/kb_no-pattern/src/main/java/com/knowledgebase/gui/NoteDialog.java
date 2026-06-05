package com.knowledgebase.gui;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

public class NoteDialog extends JDialog {
    private JTextField titleField;
    private JTextArea contentArea;
    private boolean saved = false;
    private Note note;
    private DatabaseManager dbManager;
    private boolean isNew;

    public NoteDialog(Frame owner, Note note, DatabaseManager dbManager) {
        super(owner, note == null ? "Новая заметка" : "Редактирование", true);
        this.note = note;
        this.dbManager = dbManager;
        this.isNew = (note == null);

        if (this.note == null) {
            this.note = new Note();
            this.note.setCreatedAt(LocalDateTime.now());
            this.note.setUpdatedAt(LocalDateTime.now());
        }

        setSize(500, 400);
        initUI();
    }

    private void initUI() {
        titleField = new JTextField(note.getTitle(), 30);
        contentArea = new JTextArea(note.getContent(), 20, 40);
        contentArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(contentArea);

        JButton saveBtn = new JButton("Сохранить");
        JButton cancelBtn = new JButton("Отмена");

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Заголовок:"), BorderLayout.WEST);
        top.add(titleField, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(cancelBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);

        saveBtn.addActionListener(e -> save());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void save() {
        note.setTitle(titleField.getText());
        note.setContent(contentArea.getText());
        note.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            note.setCreatedAt(LocalDateTime.now());
        }
        dbManager.saveNote(note);
        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}