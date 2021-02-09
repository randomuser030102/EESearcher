package me.XXX.eesearcher.ui;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import me.XXX.eesearcher.common.IndexDataController;
import me.XXX.eesearcher.SearchHistoryController;
import me.XXX.eesearcher.data.IndexData;
import me.XXX.eesearcher.data.QueryParameters;
import me.XXX.eesearcher.data.SearchResult;
import me.XXX.eesearcher.data.SubjectDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the landing page users will see. This class will draw a window which contains:
 *  - A text input field (search box)
 *  - A ListView (search history)
 *  - A ListView (search results)
 *  - A button (Add EEs)
 *
 * @see ImportPage
 */
@Singleton
public class GuestHomepage {

    private final VBox root = new VBox();
    private final HBox boxInfo = new HBox();
    private final Label info = new Label(" ");
    private final ProgressBar progressBar = new ProgressBar();
    private final SplitPane paneCentralView = new SplitPane();
    private final TitledPane paneSearchHistory = new TitledPane();

    private final ListView<Hyperlink> viewSearchHistory = new ListView<>();
    private final TextField fieldSearchInput = new TextField(" ");
    private final TitledPane paneSearchResultsParent = new TitledPane();
    private final ScrollPane paneSearchResults = new ScrollPane();
    private final TextFlow flowSearchResults = new TextFlow();
    private final Button importerButton = new Button("Add EEs");

    private final Stage stage;
    private final SceneController sceneController;
    private final boolean allowRawRegex = true;

    @Inject
    private SubjectDatabase subjectDatabase;
    @Inject
    private Injector injector;
    @Inject
    private IndexDataController indexDataController;
    @Inject
    private SearchHistoryController historyController;


    private boolean searching;

    @Inject
    public GuestHomepage(@Named("main") Stage stage, @NotNull SceneController controller) {
        this.stage = stage;
        this.sceneController = controller;
        initStage();
        controller.init(this, root);
    }


    private static void changeFontWeight(final Text text, final FontWeight weight) {
        final Font original = text.getFont();
        final Font newFont = Font.font(original.getFamily(), weight, original.getSize());
        text.setFont(newFont);
    }

    private static void incrementFontWeight(final Text text, final FontWeight fontWeight, final float sizeIncrement) {
        final Font original = text.getFont();
        final Font newFont = Font.font(original.getFamily(), fontWeight, original.getSize() + sizeIncrement);
        text.setFont(newFont);
    }


    public void draw() {
        //rootJMetro.setScene(scene);
        sceneController.setSceneFrom(this);
        this.stage.setTitle("Extended Essay Searcher");
        stage.show();
    }

    public void initStage() {
        initView();
        initLogic();
    }

    public void initLogic() {
        fieldSearchInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                performSearch(fieldSearchInput.getText(), allowRawRegex);
            }
            event.consume();
        });
        importerButton.setOnAction(event -> {
            final ImportPage importPage = injector.getInstance(ImportPage.class);
            importPage.setToPreviousPage(this::draw);
            importPage.draw();
            event.consume();
        });
        viewSearchHistory.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

    }

    public void initView() {
        // Init root
        root.setPadding(new Insets(12, 12, 12, 12));
        root.setSpacing(10);
        root.setAlignment(Pos.CENTER);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        fieldSearchInput.setMaxSize(Double.MAX_VALUE, Control.USE_PREF_SIZE);

        /// Init search history display
        viewSearchHistory.setBackground(Background.EMPTY);
        viewSearchHistory.setPadding(Insets.EMPTY);
        viewSearchHistory.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // Init search history pane
        final Label paneSearchHistoryLabel = new Label("Search History");
        final Font searchHistoryLabelFont = paneSearchHistoryLabel.getFont();
        paneSearchHistoryLabel.setFont(Font.font(searchHistoryLabelFont.getFamily(), FontWeight.BOLD, searchHistoryLabelFont.getSize()));
        paneSearchHistoryLabel.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        paneSearchHistoryLabel.setTextFill(Color.WHITE);
        paneSearchHistory.setGraphic(paneSearchHistoryLabel);
        paneSearchHistory.setText(" ");
        paneSearchHistory.setCollapsible(false);
        paneSearchHistory.setContent(viewSearchHistory);
        paneSearchHistory.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        flowSearchResults.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        flowSearchResults.setLineSpacing(1.5);
        flowSearchResults.setPadding(new Insets(5, 5, 5, 5));

        paneSearchResults.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        paneSearchResults.setContent(flowSearchResults);

        paneSearchResultsParent.setText("Results");
        paneSearchResultsParent.setTextFill(Color.RED);
        final Font searchResultsLabelFont = paneSearchResultsParent.getFont();
        paneSearchResultsParent.setFont(Font.font(searchResultsLabelFont.getFamily(), FontWeight.BOLD, searchResultsLabelFont.getSize()));
        paneSearchResultsParent.setCollapsible(false);
        paneSearchResultsParent.setContent(paneSearchResults);
        paneSearchResultsParent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);


        paneCentralView.setPadding(Insets.EMPTY);
        paneCentralView.getItems().addAll(paneSearchHistory, paneSearchResultsParent);
        paneCentralView.setDividerPositions(0.25f, 0.75f);
        paneCentralView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.root.heightProperty().addListener((obs, oldVal, newVal) -> {
            double[] positions = paneCentralView.getDividerPositions(); // reccord the current ratio
            Platform.runLater(() -> paneCentralView.setDividerPositions(positions)); // apply the now former ratio
        });
        info.setPadding(Insets.EMPTY);

        progressBar.setPadding(Insets.EMPTY);
        progressBar.setMaxSize(Double.MAX_VALUE, 15);
        progressBar.setVisible(false);

        boxInfo.setSpacing(10);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        boxInfo.getChildren().addAll(importerButton, info, progressBar);
        root.getChildren().addAll(fieldSearchInput, paneCentralView, boxInfo);
    }

    private void performSearch(@NotNull final String search, boolean allowRawRegex) {
        if (search.isBlank() || searching) {
            return;
        }
        final String regex;
        if (allowRawRegex) {
            regex = String.format("'(%s)'", search.trim());
        } else {
            // Force REGEX escaping
            regex = String.format("'\\Q(%s)\\E'", search.trim());
        }
        // Remove duplicate values from the search history
        viewSearchHistory.getItems().removeIf(hl -> hl.getText().equals(search));
        // Remove this value from the search history
        historyController.removeEntry(search);
        final Hyperlink hyperlink = new Hyperlink(search);
        // Set hyperlink to perform "this" search again
        hyperlink.setOnAction(event -> performSearch(search, allowRawRegex));
        // Move the hyperlink to the top of the list
        viewSearchHistory.getItems().add(0, hyperlink);
        historyController.addEntry(search);
        // Update info text
        info.setText("Searching... ");
        searching = true;
        // Make progressbar visible
        progressBar.setVisible(true);
        // Build search query
        final QueryParameters parameters = QueryParameters.builder().regex(regex).regexFlags('i').deepSearch(false).build();
        // Perform the query asynchronously
        indexDataController.performQuery(parameters).thenAccept(results ->
                // Synchronise back to the display thread
                Platform.runLater(() -> {
                    // Reset the "Search Results column"
            this.flowSearchResults.getChildren().clear();
            if (results.isEmpty()) {
                this.flowSearchResults.getChildren().add(new Text("No Results"));
            } else {
                for (SearchResult result : results) {
                    // Append each individual result to the list view
                    processSearchResultEntries(result);
                }
            }
            progressBar.setProgress(0);
            progressBar.setVisible(false);
            searching = false;
            info.setText(" ");
        }));
    }

    private void processSearchResultEntries(@NotNull final SearchResult result) {
        final List<String> correctText = result.getMatchingText();
        // FIXME keyword highlighting
        final IndexData indexData = result.getEssay().getIndexData();
        // Parse values
        final String subject = indexData.getSubject().getDisplayName();
        final String examSession = indexData.getExamSession().displayName;
        final String elementDisplayName = String.format("%1$s | %2$s", subject, examSession);
        final String title = indexData.getTitle();
        final String researchQuestion = indexData.getResearchQuestion();

        // Begin adding elements to search history
        final TextFlow newFlow = this.flowSearchResults;

        final Text entryTitle = new Text(elementDisplayName + System.lineSeparator());
        entryTitle.setFill(Color.DARKGREEN);
        incrementFontWeight(entryTitle, FontWeight.BOLD, 4);

        // Setup Title
        final Text textEssayTitleIdentifier = new Text("Title: ");
        changeFontWeight(textEssayTitleIdentifier, FontWeight.BOLD);
        textEssayTitleIdentifier.setFill(Color.DARKGRAY);
        final Text textEssayTitle = new Text(title + System.lineSeparator());
        changeFontWeight(textEssayTitle, FontWeight.NORMAL);

        // Setup Research Question
        final Text textEssayRQIdentifier = new Text("Research Question: ");
        textEssayRQIdentifier.setFill(Color.DARKGRAY);
        changeFontWeight(textEssayRQIdentifier, FontWeight.BOLD);
        final Text textRQ = new Text(researchQuestion + System.lineSeparator());
        changeFontWeight(textRQ, FontWeight.NORMAL);

        // Update root node to display text
        newFlow.getChildren().addAll(entryTitle, textEssayTitleIdentifier, textEssayTitle, textEssayRQIdentifier, textRQ);
        for (String s : correctText) {
            newFlow.getChildren().add(new Text(s + System.lineSeparator()));
        }
        if (correctText.isEmpty()) {
            newFlow.getChildren().add(new Text(System.lineSeparator()));
        }
    }

}
