package com.knowledgebase.plugin;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;

import javax.swing.JFrame;
import java.util.List;

/**
 * Контекст, который ядро передаёт плагину, чтобы тот мог взаимодействовать с данными и UI.
 */
public interface PluginContext {
    DatabaseManager getDatabaseManager();
    JFrame getMainWindow();          // чтобы плагин мог показать диалоги относительно главного окна
    List<Note> getSelectedNotes();   // текущие выделенные заметки в GUI
    void refreshNotesList();         // попросить GUI обновить список заметок
}