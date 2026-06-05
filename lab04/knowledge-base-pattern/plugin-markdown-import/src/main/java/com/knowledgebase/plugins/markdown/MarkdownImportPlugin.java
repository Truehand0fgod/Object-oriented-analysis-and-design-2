package com.knowledgebase.plugins.markdown;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;
import com.knowledgebase.plugin.Plugin;
import com.knowledgebase.plugin.PluginContext;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MarkdownImportPlugin implements Plugin {

    @Override
    public String getName() {
        return "Импорт Markdown";
    }

    @Override
    public List<Action> getActions(PluginContext context) {
        List<Action> actions = new ArrayList<>();
        actions.add(new AbstractAction("Импортировать .md файлы из папки") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(context.getMainWindow()) == JFileChooser.APPROVE_OPTION) {
                    File dir = chooser.getSelectedFile();
                    File[] mdFiles = dir.listFiles(f -> f.getName().endsWith(".md"));
                    if (mdFiles == null || mdFiles.length == 0) {
                        JOptionPane.showMessageDialog(context.getMainWindow(), "Нет .md файлов.");
                        return;
                    }
                    Parser parser = Parser.builder().build();
                    DatabaseManager db = context.getDatabaseManager();
                    int count = 0;
                    for (File md : mdFiles) {
                        try {
                            String content = Files.readString(md.toPath());
                            Node document = parser.parse(content);
                            // Используем массив для возможности изменения внутри анонимного класса
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
                            note.setContent(content);  // храним оригинальный markdown
                            note.setCreatedAt(LocalDateTime.now());
                            note.setUpdatedAt(LocalDateTime.now());
                            db.saveNote(note);
                            count++;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    context.refreshNotesList();
                    JOptionPane.showMessageDialog(context.getMainWindow(), "Импортировано заметок: " + count);
                }
            }
        });
        return actions;
    }
}