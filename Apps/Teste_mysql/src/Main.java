public class Main {
    public static void main(String[] args) {

        UsuarioDAO dao = new UsuarioDAO();

        System.out.println("📌 Lista de usuários no banco de dados:");

        dao.listarUsuarios().forEach(System.out::println);
    }
}
