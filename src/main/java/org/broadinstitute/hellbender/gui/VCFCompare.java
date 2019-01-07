package org.broadinstitute.hellbender.gui;

import avro.shaded.com.google.common.collect.Lists;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.broadinstitute.hellbender.engine.FeatureDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VCFCompare extends Application {

    private String left = "/Users/louisb/Workspace/gatk/src/test/resources/org/broadinstitute/hellbender/tools/walkers/variantutils/SelectVariants/vcf4.1.example.vcf";
    private String right = "/Users/louisb/Workspace/gatk/src/test/resources/org/broadinstitute/hellbender/tools/walkers/variantutils/SelectVariants/vcf4.1.example.vcf";


    private FeatureDataSource<VariantContext> dataSource = new FeatureDataSource<VariantContext>(left);
    private List<VariantContext> variants = Lists.newArrayList(dataSource.iterator());
    private VCFHeader header = (VCFHeader)dataSource.getHeader();

    private TableView table = new TableView();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new Group());

        stage.setTitle("Table View Sample");
        stage.setWidth(1200);
        stage.setHeight(500);

        TabPane tabs = new TabPane();
        final Label label = new Label("VCF View");
        label.setFont(new Font("Arial", 20));

        table.setEditable(true);

        ObservableList<TableColumn> columns = getColumns();

        table.getColumns().addAll(columns);
        table.setItems(FXCollections.observableArrayList(variants));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(table);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefSize(1200, 500);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);


        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        vbox.getChildren().addAll(label, scrollPane);

        tabs.getTabs().add(new Tab("View", vbox));
        tabs.getTabs().add(new Tab("Select", controlPane()));

        ((Group) scene.getRoot()).getChildren().addAll(tabs);
        stage.setScene(scene);
        stage.show();
    }

    public Node controlPane(){
        final ObservableList<TableColumn<?,?>> columns = table.getColumns();
        ScrollPane scrollPane = new ScrollPane();
        final VBox vbox = new VBox();
        vbox.setFillWidth(true);
        vbox.setSpacing(5);
        Label label = new Label("Columns to Display");
        vbox.getChildren().add(label);
        final FlowPane flowPane = new FlowPane();
        flowPane.setHgap(10);
        flowPane.setColumnHalignment(HPos.CENTER);
        Label siteLabel = new Label("Info Field");
        vbox.getChildren().add(siteLabel);
        vbox.getChildren().add(flowPane);
        vbox.setPadding(new Insets(10, 0, 0, 10));


        getSiteSpecificCheckBoxes(columns, flowPane);

        scrollPane.setContent(vbox);
        return scrollPane;
    }

    private void getSiteSpecificCheckBoxes(ObservableList<TableColumn<?, ?>> columns, FlowPane flowPane) {
        columns.forEach( column -> {
            final CheckBox check = new CheckBox(column.getText());
            check.setSelected(true);
            column.visibleProperty().bindBidirectional(check.selectedProperty());
            flowPane.getChildren().add(check);
        });
    }

    private ObservableList<TableColumn> getColumns() {
        ObservableList<TableColumn> columns = FXCollections.observableArrayList();
        final Collection<VCFInfoHeaderLine> infoHeaderLines = header.getInfoHeaderLines();

        final TableColumn position = new TableColumn("Position");
        position.getColumns().add(getTableColumn(VariantContext::getContig, "Chrom"));
        position.getColumns().add(getTableColumn(v -> String.valueOf(v.getStart()), "Start"));
        columns.add(position);
        columns.add(getTableColumn( v -> v.getReference().getDisplayString(),"Ref"));
        columns.add(getTableColumn( v -> v.getAlternateAlleles().stream().map(Allele::getDisplayString).collect(
                Collectors.joining(",")), "Alt"));
        infoHeaderLines.forEach(line -> {
            final String id = line.getID();
            columns.add(getTableColumn(v -> {
                final Object attribute = v.getAttribute(id);
                return attribute == null ? "" : attribute.toString();
            }, id));
        });

        return columns;
    }

    private TableColumn getTableColumn(Function<VariantContext, String> getContig, String label) {
        final TableColumn chrom = new TableColumn(label);

        chrom.setCellFactory(new Callback<TableColumn, TableCell>() {
                                 @Override
                                 public TableCell call(TableColumn param) {
                                     return new TableCell(){
                                         @Override
                                         protected void updateItem(Object item, boolean empty) {
                                             String value = (String)item;
                                             VBox vbox = new VBox();
                                             vbox.getChildren().addAll(new Label(value), new Label("lable2"));
                                             this.setGraphic(vbox);
                                         }
                                     };
                                 }
                             });
        chrom.setCellValueFactory(getCellFactory(getContig));
        return chrom;
    }

    private Callback<TableColumn.CellDataFeatures<VariantContext, String>, ObservableValue<String>> getCellFactory(Function<VariantContext, String> getter) {
        return (Callback<TableColumn.CellDataFeatures<VariantContext, String>, ObservableValue<String>>) p -> {
                    // p.getValue() returns the Person instance for a particular TableView row
            return new SimpleStringProperty(getter.apply(p.getValue()));
                };
    }

    private static Method columnToFitMethod;

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void autoFitTable(TableView tableView) {
        tableView.getItems().addListener(new ListChangeListener<Object>() {
            @Override
            public void onChanged(Change<?> c) {
                for (Object column : tableView.getColumns()) {
                    try {
                        columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
