package me.XXX.eesearcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.stage.Stage;
import me.XXX.eesearcher.data.DataUtil;
import me.XXX.eesearcher.module.BackendModule;
import me.XXX.eesearcher.module.FrontendModule;
import me.XXX.eesearcher.ui.GuestHomepage;

import java.sql.SQLException;

public final class MainApplication extends Application {

    private static Thread HEART_BEAT;

    public static Thread getHeartBeat() {
        return HEART_BEAT;
    }

    public static boolean isPrimaryThread() {
        return Thread.currentThread() == HEART_BEAT;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        if (HEART_BEAT != null) {
            HEART_BEAT = Thread.currentThread();
        }
        final Injector injector = Guice.createInjector(com.google.inject.Stage.PRODUCTION, new BackendModule(), new FrontendModule(primaryStage));
        initBackend(injector);
        // Draw the homepage
        final GuestHomepage homepage = injector.getInstance(GuestHomepage.class);
        homepage.draw();
    }

    /**
     * Initialize the backend.
     * @param injector The injector to use for initializing
     */
    private void initBackend(Injector injector) {
        final DataUtil dataUtil = injector.getInstance(DataUtil.class);
        try {
            dataUtil.initDatabase();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

}
