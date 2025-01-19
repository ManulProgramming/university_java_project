import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//An external class responsible for managing MySQL database
public class DB_manager {
    //Connection to the DB
    Connection connect() {
        String url = "jdbc:mysql://localhost:3306/upload_history"; //this is an address to the database that will be used
        String user = "YOUR_USER_HERE";
        String password = "YOUR_PASSWORD_HERE";
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    //Creating a table "history" with columns being (id PK, url, title, format, quality)
    public void createTable(Connection conn) {
        String sql = """
                CREATE TABLE IF NOT EXISTS history (
                    id int NOT NULL AUTO_INCREMENT,
                    url varchar(255),
                    title varchar(1024),
                    format varchar(255),
                    quality varchar(255),
                    PRIMARY KEY (id)
                );
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //Get all the data from the table "history"
    public List<List<String>> select_history(Connection conn) {
        String sql = "SELECT id,url,title,format,quality FROM history";
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            List<List<String>> history_data = new ArrayList<>();
            while (rs.next()) {
                String id = rs.getString("id");
                String url = rs.getString("url");
                String title = rs.getString("title");
                String format = rs.getString("format");
                String quality = rs.getString("quality");
                List<String> tmp = new ArrayList<>();
                tmp.add(id);
                tmp.add(url);
                tmp.add(title);
                tmp.add(format);
                tmp.add(quality);
                history_data.add(tmp);
            }
            return history_data;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
    //Adding new entries to the history
    public void insert_history(Connection conn, List<String> data) {
        if (data.get(1).equalsIgnoreCase("mp4") || data.get(1).equalsIgnoreCase("webm") || data.get(1).equalsIgnoreCase("mp3")) {
            String sql = "INSERT INTO history (url, title, format, quality) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, data.get(0));
                pstmt.setString(2, data.get(3));
                pstmt.setString(3, data.get(1));
                pstmt.setString(4, data.get(2));
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    //Drops (deletes) the whole history table, basically deletes all the previous entries
    public void drop_history(Connection conn) {
        String sql = "DROP TABLE history CASCADE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
