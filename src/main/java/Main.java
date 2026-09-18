import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        //TEXT: User will be asked to select their fav colors (can choose more than one)
        Text headerText = new Text("Select your favorite colors (can choose more than one):");
        headerText.setFont(Font.font("Times New Roman", 20));

        //LINES
        Line line1 = new Line(0, 0, 300, 0);
        Line line2 = new Line(0, 0, 300, 0);

        //CHECKBOXES
        CheckBox redBox = new CheckBox("Red");
        CheckBox orangeBox = new CheckBox("Orange");
        CheckBox yellowBox = new CheckBox("Yellow");
        CheckBox greenBox = new CheckBox("Green");
        CheckBox blueBox = new CheckBox("Blue");
        CheckBox indigoBox = new CheckBox("Indigo");
        CheckBox violetBox = new CheckBox("Violet");

        //RADIO BUTTONS
        ToggleGroup styleGroup = new ToggleGroup();
        RadioButton neonRadio = new RadioButton("Neon");
        neonRadio.setToggleGroup(styleGroup);

        RadioButton pastelRadio = new RadioButton("Pastel");
        pastelRadio.setToggleGroup(styleGroup);
        pastelRadio.setSelected(true);

        //LAYOUT
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(headerText, line1, redBox, orangeBox, yellowBox, greenBox, blueBox, indigoBox, violetBox, line2, neonRadio, pastelRadio);

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("Color Selection");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
