/*
 * Copyright (c) 2017 Martin Pfeffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.celox;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The type Main.
 * <p>
 *
 * @see <a href="https://gist.github.com/jewelsea/1441960">JavaFX</a>
 * @see <a href="http://stackoverflow.com/questions/21091022/listing-permissions-of-android-application-via-adb">List
 * Android permissions</a>
 * @see <a href="http://stackoverflow.com/questions/15464111/run-cmd-commands-through-java">Run cmd through Java</a>
 * @see <a href="http://stackoverflow.com/questions/16410167/how-do-i-use-adb-grant-or-adb-revoke">Grant and revoke
 * permissions via adb</a>
 * <p>
 * * @author Martin Pfeffer <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */

public class Main extends Application {

    private static final String TAG = "Main";

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout_main.fxml"));
        stage.setTitle(Const.APP_NAME);

        TextField tfPackageName = new TextField();
        tfPackageName.setText(Prefs.getLastPackageName());
        tfPackageName.setPromptText("Enter package name...");

        Label lOutput = new Label("...");
        lOutput.setWrapText(true);

        Button btnGetPermissions = new Button("GET PERMISSIONS");
        btnGetPermissions.setOnAction(event -> {
            try {
                Prefs.setLastPackageName(tfPackageName.getText());

                String[] lines = runProcess("adb shell dumpsys package " + tfPackageName.getText()).split("\n");
                StringBuilder output = new StringBuilder();
                boolean writeOutput = false;
                for (String line : lines) {
                    if (line.contains("requested permissions:") && !writeOutput) {
                        writeOutput = true;
                    }
                    if (line.contains("mSkippingApks:")) {
                        writeOutput = false;
                    }
                    System.out.println(line);
                    if (writeOutput) {
                        output.append(line).append("\n");
                    }
                    lOutput.setText(output.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        List<String> permissions = new ArrayList<>();
        Field[] fields = Manifest.permission.class.getFields();
        System.out.println(fields.length);
        for (Field field : fields) {
            String permission = "android.permission." + field.getName();
            permissions.add(permission);
            System.out.println(permission);
        }


        ObservableList<String> data = FXCollections.observableList(permissions);
        final ChoiceBox<String> choiceBoxPermissions = new ChoiceBox<>(data);
        choiceBoxPermissions.getSelectionModel().selectFirst();

        VBox vBoxLeft = new VBox(5);
        vBoxLeft.getChildren().addAll(tfPackageName, btnGetPermissions, lOutput);
        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(vBoxLeft);

        Button btnGrantPermission = new Button("GRANT PERMISSION");
        btnGrantPermission.setOnAction(event -> {
            try {
                runProcess("adb shell pm grant " + tfPackageName.getText() + " " + choiceBoxPermissions.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Button btnRevokePermission = new Button("REVOKE PERMISSION");
        btnRevokePermission.setOnAction(event -> {
            try {
                runProcess("adb shell pm revoke " + tfPackageName.getText() + " " + choiceBoxPermissions.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HBox hBoxButtonsRight = new HBox(5);
        hBoxButtonsRight.getChildren().addAll(btnGrantPermission, btnRevokePermission);

        VBox vBoxRight = new VBox(5);
        vBoxRight.getChildren().addAll(choiceBoxPermissions, hBoxButtonsRight);
        borderPane.setRight(vBoxRight);

        // layout the scene.
        final StackPane background = new StackPane();
        background.setStyle("-fx-background-color: cornsilk;");
        final Scene scene = new Scene(new Group(background, borderPane), 800, 600);
        background.prefHeightProperty().bind(scene.heightProperty());
        background.prefWidthProperty().bind(scene.widthProperty());
        stage.setScene(scene);
        stage.show();

        setCloseOnEsc(scene);
    }

    public String runProcess(String command) throws IOException {
        System.out.print("command to run: " + command + "\n");

        StringBuilder result = new StringBuilder();

        ProcessBuilder builder;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            builder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            builder = new ProcessBuilder("/bin/bash", "-l", "-c", command);
        }
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            result.append(line).append("\n");
            System.out.println(line);
        }

        return result.toString();
    }


    private void createFolderOnDesktop() throws IOException {

        ProcessBuilder builder;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            builder = new ProcessBuilder("cmd.exe", "/c", "cd \"C:\\Users\\marti\\Desktop\" && mkdir ok");
        } else {
            builder = new ProcessBuilder("/bin/bash", "-l", "-c", "cd \"C:\\Users\\marti\\Desktop\" && mkdir ok");
        }

        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while (true) {
            line = r.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
        }
    }

    private void setCloseOnEsc(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                System.out.println("[ESC] Clicked");
                ((Stage) scene.getWindow()).close();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
