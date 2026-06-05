package com.knowledgebase.db;

import com.knowledgebase.model.Note;
import com.knowledgebase.model.Tag;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        initTables();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(500) NOT NULL,
                    content TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                );
                CREATE TABLE IF NOT EXISTS tags (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(100) UNIQUE NOT NULL
                );
                CREATE TABLE IF NOT EXISTS note_tags (
                    note_id VARCHAR(36) REFERENCES notes(id) ON DELETE CASCADE,
                    tag_id VARCHAR(36) REFERENCES tags(id) ON DELETE CASCADE,
                    PRIMARY KEY (note_id, tag_id)
                );
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot initialize database", e);
        }
    }

    // CRUD Заметок
    public void saveNote(Note note) {
        String sql = "INSERT INTO notes (id, title, content, created_at, updated_at) VALUES (?,?,?,?,?) " +
                     "ON CONFLICT (id) DO UPDATE SET title=?, content=?, updated_at=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(note.getCreatedAt()));
            ps.setTimestamp(5, Timestamp.valueOf(note.getUpdatedAt()));
            ps.setString(6, note.getTitle());
            ps.setString(7, note.getContent());
            ps.setTimestamp(8, Timestamp.valueOf(note.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save note", e);
        }
    }

    public void deleteNote(String noteId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM notes WHERE id=?")) {
            ps.setString(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete note", e);
        }
    }

    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM notes ORDER BY updated_at DESC");
            while (rs.next()) {
                notes.add(new Note(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notes", e);
        }
        return notes;
    }

    public List<Note> searchNotes(String query) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM notes WHERE title ILIKE ? OR content ILIKE ? ORDER BY updated_at DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notes.add(new Note(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search failed", e);
        }
        return notes;
    }

    // Теги
    public Tag createTag(String name) {
        String id = java.util.UUID.randomUUID().toString();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tags (id, name) VALUES (?, ?) ON CONFLICT (name) DO NOTHING")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tag", e);
        }
        return new Tag(id, name);
    }

    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM tags ORDER BY name");
            while (rs.next()) {
                tags.add(new Tag(rs.getString("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch tags", e);
        }
        return tags;
    }

    public void addTagToNote(String noteId, String tagId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO note_tags (note_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            ps.setString(1, noteId);
            ps.setString(2, tagId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add tag to note", e);
        }
    }

    public void removeTagFromNote(String noteId, String tagId) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM note_tags WHERE note_id=? AND tag_id=?")) {
            ps.setString(1, noteId);
            ps.setString(2, tagId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove tag from note", e);
        }
    }

    public List<Tag> getTagsForNote(String noteId) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT t.* FROM tags t JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id=?")) {
            ps.setString(1, noteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(new Tag(rs.getString("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tags for note", e);
        }
        return tags;
    }
    public List<Note> getNotesByTag(String tagId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.* FROM notes n " +
                    "JOIN note_tags nt ON n.id = nt.note_id " +
                    "WHERE nt.tag_id = ? " +
                    "ORDER BY n.updated_at DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tagId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                notes.add(new Note(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get notes by tag", e);
        }
        return notes;
    }

    public void deleteTag(String tagId) {
        // Удаляем связи с заметками
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM note_tags WHERE tag_id=?")) {
            ps.setString(1, tagId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove tag from notes", e);
        }
        // Удаляем сам тег
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM tags WHERE id=?")) {
            ps.setString(1, tagId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete tag", e);
        }
    }
}