package database;

import java.sql.Statement;

public class DatabaseInit {

    public static void criarTabelas() throws Exception {
        Statement st = Conexao.get().createStatement();

        st.execute("""
            CREATE TABLE IF NOT EXISTS clientes(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT,
                nif TEXT,
                tipo TEXT
            );
        """);

        st.execute("""
            CREATE TABLE IF NOT EXISTS produtos(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT,
                precoCusto REAL,
                margem REAL,
                incluiIVA INTEGER,
                stock INTEGER
            );
        """);

        st.execute("""
            CREATE TABLE IF NOT EXISTS vendas(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data TEXT,
                total REAL
            );
        """);

        st.execute("""
            CREATE TABLE IF NOT EXISTS venda_itens(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                idVenda INTEGER,
                nomeProduto TEXT,
                preco REAL,
                quantidade INTEGER,
                subtotal REAL
            );
        """);
    }
}
