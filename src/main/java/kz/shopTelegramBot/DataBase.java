package kz.shopTelegramBot;

import java.sql.*;
import java.util.HashMap;

public class DataBase extends Thread {

    static Connection conn;

    @Override
    public void run() {
        try {
            String url = "jdbc:mysql://database-1.cb1y1q5wlenm.us-east-1.rds.amazonaws.com:3306/Shop?serverTimezone=Asia/Almaty&useSSL=false";
            String username = "root";
            String password = "mypassword";
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int addProduct(String[] data) throws SQLException {
        String query = "INSERT INTO `Shop`.`Goods` (`Price`, `G_Count`, `G_Name`, `G_CPU`, `RAM`, `G_Memory`, `Weight`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?);";

        int id;
        try (PreparedStatement prStmt = conn.prepareStatement(query);
             Statement statement = conn.createStatement()) {
            prStmt.setInt(1, Integer.parseInt(data[0]));
            prStmt.setInt(2, Integer.parseInt(data[1]));
            prStmt.setString(3, data[2]);
            prStmt.setString(4, data[3]);
            prStmt.setInt(5, Integer.parseInt(data[4]));
            prStmt.setString(6, data[5]);
            prStmt.setDouble(7, Double.parseDouble(data[6]));
            prStmt.executeUpdate();

            ResultSet getId = statement.executeQuery(
                    "SELECT GoodsID FROM `Shop`.`Goods` ORDER BY GoodsID DESC LIMIT 1");
            getId.next();
            id = getId.getInt("GoodsID");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return id;
    }

    public void setMsgId(int id) {
        String query = "UPDATE `Shop`.`Goods` SET Msg_ID = ? " +
                "WHERE GoodsID = (SELECT Gid FROM (SELECT GoodsID Gid FROM Goods ORDER BY GoodsID DESC LIMIT 1) sub);";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setInt(1, id);
            prStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, String> updateCount(int id, int count) {
        String query = "UPDATE `Shop`.`Goods` SET G_Count = ?" +
                " WHERE GoodsID = ?";
        try (PreparedStatement prStmt = conn.prepareStatement(query);
             Statement statement = conn.createStatement()) {
            prStmt.setInt(1, count);
            prStmt.setInt(2, id);
            prStmt.executeUpdate();
            statement.executeQuery("SELECT * FROM `Shop`.`Goods` WHERE GoodsID = " + id);
            ResultSet rs = statement.getResultSet();
            ResultSetMetaData rsmd = rs.getMetaData();
            rs.next();
            HashMap<String, String> result = new HashMap<>();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String key = rsmd.getColumnName(i);
                String value = rs.getString(key);
                result.put(key, value);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void purchaseProduct(Long user, int goodsId) {
        String query = "CALL Purchase(?, ?)";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setLong(1, user);
            prStmt.setInt(2, goodsId);

            prStmt.executeUpdate();

        } catch (SQLException ignored) {
        }
    }

    public HashMap<String, Object> doesProductAvailable(int goodsID){
        try (PreparedStatement prStmt = conn.prepareStatement("SELECT * FROM `Shop`.`Goods` WHERE GoodsID = ? ")){
            prStmt.setInt(1, goodsID);
            System.out.println(prStmt);
            prStmt.executeQuery();
            ResultSet rs = prStmt.getResultSet();
            ResultSetMetaData rsmd = rs.getMetaData();
            System.out.println(rs.next());

            HashMap<String, Object> result = new HashMap<>();

            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String key = rsmd.getColumnName(i);
                Object value = rs.getObject(key);
                result.put(key, value);
            }

            return result;

        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public String findProduct(int id) {
        String query = "SELECT * FROM `Shop`.`Goods` WHERE GoodsID = ?";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setInt(1, id);
            ResultSet resultSet = prStmt.executeQuery();
            resultSet.next();
            return resultSet.getString("G_Name");
        } catch (SQLException ignored) {
        }

        return "";
    }

    public void addUser(HashMap<String, Object> data) {
        if (doesUserExists(data.get("user_id"))) {
            updateUserInfo(data);
            return;
        }
        String query = "INSERT INTO `Shop`.`Users` (`UserID`, `FirstName`, `LastName`, `Username`) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setObject(1, data.get("user_id"));
            prStmt.setObject(2, data.get("f_name"));
            prStmt.setObject(3, data.get("l_name"));
            prStmt.setObject(4, data.get("username"));

            prStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean doesUserExists(Object id) {
        String query = "SELECT count(*) FROM `Shop`.`Users` " +
                "WHERE UserID = ?";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setObject(1, id);
            ResultSet resultSet = prStmt.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) > 0) {
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    private void updateUserInfo(HashMap<String, Object> data) {
        String query = "UPDATE `Shop`.`Users` SET  `FirstName` = ? , `LastName` = ? , `Username` = ? " +
                "WHERE `UserID` = ?";
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setObject(1, data.get("f_name"));
            prStmt.setObject(2, data.get("l_name"));
            prStmt.setObject(3, data.get("username"));
            prStmt.setObject(4, data.get("user_id"));

            prStmt.executeUpdate();

        } catch (SQLException ignored) {
        }
    }

    public String getCartData(Long userId) throws SQLException {
        String query = "SELECT * FROM `Shop`.`Cart` c " +
                "JOIN `Shop`.`Goods` g " +
                "ON g.GoodsID = c.GoodsID " +
                "WHERE c.UserID = ?";
        StringBuffer result = new StringBuffer();
        try (PreparedStatement prStmt = conn.prepareStatement(query)) {
            prStmt.setLong(1, userId);
            ResultSet rs = prStmt.executeQuery();
            while (rs.next()) {
                String eachRow = rs.getString("G_Name") + " "
                        + rs.getString("G_CPU") +  " x" + rs.getString("Count") + "\n";
                result.append(eachRow);
            }
            return result.toString();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
