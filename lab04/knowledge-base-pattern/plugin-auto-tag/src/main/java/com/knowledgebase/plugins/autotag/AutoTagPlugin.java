package com.knowledgebase.plugins.autotag;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;
import com.knowledgebase.model.Tag;
import com.knowledgebase.plugin.Plugin;
import com.knowledgebase.plugin.PluginContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

public class AutoTagPlugin implements Plugin {

    @Override
    public String getName() {
        return "Автотегирование";
    }

    @Override
    public List<Action> getActions(PluginContext context) {
        return List.of(new AbstractAction("Автотегировать выделенные заметки") {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Note> selectedNotes = context.getSelectedNotes();
                if (selectedNotes.isEmpty()) {
                    JOptionPane.showMessageDialog(context.getMainWindow(),
                            "Выделите хотя бы одну заметку.");
                    return;
                }
                DatabaseManager db = context.getDatabaseManager();
                // Получаем все существующие теги
                List<Tag> existingTags = db.getAllTags();
                int tagsAdded = 0;
                for (Note note : selectedNotes) {
                    String text = (note.getTitle() + " " + note.getContent()).toLowerCase();
                    for (Tag tag : existingTags) {
                        // Проверяем, содержит ли текст имя тега (без учёта регистра)
                        if (text.contains(tag.getName().toLowerCase())) {
                            db.addTagToNote(note.getId(), tag.getId());
                            tagsAdded++;
                        }
                    }
                }
                context.refreshNotesList();
                JOptionPane.showMessageDialog(context.getMainWindow(),
                        "Проставлено тегов: " + tagsAdded);
            }
        });
    }
}