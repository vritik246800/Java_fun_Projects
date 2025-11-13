import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public List<String> listarUsuarios() {
        List<String> lista = new ArrayList<>();

        String sql = "SELECT nome, email FROM usuarios";

        try (Connection con = Conexao.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nome = rs.getString("nome");
                String email = rs.getString("email");
                lista.add(nome + " - " + email);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }
}
