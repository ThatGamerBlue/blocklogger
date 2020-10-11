package tech.dttp.block.logger.save.sql;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import tech.dttp.block.logger.LoggedEventType;
import tech.dttp.block.logger.util.PlayerUtils;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DbConn {
    private static Connection con = null;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault()); // year-month-day hour:minute:second timezone

    /**
     * Establishes a connection to the database. Must be called before anything else.
     * @param server the MinecraftServer instance
     */
    public static void connect(MinecraftServer server) {
        try {
            Class.forName("org.sqlite.JDBC");
            // Find the correct location for the database
            File databaseFile = new File(server.getSavePath(WorldSavePath.ROOT).toFile(), "interactions.bl");
            // JDBC will create the database for us if it doesn't exist
            con = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getPath().replace('\\', '/'));
            // Ensure the events table exists
            ensureTable("events", "(type STRING, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, dimension STRING NOT NULL, oldstate STRING, newstate STRING, player STRING, time INT, rolledbackat INT DEFAULT -1)");
            System.out.println("[BL] Connected to database");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a table in the database if it doesn't exist already. Wrapper for CREATE TABLE IF NOT EXISTS
     *
     * @param name table name
     * @param description table structure
     */
    private static void ensureTable(String name, String description) {
        if (con == null) {
            throw new IllegalStateException("Databse connection not initialized");
        }
        String sql = "CREATE TABLE IF NOT EXISTS " + name + " " + description + ";";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.execute();

            System.out.println("[BL] prepared table");
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Writes a block break into the database
     *
     * @param x x position
     * @param y y position
     * @param z z position
     * @param state block broken
     * @param player player responsible for breaking
     */
    public static void writeBreak(int x, int y, int z, BlockState state, PlayerEntity player) {
        if (con == null) {
            throw new IllegalStateException("Database connection not initialized");
        }
        try {
            String sql = "INSERT INTO events(type, x, y, z, dimension, oldstate, newstate, player, time) VALUES(?,?,?,?,?,?,?,?,?)";
            // Setup prepared statement
            PreparedStatement ps = con.prepareStatement(sql);
            // Set parameters
            ps.setString(1, LoggedEventType.BREAK.name());
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.setString(5, PlayerUtils.getPlayerDimension(player));
            ps.setString(6, state.toString());
            ps.setString(7, null);
            ps.setString(8, player.getUuidAsString());
            ps.setLong(9, Instant.now().getEpochSecond());
            // Save the data
            ps.execute();
            System.out.println("[BL] Saved data");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads events from the database, at x, y, z in dimension. Optionally filtered by event type.
     * @param x x position
     * @param y y position
     * @param z z position
     * @param dimension dimension
     * @param eventType if null, reads all events, otherwise only reads events of this type
     */
    public static void readEvents(int x, int y, int z, String dimension, LoggedEventType eventType) {
        if (con == null) {
            throw new IllegalStateException("Database connection not initialized");
        }
        PreparedStatement ps;
        ResultSet rs;
        try {
            String sql = "SELECT type,x,y,z,dimension,oldstate,newstate,player,time,rolledbackat FROM events WHERE x=? AND y=? AND z=? AND dimension=?";
            // Add type filtering
            if (eventType != null) {
                sql += " AND type=?";
            }
            // Setup prepared statement parameters
            ps = con.prepareStatement(sql);
            ps.setInt(1, x);
            ps.setInt(2, y);
            ps.setInt(3, z);
            ps.setString(4, dimension);
            if (eventType != null) {
                ps.setString(5, eventType.name());
            }
            rs = ps.executeQuery();
            // Build a string for all the events on this block
            StringBuilder sb = new StringBuilder();
            sb.append("----- BlockLogger -----\n");
            while (rs.next()) {
                sb.append(rs.getString("type"));
                sb.append(" Old: ").append(rs.getString("oldstate"));
                sb.append(" New: ").append(rs.getString("newstate"));
                sb.append(" Player: ").append(rs.getString("player"));
                sb.append(" At: ").append(timeFormatter.format(Instant.ofEpochSecond(rs.getLong("time"))));
                sb.append(" Rolled Back? ").append(rs.getLong("rolledbackat") >= 0);
                sb.append("\n");
            }
            System.out.println(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
