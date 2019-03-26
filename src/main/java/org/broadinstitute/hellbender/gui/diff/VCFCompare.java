package org.broadinstitute.hellbender.gui.diff;

import com.google.common.collect.Lists;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
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
import org.broadinstitute.hellbender.gui.diff.iterators.FilteringIterator;
import org.broadinstitute.hellbender.gui.diff.iterators.PositionMatchingPairIterator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VCFCompare extends Application {

    private FeatureInput<VariantContext> left = new FeatureInput<VariantContext>("testData/left.vcf", "left", Collections.emptyMap());
    private FeatureInput<VariantContext> right = new FeatureInput<VariantContext>("testData/right.vcf", "right", Collections.emptyMap());

    private final MultiVariantDataSource dataSource = new MultiVariantDataSource(Arrays.asList(left, right), 1000);
    private final CloseableIterator<VariantDiffPair> variantIter = new PositionMatchingPairIterator(dataSource.iterator(),
                                                                                                    dataSource.getHeader(),
                                                                                                    left.getName(), right.getName());

    private final CloseableIterator<VariantDiffPair> filteredVariantIter = new FilteringIterator<>(variantIter, VariantDiffPair::mismatching);

    private final List<VariantDiffPair> variants = Lists.newArrayList(variantIter);

    private TableView<VariantDiffPair> table = new TableView<>();

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * get the checkbox to determine if the table is filtered or not
     * @return
     */
    public Node getFilterSettings() {
        final VBox vBox = new VBox();
        final CheckBox onlyDifferences = new CheckBox("Only Differences");
        onlyDifferences.setSelected(false);
        onlyDifferences.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != oldValue) {
                if (newValue) {
                    setTableItems(variants.stream()
                                          .filter(VariantDiffPair::mismatching)
                                          .collect(Collectors.toList()));
                } else {
                    setTableItems(variants);
                }
            }
        });
        vBox.getChildren().addAll(onlyDifferences);
        vBox.setSpacing(10);
        return vBox;
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
        final HBox topBar = new HBox();
        topBar.setSpacing(10);
        topBar.getChildren().addAll(label, getFilterSettings());

        table.setEditable(true);

        ObservableList<TableColumn<VariantDiffPair, DiffDisplay>> columns = getColumns();

        table.getColumns().addAll(columns);
        setTableItems(variants);

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
        vbox.getChildren().addAll(topBar, scrollPane);

        final Tab viewTab = new Tab("View", vbox);
        viewTab.setClosable(false);
        tabs.getTabs().add(viewTab);

        final Tab selectTab = new Tab("Select", controlPane(table));
        selectTab.setClosable(false);

        tabs.getTabs().add(selectTab);

        ((Group) scene.getRoot()).getChildren().addAll(tabs);
        stage.setScene(scene);
        stage.show();

        hideColumnsWithCondition(table, DiffDisplay::isEmpty);

    }

    private static void hideColumnsWithCondition(TableView<VariantDiffPair> table, Predicate<DiffDisplay> include) {
        final int size = table.getItems().size();
        for(TableColumn<VariantDiffPair, ?> column : table.getColumns()){
            boolean allNull = true;
            boolean allEmpty = true;
            for(int i = 0; i < size; i++){
                final ObservableValue<DiffDisplay> cellObservableValue = (ObservableValue<DiffDisplay>)column.getCellObservableValue(i);
                if (cellObservableValue != null){
                    allNull = false;
                    if( !include.test(cellObservableValue.getValue())){
                        allEmpty = false;
                    }
                }
            }
            if ( !allNull && allEmpty) {
                System.out.println(column.getText() + " is empty");
                column.setVisible(false);
            }
        }
    }



    private void setTableItems(List<VariantDiffPair> variants) {
        table.setItems(FXCollections.observableArrayList(variants));
        table.refresh();
    }

    public static Node controlPane(TableView<VariantDiffPair> table){
        final ObservableList<TableColumn<VariantDiffPair,?>> columns = table.getColumns();
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

    private static void getSiteSpecificCheckBoxes(ObservableList<TableColumn<VariantDiffPair, ?>> columns, FlowPane flowPane) {
        columns.forEach( column -> {
            final CheckBox check = new CheckBox(column.getText());
            check.setSelected(true);
            column.visibleProperty().bindBidirectional(check.selectedProperty());
            flowPane.getChildren().add(check);
        });

    }

    private ObservableList<TableColumn<VariantDiffPair, DiffDisplay>> getColumns() {
        ObservableList<TableColumn<VariantDiffPair, DiffDisplay>> columns = FXCollections.observableArrayList();
        final Collection<VCFInfoHeaderLine> infoHeaderLines = dataSource.getHeader().getInfoHeaderLines();

        final TableColumn<VariantDiffPair, DiffDisplay> position = new TableColumn<>("Position");
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

    private TableColumn<VariantDiffPair, DiffDisplay> getTableColumn(FieldKey key, String label) {
        final TableColumn<VariantDiffPair, DiffDisplay> chrom = new TableColumn<>(label);

        chrom.setCellFactory(new Callback<TableColumn<VariantDiffPair, DiffDisplay>, TableCell<VariantDiffPair, DiffDisplay>>() {
                                 @Override
                                 public TableCell<VariantDiffPair, DiffDisplay> call(TableColumn<VariantDiffPair, DiffDisplay> param) {
                                     return new TableCell<VariantDiffPair, DiffDisplay>(){
                                         @Override
                                         protected void updateItem(DiffDisplay item, boolean empty) {
                                             if( item != null) {
                                                 this.setGraphic(item.getDisplayNode());
                                             }
                                         }
                                     };
                                 }
                             });
        chrom.setCellValueFactory(getCellFactory(key));
        return chrom;
    }

    private Callback<TableColumn.CellDataFeatures<VariantDiffPair, DiffDisplay>, ObservableValue<DiffDisplay>> getCellFactory(FieldKey key) {
        return p -> new SimpleObjectProperty<>(p.getValue().getDisplay(key));
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

    public static <T> void  autoFitTable(TableView<T> tableView) {
        tableView.getItems().addListener(new ListChangeListener<T>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                for (TableColumn<T,?> column : tableView.getColumns()) {
                    try {
                        columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    public static class DiffDisplay extends Pair<String, String> {

        /**
         * Creates a new pair
         *
         * @param key   The key for this pair
         * @param value The value to use for this pair
         */
        public DiffDisplay(String key, String value) {
            super(key, value);
        }

        /**
         * get a visual representation of this diff
         * @return
         */
        public Node getDisplayNode(){
            VBox vbox = new VBox();
            final String left = getKey();
            final String right = getValue();
            if (Objects.equals(left, right)) {
                vbox.getChildren().add(new Label(left));
            } else {
                final Label leftLabel = new Label(left);
                leftLabel.setTextFill(Color.RED);
                final Label rightLabel = new Label(right);
                rightLabel.setTextFill(Color.RED);
                vbox.getChildren().addAll(leftLabel, rightLabel);
            }
            return vbox;
        }

        public boolean isEmpty(){
            final String left = getKey();
            final String right = getValue();
            return (left == null || left.isEmpty())
                    && ( right == null || right.isEmpty());
        }

        public boolean hasDiff(){
            return Objects.equals(getKey(), getValue());
        }
    }

    public static class VariantFields {
        final VariantContext vc;

        final Map<FieldKey, String> fieldToValueMap = new LinkedHashMap<>();

        public VariantFields(VariantContext vc, VCFHeader header) {
            this.vc = vc;
            vc.getAttributes()
                    .keySet()
                    .forEach(key -> fieldToValueMap.put(new InfoKey(key), vc.getAttributeAsString(key, null)));

            final List<String> genotypeKeys = vc.calcVCFGenotypeKeys(header);
            for (Genotype genotype : vc.getGenotypes()) {
                final String sampleName = genotype.getSampleName();
                for (String key : genotypeKeys) {
                    final Object anyAttribute = genotype.getAnyAttribute(key);
                    if (anyAttribute != null) {
                        fieldToValueMap.put(new FormatKey(sampleName, key), anyAttribute.toString());
                    }
                }
            }
        }

        public String get(FieldKey key){
            return fieldToValueMap.get(key);
        }

    }


    private static class InfoKey implements FieldKey {
        private final String key;

        public InfoKey(String key) {
            this.key = key;
        }

        @Override
        public FieldType getFieldType() {
            return FieldType.INFO;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public boolean matches(FieldKey other) {
            return this.equals(other);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InfoKey infoKey = (InfoKey) o;
            return Objects.equals(key, infoKey.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    public interface FieldKey{
        public enum FieldType{
            INFO, FORMAT
        }
        FieldType getFieldType();
        String getKey();
        boolean matches(FieldKey other);
    }

    public static class FormatKey implements FieldKey {
        final private String key;
        final private String sample;


        private FormatKey(String key, String sample) {
            this.key = key;
            this.sample = sample;
        }

        @Override
        public FieldType getFieldType() {
            return FieldType.FORMAT;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public boolean matches(FieldKey other) {
            return this.equals(other);
        }

        public String getSample() {
            return sample;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FormatKey formatKey = (FormatKey) o;
            return Objects.equals(key, formatKey.key) &&
                    Objects.equals(sample, formatKey.sample);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, sample);
        }
    }

}
