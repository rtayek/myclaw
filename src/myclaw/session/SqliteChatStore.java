package myclaw.session;

import myclaw.domain.ChatData;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQLite store for ChatData metadata and project records.
 */
public final class SqliteChatStore implements AutoCloseable {
    private final Connection connection;

    public SqliteChatStore(Path databasePath) {
        Objects.requireNonNull(databasePath, "databasePath");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            connection.setAutoCommit(true);
            initializeSchema();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not open chat store: " + databasePath, exception);
        }
    }

    public synchronized void initializeSchema() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS projects (
                        project_name TEXT PRIMARY KEY,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS chats (
                        id TEXT PRIMARY KEY,
                        project_name TEXT NOT NULL,
                        chat_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        web_url TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        last_active TEXT NOT NULL,
                        FOREIGN KEY (project_name) REFERENCES projects(project_name)
                    )
                    """);
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not initialize chat store schema", exception);
        }
    }

    public synchronized void saveProject(String projectName) {
        Objects.requireNonNull(projectName, "projectName");
        if (projectName.isBlank()) return;
        String now = Instant.now().toString();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO projects(project_name, created_at, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(project_name) DO UPDATE SET updated_at = excluded.updated_at
                """)) {
            ps.setString(1, projectName);
            ps.setString(2, now);
            ps.setString(3, now);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not save project: " + projectName, exception);
        }
    }

    public synchronized List<String> listProjects() {
        List<String> projects = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT project_name FROM projects ORDER BY project_name ASC")) {
            while (rs.next()) {
                projects.add(rs.getString("project_name"));
            }
            return projects;
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not list projects", exception);
        }
    }

    public synchronized void saveChat(ChatData chat) {
        Objects.requireNonNull(chat, "chat");
        if (chat.projectName() != null && !chat.projectName().isBlank()) {
            saveProject(chat.projectName());
        }
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO chats(id, project_name, chat_id, title, web_url, provider, last_active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    project_name = excluded.project_name,
                    chat_id = excluded.chat_id,
                    title = excluded.title,
                    web_url = excluded.web_url,
                    provider = excluded.provider,
                    last_active = excluded.last_active
                """)) {
            ps.setString(1, chat.id());
            ps.setString(2, chat.projectName());
            ps.setString(3, chat.chatId());
            ps.setString(4, chat.title());
            ps.setString(5, chat.webUrl());
            ps.setString(6, chat.provider());
            ps.setString(7, chat.lastActive());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not save chat: " + chat.id(), exception);
        }
    }

    public synchronized void saveChats(List<ChatData> chats) {
        if (chats == null || chats.isEmpty()) return;
        for (ChatData chat : chats) {
            saveChat(chat);
        }
    }

    public synchronized Optional<ChatData> getChat(String id) {
        Objects.requireNonNull(id, "id");
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, project_name, chat_id, title, web_url, provider, last_active
                FROM chats WHERE id = ?
                """)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChatData(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not get chat: " + id, exception);
        }
    }

    public synchronized List<ChatData> listAllChats() {
        List<ChatData> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("""
                SELECT id, project_name, chat_id, title, web_url, provider, last_active
                FROM chats ORDER BY title ASC
                """)) {
            while (rs.next()) {
                list.add(mapResultSetToChatData(rs));
            }
            return list;
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not list all chats", exception);
        }
    }

    public synchronized List<ChatData> listChatsByProject(String projectName) {
        Objects.requireNonNull(projectName, "projectName");
        List<ChatData> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, project_name, chat_id, title, web_url, provider, last_active
                FROM chats WHERE project_name = ? ORDER BY title ASC
                """)) {
            ps.setString(1, projectName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChatData(rs));
                }
                return list;
            }
        } catch (SQLException exception) {
            throw new SessionStoreException("Could not list chats for project: " + projectName, exception);
        }
    }

    private static ChatData mapResultSetToChatData(ResultSet rs) throws SQLException {
        return new ChatData(
                rs.getString("id"),
                rs.getString("project_name"),
                rs.getString("chat_id"),
                rs.getString("title"),
                rs.getString("web_url"),
                rs.getString("provider"),
                rs.getString("last_active")
        );
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
