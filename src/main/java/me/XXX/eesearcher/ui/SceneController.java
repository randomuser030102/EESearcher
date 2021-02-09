package me.XXX.eesearcher.ui;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Map;
import java.util.WeakHashMap;

public class SceneController {

    private final Map<Object, Scene> sceneMap = new WeakHashMap<>();
    @Inject
    @Named("main")
    private Stage stage;

    public SceneController init(Object object, final Parent rootNode) {
        sceneMap.computeIfAbsent(object, unused -> new Scene(rootNode));
        return this;
    }

    public boolean setSceneFrom(Object object) {
        final Scene scene = sceneMap.get(object);
        if (scene == null) {
            System.out.println(object);
            return false;
        }
        stage.setScene(scene);
        stage.show();
        return true;
    }

    public SceneController reset(Object object) {
        Scene scene = sceneMap.remove(object);
        if (scene != null) {
            scene.setRoot(null);
        }
        return this;
    }

    public void clear() {
        this.sceneMap.clear();
    }

}
