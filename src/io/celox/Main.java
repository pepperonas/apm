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
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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

    @SuppressWarnings("unused")
    private static final String TAG = "Main";

    private TextField tfPackageName;
    private ChoiceBox<String> choiceBoxPermissions;

    private static final String ANDROID_PERMISSION = "android.permission.";
    private ListView<String> listView;
    private Button btnGetPermissions;
    private String lastSelectedPermission = "";

    @Override
    public void start(Stage stage) throws Exception {
        @SuppressWarnings("unused")
        Parent root = FXMLLoader.load(getClass().getResource("layout_main.fxml"));

        stage.setTitle(Const.APP_NAME);

        visitPlayground();

        BorderPane borderPane = new BorderPane();

        VBox vBoxLeft = getPaneLeft();
        vBoxLeft.setPadding(new Insets(5, 5, 5, 5));
        borderPane.setLeft(vBoxLeft);

        VBox vBoxCenter = getPaneCenter();
        vBoxCenter.setPadding(new Insets(5, 5, 5, 5));
        borderPane.setCenter(vBoxCenter);

        VBox vBoxRight = getPaneRight(tfPackageName, choiceBoxPermissions);
        vBoxRight.setPadding(new Insets(5, 5, 5, 5));
        borderPane.setRight(vBoxRight);
        borderPane.setPadding(new Insets(10, 10, 10, 10));

        // layout the scene.
        final StackPane background = new StackPane();
        background.setStyle("-fx-background-color: #B2DFDB;");
        final Scene scene = new Scene(new Group(background, borderPane), 800, 600);
        background.prefHeightProperty().bind(scene.heightProperty());
        background.prefWidthProperty().bind(scene.widthProperty());
        stage.setScene(scene);
        stage.show();

        setCloseOnEsc(scene);
    }


    private VBox getPaneLeft() {
        listView = new ListView<>();
        listView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> tfPackageName.setText(newValue != null ? newValue : oldValue));

        Button btnGetPackages = new Button("GET PACKAGES");
        btnGetPackages.setOnAction(event -> {

            List<String> packages = new ArrayList<>();
            try {
                String[] lines = runProcess("adb shell pm list packages").split("\n");
                for (String line : lines) {
                    if (line == null
                            || line.isEmpty()
                            || line.equals("package:android")
                            || line.equals("package:x.abcd")
                            || line.contains("package:com.android.")) {
                        continue;
                    }
                    String pkg = line.replace("package:", "");
                    packages.add(pkg);
                    System.out.println(pkg);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                ObservableList<String> data = FXCollections.observableList(packages);
                listView.getItems().setAll(data.sorted());

                if (tfPackageName.getText() != null && !tfPackageName.getText().isEmpty()) {
                    listView.getSelectionModel().select(tfPackageName.getText());
                }
            });
        });

        VBox vBoxLeft = new VBox(5);
        vBoxLeft.getChildren().addAll(btnGetPackages, listView);

        btnGetPackages.fire();

        return vBoxLeft;
    }

    private VBox getPaneCenter() {
        tfPackageName = new TextField();
        tfPackageName.setText(Prefs.getLastPackageName());
        tfPackageName.setEditable(false);

        choiceBoxPermissions = new ChoiceBox<>();

        Label lOutput = new Label("...");
        lOutput.setWrapText(true);

        btnGetPermissions = new Button("GET PERMISSIONS");
        btnGetPermissions.setDisable(tfPackageName.getText().isEmpty());
        btnGetPermissions.setOnAction(event -> {

            if (choiceBoxPermissions.getItems().size() != 0) {
                lastSelectedPermission = choiceBoxPermissions.getValue();
            }

            try {
                Prefs.setLastPackageName(tfPackageName.getText());

                String[] lines = runProcess("adb shell dumpsys package " +
                        tfPackageName.getText()).split("\n");

                List<String> requestedPermissions = new ArrayList<>();

                StringBuilder output = new StringBuilder();
                boolean writeOutput = false;
                for (String line : lines) {
                    if (line == null || line.isEmpty()) {
                        continue;
                    }

                    if (line.contains("requested permissions:") && !writeOutput) {
                        output.append("---REQUESTED PERMISSIONS---")
                                .append("\n");
                        writeOutput = true;
                    }
                    if (line.contains("install permissions:")) {
                        output.append("\n")
                                .append("---INSTALL PERMISSIONS---")
                                .append("\n");
                    }
                    if (line.contains("mSkippingApks:")) {
                        writeOutput = false;
                    }
                    System.out.println(line);
                    if (writeOutput && line.contains(ANDROID_PERMISSION)) {
                        String requestedPermission = line
                                .replace(" ", "")
                                .replace(ANDROID_PERMISSION, "");

                        output.append(requestedPermission)
                                .append("\n");

                        if (!requestedPermission.contains("granted")) {
                            requestedPermissions.add(requestedPermission);
                        }
                    }
                    lOutput.setText(output.toString());

                    ObservableList<String> data = FXCollections.observableList(requestedPermissions);
                    choiceBoxPermissions.getItems().setAll(data);

                    if (lastSelectedPermission.equals("")) {
                        choiceBoxPermissions.getSelectionModel().selectFirst();
                    } else choiceBoxPermissions.getSelectionModel().select(lastSelectedPermission);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        tfPackageName.textProperty().addListener((observable, oldValue, newValue) -> btnGetPermissions.setDisable(newValue.isEmpty()));

        VBox vBoxCenter = new VBox(5);
        vBoxCenter.getChildren().addAll(tfPackageName, btnGetPermissions, lOutput);
        return vBoxCenter;
    }

    private VBox getPaneRight(TextField tfPackageName, ChoiceBox<String> choiceBoxPermissions) {
        Button btnGrantPermission = new Button("GRANT PERMISSION");
        btnGrantPermission.setDisable(tfPackageName.getText().isEmpty());
        btnGrantPermission.setOnAction(event -> {
            try {
                runProcess("adb shell pm grant " + tfPackageName.getText() + " " +
                        ANDROID_PERMISSION + choiceBoxPermissions.getValue());

                btnGetPermissions.fire();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Button btnRevokePermission = new Button("REVOKE PERMISSION");
        btnRevokePermission.setDisable(tfPackageName.getText().isEmpty());
        btnRevokePermission.setOnAction(event -> {
            try {
                runProcess("adb shell pm revoke " + tfPackageName.getText() + " " +
                        ANDROID_PERMISSION + choiceBoxPermissions.getValue());

                btnGetPermissions.fire();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HBox hBoxButtonsRight = new HBox(5);
        hBoxButtonsRight.getChildren().addAll(btnGrantPermission, btnRevokePermission);


        Button btnUninstallApp = new Button("UNINSTALL");
        btnUninstallApp.setDisable(tfPackageName.getText().isEmpty());
        btnUninstallApp.setOnAction(event -> {
            try {
                runProcess("adb shell pm uninstall -k " + tfPackageName.getText());

                listView.getSelectionModel().clearSelection();
                listView.getItems().remove(tfPackageName.getText());

                tfPackageName.setText("");
                Prefs.setLastPackageName("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Button btnUnused = new Button("---");
        btnUnused.setDisable(tfPackageName.getText().isEmpty());
        btnUnused.setOnAction(event -> {
            try {
                runProcess("adb devices");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        HBox hBoxButtonsRight2 = new HBox(5);
        hBoxButtonsRight2.getChildren().addAll(btnUninstallApp, btnUnused);

        tfPackageName.textProperty().addListener((observable, oldValue, newValue) -> {
            btnGrantPermission.setDisable(tfPackageName.getText().isEmpty());
            btnRevokePermission.setDisable(tfPackageName.getText().isEmpty());
            btnUninstallApp.setDisable(tfPackageName.getText().isEmpty());
            btnUnused.setDisable(tfPackageName.getText().isEmpty());
        });

        VBox vBoxRight = new VBox(5);
        vBoxRight.getChildren().addAll(choiceBoxPermissions, hBoxButtonsRight, hBoxButtonsRight2);
        return vBoxRight;
    }

    private String runProcess(String command) throws IOException {
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

    private void visitPlayground() {
//        Prefs.setLastPackageName("");
//        Field[] fields = Manifest.permission.class.getFields(); // use to get all...
    }

}
