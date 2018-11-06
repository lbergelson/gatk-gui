package org.broadinstitute.hellbender.gui;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
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
    public void start(Stage stage) {
        ObservableList<String> classNames = FXCollections.observableArrayList(commandLineClasses.keySet());

        final Label toolLabel = new Label();
        final SimpleStringProperty toolProperty = new SimpleStringProperty();
        toolLabel.textProperty().bind(toolProperty);

        final VBox commandBox = new VBox();
        commandBox.setFillWidth(true);

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
        ObservableList<ArgumentEntryLine> values = FXCollections.observableArrayList( w -> new Observable[]{ w.getCommandLineString()});
        requiredArguments.forEach(a -> {
            final ArgumentEntryLine argumentEntryLine = new ArgumentEntryLine(stage, a);
            values.add(argumentEntryLine);
            commandBox.getChildren().add(argumentEntryLine.getNode());

        });

        commandBox.getChildren().add(new Separator());

        final Button runButton = new Button("Run");

        runButton.disableProperty().bind(Bindings.createBooleanBinding(() -> values.stream().anyMatch(a -> a.getText().isEmpty()), values));

        StringBinding argumentListBinding = Bindings.createStringBinding(() -> values.stream()
                .map(a -> a.getText().isEmpty() ? "" : a.getArgument() + " " + a.getText())
                .collect(Collectors.joining(" ")), values);

        Label arglist = new Label();
        arglist.textProperty().bind(Bindings.concat(toolName, " ", argumentListBinding));
        commandBox.getChildren().add(arglist);

        runButton.setOnAction( action -> {
            final Stream<String> args = values.stream()
                    .flatMap(e -> Stream.of(e.getArgument(), e.getText()));
            main.instanceMain(Stream.concat(Stream.of(toolName), args).toArray(String[]::new));
        });

        commandBox.getChildren().add(runButton);
    }

    private static class ArgumentEntryLine {
        private final HBox root = new HBox();
        private final TextField textField;
        private final Stage stage;
        private final CommandLineArgumentParser.ArgumentDefinition argumentDefinition;
        private final Label label;
        private final ChoiceBox<Enum<?>> choice;
        private final StringExpression argumentValue;

        public ArgumentEntryLine(Stage stage, CommandLineArgumentParser.ArgumentDefinition argumentDefinition) {
            this.stage = stage;
            this.argumentDefinition = argumentDefinition;
            root.setSpacing(10);
            root.setPrefWidth(800);

            label = new Label(argumentDefinition.getLongName());

            Tooltip tip = new Tooltip(argumentDefinition.doc);
            Tooltip.install(root, tip);

            final ObservableList<Node> rootChildren = root.getChildren();

            if (argumentDefinition.type.isEnum()){
                final Enum<?>[] enumConstants = ((Class<Enum<?>>) argumentDefinition.type).getEnumConstants();
                choice = new ChoiceBox<>(FXCollections.observableArrayList(Arrays.asList(enumConstants)));
                choice.setPrefWidth(500);
                rootChildren.add(choice);
                argumentValue = Bindings.createStringBinding(() -> choice.getValue().toString(), choice.valueProperty());
                textField = null;
            } else {
                choice = null;

                textField = new TextField();
                textField.setPromptText(argumentDefinition.type.getSimpleName());


                argumentValue = textField.textProperty();
                if (isFilelike(argumentDefinition)) {
                    textField.setPrefWidth(430);
                    final HBox subBox = new HBox(textField, getFileSelectButton());
                    subBox.setSpacing(0);
                    rootChildren.add(subBox);
                } else {
                    textField.setPrefWidth(500);
                    rootChildren.add(textField);
                }
            }
            rootChildren.add(label);
        }

        private Button getFileSelectButton(){
            final Button select = new Button("Select");
            select.setPrefWidth(70);
            select.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Input");
                File selectedFile = fileChooser.showOpenDialog(stage);
                if (selectedFile != null) {
                    textField.setText(selectedFile.getPath());
                }
            });
            return select;
        }

        private static boolean isFilelike(CommandLineArgumentParser.ArgumentDefinition argDef) {
            return argDef.type == File.class || isFilish(argDef.getLongName());
        }

        public Node getNode(){
            return root;
        }

        public StringExpression getCommandLineString(){
            return argumentValue;
        }

        public String getText(){
            return argumentValue.getValue();
        }

        public String getArgument(){
            return "--" + argumentDefinition.getLongName();
        }
    }

    private static boolean isFilish(String name){
        final Set<String> fileish = new HashSet<>(Arrays.asList("input", "output", "reference", "variant"));
        return fileish.contains(name);
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


