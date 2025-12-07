import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Olá, JavaFX 25 no macOS M1!");
        Scene scene = new Scene(label, 400, 200);
        stage.setScene(scene);
        stage.setTitle("JavaFX Teste");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
