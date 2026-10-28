package com.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        TextField inputField = new TextField();
        inputField.setPromptText("Type your name here...");
        
        Button actionButton = new Button("Greet Me!");
        Label greetingLabel = new Label("Welcome!");

        actionButton.setOnAction(e -> {
            String name = inputField.getText();
            if (!name.isEmpty()) {
                greetingLabel.setText("Hello, " + name + "!");
            }
        });

        VBox layout = new VBox(15);
        layout.getChildren().addAll(inputField, actionButton, greetingLabel);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 350, 250);
        stage.setScene(scene);
        stage.setTitle("Interactive JavaFX App");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
