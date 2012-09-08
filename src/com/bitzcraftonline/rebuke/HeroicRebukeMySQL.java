package com.bitzcraftonline.rebuke;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public class HeroicRebukeMySQL extends HeroicRebukeDatasource
{
  public HeroicRebukeMySQL(HeroicRebuke instance)
  {
    plugin = instance;
    connection = getConnection();
  }

  protected Connection createConnection()
  {
    try {
      Class.forName("com.mysql.jdbc.Driver");
      Enumeration e = DriverManager.getDrivers();

      while (e.hasMoreElements()) {
        Driver d = (Driver)e.nextElement();
        if (d.getClass().getName().toLowerCase().indexOf("mysql") != -1) {
          Properties prop = new Properties();
          prop.setProperty("user", plugin.mySqlUser);
          prop.setProperty("password", plugin.mySqlPass);
          Connection conn = d.connect(new StringBuilder().append("jdbc:mysql://").append(plugin.mySqlDir).append(plugin.mySqlDir.contains("?") ? "&" : "?").append("zeroDateTimeBehavior=convertToNull&autoReconnect=true").toString(), prop);
          conn.setAutoCommit(false);
          return conn;
        }
      }
    }
    catch (ClassNotFoundException e) {
      HeroicRebuke.log.severe("[HeroicRebuke] Connector for MySQL not found! Is 'mysql-connector-java-bin.jar' in /lib?");
    } catch (SQLException e) {
      HeroicRebuke.log.log(Level.SEVERE, MessageFormat.format("[HeroicRebuke] Error connecting to MySQL Database: {0}", new Object[] { e.getMessage() }));
    }
    return null;
  }

  public void initDB()
  {
    try {
      Connection conn = getConnection();
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `warnings` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,`to` VARCHAR(32) NOT NULL,`from` VARCHAR(32) NOT NULL,`message` VARCHAR(255) NOT NULL,`ack` BOOLEAN NOT NULL DEFAULT '0',`send_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,`ack_time` TIMESTAMP,`code` TEXT, INDEX `warned` (`to`)) ENGINE = MYISAM;");
      conn.commit();
      loadWarnings();
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Table creation error: {1}", new Object[] { plugin.name, e }));
    }
  }

  public int newWarning(Warning w)
  {
    int index = -1;
    try {
      Connection conn = getConnection();
      conn.setAutoCommit(false);
      PreparedStatement ps = conn.prepareStatement("INSERT INTO `warnings` (`to`, `from`, `message`, `code`) VALUES (?,?,?,?)", 1);
      ps.setString(1, w.getTarget());
      ps.setString(2, w.getSender());
      ps.setString(3, w.getMessage());
      ps.setString(4, w.getCode());
      ps.executeUpdate();

      ResultSet rs = ps.getGeneratedKeys();
      if (rs.next()) {
        index = rs.getInt(1);
      }
      conn.commit();
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Warning creation error: {1}", new Object[] { plugin.name, e }));
    }
    return index;
  }

  public void loadWarnings()
  {
    try {
      Connection conn = getConnection();
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT `id`,`to`,`from`,`message`,`ack`,`send_time`,`ack_time`,`code` FROM `warnings` WHERE `ack` = '0' ORDER BY `id` ASC");
      int i = 0;
      while (rs.next()) {
        Timestamp send_ts = rs.getTimestamp(6);
        Timestamp ack_ts = rs.getTimestamp(7);
        Long send_time = Long.valueOf(0L);
        Long ack_time = Long.valueOf(0L);
        if (send_ts != null) {
          send_time = Long.valueOf(send_ts.getTime());
        }
        if (ack_ts != null) {
          ack_time = Long.valueOf(ack_ts.getTime());
        }
        Warning w = new Warning(Integer.valueOf(rs.getInt("id")), rs.getString("to"), rs.getString("from"), rs.getString("message"), rs.getBoolean("ack"), send_time, ack_time, rs.getString("code"));
        HeroicRebuke.warnings.put(rs.getString("to").toLowerCase(), w);
        Player p = plugin.getServer().getPlayer(rs.getString("to"));
        if ((p != null) && (!HeroicRebukeListener.rootLocations.containsKey(p))) {
          HeroicRebukeListener.rootLocations.put(p, p.getLocation());
        }
        HeroicRebuke.debug(new StringBuilder().append("Loaded Warning: ").append(w.toString()).toString());
        i++;
      }
      conn.commit();
      log.log(Level.INFO, MessageFormat.format("[{0}] Loaded {1} active warning{2}", new Object[] { plugin.name, Integer.valueOf(i), i == 1 ? "." : "s." }));
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Warning load error: {1}", new Object[] { plugin.name, e }));
    }
  }

  public Warning getWarning(int index)
  {
    Warning w = null;
    try {
      Connection conn = getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT `id`,`to`,`from`,`message`,`ack`,`send_time`,`ack_time`,`code` FROM `warnings` WHERE `id` = ?");
      ps.setInt(1, index);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Timestamp send_ts = rs.getTimestamp(6);
        Timestamp ack_ts = rs.getTimestamp(7);
        Long send_time = Long.valueOf(0L);
        Long ack_time = Long.valueOf(0L);
        if (send_ts != null) {
          send_time = Long.valueOf(send_ts.getTime());
        }
        if (ack_ts != null) {
          ack_time = Long.valueOf(ack_ts.getTime());
        }
        w = new Warning(Integer.valueOf(rs.getInt("id")), rs.getString("to"), rs.getString("from"), rs.getString("message"), rs.getBoolean("ack"), send_time, ack_time, rs.getString("code"));
      }
      conn.commit();
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Warning get error: {1}", new Object[] { plugin.name, e }));
    }
    return w;
  }

  public ArrayList<String> listWarnings(String to)
  {
    ArrayList output = new ArrayList();
    try {
      Connection conn = getConnection();
      PreparedStatement ps = conn.prepareStatement(new StringBuilder().append("SELECT `id`,`to`,`from`,`message`,`ack`,`send_time`,`ack_time`,`code` FROM `warnings` WHERE `to` LIKE '%").append(to).append("%' ORDER BY `id` ASC").toString());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Timestamp send_ts = rs.getTimestamp(6);
        Timestamp ack_ts = rs.getTimestamp(7);
        Long send_long = Long.valueOf(0L);
        Long ack_long = Long.valueOf(0L);
        if (send_ts != null) {
          send_long = Long.valueOf(send_ts.getTime());
        }
        if (ack_ts != null) {
          ack_long = Long.valueOf(ack_ts.getTime());
        }
        String send_time = plugin.getFormatTime(send_long);
        String ack_time = plugin.getFormatTime(ack_long);
        String buildLine = new StringBuilder().append(plugin.messageColor).append("[").append(plugin.infoColor).append(rs.getInt("id")).append(plugin.messageColor).append("] ").append(plugin.infoColor).append(send_time).append(plugin.messageColor).append(" From: ").append(plugin.nameColor).append(rs.getString("from")).append(plugin.messageColor).append(" To: ").append(plugin.nameColor).append(rs.getString("to")).toString();

        if (rs.getBoolean("ack")) {
          buildLine = new StringBuilder().append(buildLine).append(plugin.infoColor).append(" *ACK* ").append(plugin.messageColor).append("At: ").append(plugin.infoColor).append(ack_time).toString();
        }
        output.add(buildLine);
      }
      conn.commit();
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Warning load error: {1}", new Object[] { plugin.name, e }));
    }
    return output;
  }

  public int countWarnings(String player)
  {
    int result = -1;
    try {
      Connection conn = getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT count(`id`) FROM `warnings` WHERE `to` LIKE ?");
      ps.setString(1, player);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        result = rs.getInt(1);
      }
      conn.commit();
    } catch (SQLException e) {
      log.log(Level.SEVERE, MessageFormat.format("[{0}] Warning count error: {1}", new Object[] { plugin.name, e }));
    }
    return result;
  }
}