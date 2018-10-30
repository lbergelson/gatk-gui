package org.broadinstitute.hellbender.gui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.broadinstitute.barclay.argparser.CommandLineArgumentParser;
import org.broadinstitute.hellbender.Main;
import org.broadinstitute.hellbender.cmdline.CommandLineProgram;


import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Gui extends Application {

    private final Main main = new Main();
    private final Map<String, Class<?>> commandLineClasses = main.getCommandLineClasses(main.getPackageList(), main.getClassList());
    private boolean canRun = false;

    @Override
    public void start(Stage stage) throws Exception {
        ObservableList<String> classNames = FXCollections.observableArrayList(
                commandLineClasses.keySet());

        final Label toolLabel = new Label();
        final SimpleStringProperty toolProperty = new SimpleStringProperty();
        toolLabel.textProperty().bind(toolProperty);

        final VBox commandBox = new VBox();


        ListView<String> classNameListView = new ListView<>();
        classNameListView.setItems(classNames);
        classNameListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> setCommandBox(commandBox, newValue, stage));
        classNameListView.setPrefWidth(300);
        final GridPane root = new GridPane();
        root.addColumn(1, classNameListView);
        root.addColumn(2, commandBox);

        final Scene scene = new Scene(root,1200, 1600);

        stage.setScene(scene);
        stage.show();
    }

    private void setCommandBox(VBox commandBox, String toolName, Stage stage){
        final Class<?> tool = commandLineClasses.get(toolName);
        final CommandLineProgram program = main.createCommandLineProgramInstance(tool);
        final CommandLineArgumentParser parser = (CommandLineArgumentParser) program.getCommandLineParser();
        final List<CommandLineArgumentParser.ArgumentDefinition> argumentDefinitions = parser.getArgumentDefinitions();
        final List<CommandLineArgumentParser.ArgumentDefinition> requiredArguments = argumentDefinitions.stream()
                .filter(a -> !a.optional)
                .filter(a -> !a.isControlledByPlugin())
                .collect(Collectors.toList());


        commandBox.getChildren().clear();
        commandBox.getChildren().add(new Label(toolName));
        Map<String, TextField> values = new LinkedHashMap<>();
        requiredArguments.forEach(a -> {
            final TextField text = new TextField();
            values.put(a.getLongName(), text);
            Node inputField = createInputFieldSet(stage, a);
            commandBox.getChildren().add(inputField);
        });

        final Button runButton = new Button("Run");

        runButton.setDisable(true);

        values.values().forEach(
                text -> text.setOnKeyTyped(e -> checkAllTextBoxes(values.values(), runButton))
        );


        StringBinding argumentListBinding = new ArgumentBinding(values);

        Label arglist = new Label();
        arglist.textProperty().bind(Bindings.concat(toolName, " ", argumentListBinding));
        commandBox.getChildren().add(arglist);

        runButton.setOnAction( action -> {
            final Stream<String> args = values.entrySet()
                    .stream()
                    .flatMap(e -> Stream.of("--" + e.getKey(), e.getValue().getText()));
            main.instanceMain(Stream.concat(Stream.of(toolName), args).toArray(String[]::new));
        });


        commandBox.getChildren().add(runButton);
    }

    private Node createInputFieldSet(Stage stage, TextField text, CommandLineArgumentParser.ArgumentDefinition argDef) {
        text.setPromptText(argDef.type.getSimpleName());
        final HBox argLine = new HBox(text, new Label(argDef.getLongName()));
        if( argDef.type == File.class || isFilish(argDef.getLongName())){
            final Button select = new Button("Select");
            select.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Input");
                File selectedFile = fileChooser.showOpenDialog(stage);
                if (selectedFile != null) {
                    text.setText(selectedFile.getPath());
                }
            });
            argLine.getChildren().add(select);
        }

        if (isCollectionField(argDef.field)) {
            final Button addMoreButton = new Button("+");

        }
        argLine.setSpacing(10);
        Tooltip tip = new Tooltip(argDef.doc);
        Tooltip.install(argLine, tip);
        return argLine;
    }


    private static class ArgumentEntryLine {
        private final HBox root = new HBox();
        private final TextField textField = new TextField();

        ArgumentEntryLine(Stage stage, CommandLineArgumentParser.ArgumentDefinition argumentDefinition){

        }


    }
    private void checkAllTextBoxes(Collection<TextField> values, Button run) {
        run.setDisable(values.stream().anyMatch(v -> v.getText().isEmpty()));
    }

    private boolean isFilish(String name){
        final Set<String> fileish = new HashSet<>(Arrays.asList("input", "output", "reference", "variant"));
        return fileish.contains(name);
    }

    private static class ArgumentBinding extends StringBinding {

        private final Map<String, TextField> values;

        public ArgumentBinding(Map<String, TextField> values) {
            this.values = values;
            values.values().forEach(e -> super.bind(e.textProperty())
            );
        }

        @Override
        protected String computeValue() {
            return values.entrySet().stream()
                    .filter(e -> !e.getValue().getText().isEmpty())
                    .flatMap(e -> Stream.of("--" + e.getKey(), e.getValue().getText()))
                    .collect(Collectors.joining(" "));
        }
    }

    private static boolean isCollectionField(final Field field) {
        try {
            field.getType().asSubclass(Collection.class);
            return true;
        } catch (final ClassCastException e) {
            return false;
        }
    }
}


