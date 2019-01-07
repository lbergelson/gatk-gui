package org.broadinstitute.hellbender.gui;

import com.google.common.collect.Lists;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.PeekableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.broadinstitute.hellbender.engine.FeatureInput;
import org.broadinstitute.hellbender.engine.MultiVariantDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VCFCompare extends Application {

    private FeatureInput<VariantContext> left = new FeatureInput<VariantContext>("testData/left.vcf", "left", Collections.emptyMap());
    private FeatureInput<VariantContext> right = new FeatureInput<VariantContext>("testData/right.vcf", "right", Collections.emptyMap());

    private final MultiVariantDataSource dataSource = new MultiVariantDataSource(Arrays.asList(left, right), 1000);
    private final CloseableIterator<Pair<VariantContext, VariantContext>> variantIter = new DiffIterator(dataSource.iterator(), left.getName(), right.getName());
    private final List<Pair<VariantContext, VariantContext>> variants = Lists.newArrayList(variantIter);
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
        scrollPane.setPrefSize(stage.getWidth(), stage.getHeight());
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
        final Collection<VCFInfoHeaderLine> infoHeaderLines = dataSource.getHeader().getInfoHeaderLines();

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
                                             Pair<String, String> value = (Pair<String, String>)item;
                                             VBox vbox = new VBox();
                                             if( value != null) {
                                                 final String left = value.getKey();
                                                 final String right = value.getValue();
                                                 if (Objects.equals(left, right)) {
                                                     vbox.getChildren().add(new Label(left));
                                                 } else {
                                                     final Label leftLabel = new Label(left);
                                                     leftLabel.setTextFill(Color.RED);
                                                     final Label rightLabel = new Label(right);
                                                     rightLabel.setTextFill(Color.RED);
                                                     vbox.getChildren().addAll(leftLabel, rightLabel);
                                                 }
                                                 this.setGraphic(vbox);
                                             }
                                         }
                                     };
                                 }
                             });
        chrom.setCellValueFactory(getCellFactory(getContig));
        return chrom;
    }

    private Callback<TableColumn.CellDataFeatures<Pair<VariantContext, VariantContext>, Pair<String,String>>, ObservableValue<Pair<String,String>>> getCellFactory(Function<VariantContext, String> getter) {
        return p -> {
            Pair<VariantContext, VariantContext> value = p.getValue();
            String leftString = value.getKey() == null ? null : getter.apply(value.getKey());
            String rightString = value.getValue() == null ? null : getter.apply(value.getValue());
            return new SimpleObjectProperty<>(new Pair<>(leftString, rightString));
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

    private static class DiffIterator implements CloseableIterator<Pair<VariantContext, VariantContext>>{
        private final PeekableIterator<VariantContext> iterator;
        private final String left;
        private final String right;

        private DiffIterator(Iterator<VariantContext> iterator, String left, String right) {
            this.iterator = new PeekableIterator<>(iterator);
            this.left = left;
            this.right = right;
        }


        @Override
        public void close() {
            iterator.close();;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Pair<VariantContext, VariantContext> next() {
            if( hasNext()){
                VariantContext next = iterator.next();
                next.getSource();
                final VariantContext peek = iterator.peek();
                VariantContext tmpLeft = null;
                VariantContext tmpRight = null;
//                if( next.getSource().equals(left)){
//                    tmpLeft = next;
//                } else if ( next.getSource().equals(right)){
//                    tmpRight = next;
//                }
                tmpLeft = next;
                if(next.getContig().equals(peek.getContig()) && next.getStart() == peek.getStart()){
                    next = iterator.next();
                    tmpRight = next;
//                    if( next.getSource().equals(left)){
//                        tmpLeft = next;
//                    } else if ( next.getSource().equals(right)){
//                        tmpRight = next;
//                    }
                }
                return new Pair<>(tmpLeft, tmpRight);
            } else {
                throw new NoSuchElementException();
            }
        }


    }

}
