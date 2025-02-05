package dev.aikido;

import dev.aikido.models.Pet;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class DatabaseHelper {
    // We can create a method to create and return a DataSource for our Postgres DB
    private static PGSimpleDataSource createDataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null) {
            throw new RuntimeException("DATABASE_URL environment variable is required");
        }
        URI databaseUri = URI.create(databaseUrl);

        // Create jdbc url
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://%s:%s%s?sslmode=disable".formatted(databaseUri.getHost(), databaseUri.getPort(), databaseUri.getPath()));
        dataSource.setUser(databaseUri.getUserInfo().split(":")[0]);
        dataSource.setPassword(databaseUri.getUserInfo().split(":")[1]);
        dataSource.setSsl(false);
        return dataSource;
    }
    public static void clearAll() throws SQLException {
        DataSource db = createDataSource();
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = db.getConnection();
            stmt = conn.prepareStatement("DELETE FROM pets");
            int rowsAffected = stmt.executeUpdate();
            System.out.println(rowsAffected + " pets have been removed from the database.");
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
        } finally {
            // Close resources in the reverse order of their creation
            stmt.close();
            conn.close();
        }
    }
    private static final String REGEX = "^[A-z0-9 ]+$";
    private static boolean isValidInput(String input) {
        // Compile the regex pattern
        Pattern pattern = Pattern.compile(REGEX);
        // Check if the input matches the pattern
        return !pattern.matcher(input).matches();
    }
    public static ArrayList<Object> getAllPets() {
        ArrayList<Object> pets = new ArrayList<>();
        DataSource db = createDataSource();
        try {
            Connection conn = db.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM pets");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Integer id = rs.getInt("pet_id");
                String name = rs.getString("pet_name");
                if (isValidInput(name)) {
                    name = "[REDACTED: XSS RISK]";
                }
                String owner = rs.getString("owner");
                if (isValidInput(owner)) {
                    owner = "[REDACTED: XSS RISK]";
                }
                pets.add(new Pet(id, name, owner));
            }
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
        }
        return pets;
    }
    public static Pet getPetById(Integer id) {
        ArrayList<Object> pets = new ArrayList<>();
        DataSource db = createDataSource();
        try {
            Connection conn = db.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM pets WHERE pet_id=?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Integer pet_id = rs.getInt("pet_id");
                String name = rs.getString("pet_name");
                String owner = rs.getString("owner");
                return new Pet(pet_id, name, owner);
            }
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
        }
        return new Pet(0, "Unknown", "Unknown");
    }
    public static Integer createPetByName(String pet_name) {
        String sql = "INSERT INTO pets (pet_name, owner) VALUES ('" + pet_name  + "', 'Aikido Security')";
        DataSource db = createDataSource();
        try {
            Connection conn = db.getConnection();
            PreparedStatement insertStmt = conn.prepareStatement(sql);
            return insertStmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
        }
        return 0;
    }
}
