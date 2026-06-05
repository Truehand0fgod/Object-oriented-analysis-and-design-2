package com.knowledgebase.plugins.pdf;

import com.knowledgebase.db.DatabaseManager;
import com.knowledgebase.model.Note;
import com.knowledgebase.plugin.Plugin;
import com.knowledgebase.plugin.PluginContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfExportPlugin implements Plugin {

    @Override
    public String getName() {
        return "Экспорт в PDF";
    }

    @Override
    public List<Action> getActions(PluginContext context) {
        List<Action> actions = new ArrayList<>();
        actions.add(new AbstractAction("Экспортировать выделенную заметку в PDF") {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<Note> selected = context.getSelectedNotes();
                if (selected.isEmpty()) {
                    JOptionPane.showMessageDialog(context.getMainWindow(),
                            "Выберите заметку в списке.");
                    return;
                }
                Note note = selected.get(0);
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(note.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf"));
                if (chooser.showSaveDialog(context.getMainWindow()) == JFileChooser.APPROVE_OPTION) {
                    try {
                        exportToPdf(note, chooser.getSelectedFile());
                        JOptionPane.showMessageDialog(context.getMainWindow(), "PDF сохранён.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(context.getMainWindow(),
                                "Ошибка: " + ex.getMessage());
                    }
                }
            }
        });
        return actions;
    }

    private void exportToPdf(Note note, File file) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // Загрузка шрифта с поддержкой кириллицы из ресурсов самого плагина
            InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
            if (fontStream == null) {
                throw new RuntimeException("Шрифт не найден в ресурсах плагина");
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
            document.save(file);
        }
    }
}