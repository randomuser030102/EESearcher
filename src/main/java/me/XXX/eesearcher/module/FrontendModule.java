package me.XXX.eesearcher.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import me.XXX.eesearcher.ui.SceneController;
import org.jetbrains.annotations.NotNull;

public class FrontendModule extends AbstractModule {

    private final javafx.stage.Stage uiStage;

    public FrontendModule(@NotNull final javafx.stage.Stage uiStage) {
        this.uiStage = uiStage;
    }

    @Override
    protected void configure() {
        bind(javafx.stage.Stage.class).annotatedWith(Names.named("main")).toInstance(uiStage);
        bind(SceneController.class).asEagerSingleton();
    }
}
