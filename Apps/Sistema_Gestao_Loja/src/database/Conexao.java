package database;

import java.sql.*;

public class Conexao {

    private static final String URL = "jdbc:sqlite:loja.db";

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
