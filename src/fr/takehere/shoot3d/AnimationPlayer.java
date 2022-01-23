package fr.takehere.shoot3d;


import fr.takehere.ethereal.Game;
import fr.takehere.ethereal.GameWindow;
import fr.takehere.ethereal.utils.Vector2;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AnimationPlayer {

    public String folder;
    public List<Image> sequence = new ArrayList<>();
    public Timeline timeline;
    public double delayMs;

    public AnimationPlayer(String folderPath, double delayMs) {
        this.folder = folderPath;
        this.delayMs = delayMs;

        for (int i = 0; i < 11; i++) {
            InputStream in = getClass().getResourceAsStream("/resources/shootAnim/shoot-" + i + ".png");
            sequence.add(new Image(in));
        }
    }

    int currentIncrement = 0;
    public boolean isPlaying = false;

    public void play(){
        isPlaying = true;
        timeline = new Timeline(new KeyFrame(Duration.millis(80), event -> {
            GameWindow.frontUi.getChildren().removeIf(node -> node instanceof ImageView);

            ImageView imageView = new ImageView();
            imageView.setImage(sequence.get(currentIncrement));
            imageView.setFitHeight(500);
            imageView.setFitWidth(700);
            imageView.setTranslateY(Game.height/2 - (imageView.getFitHeight() / 2));

            GameWindow.frontUi.getChildren().add(imageView);

            currentIncrement += 1;
        }));

        timeline.setOnFinished(event -> {
            currentIncrement = 0;
            GameWindow.frontUi.getChildren().removeIf(node -> node instanceof ImageView);
            isPlaying = false;
        });

        timeline.setCycleCount(sequence.size());
        timeline.play();
    }

    public List<File> sortFiles(List<File> files){
        TreeMap<Integer, File> fileHashmap = new TreeMap<>();

        for (File file : files) {
            String fileName = file.getName();
            int pos = fileName.lastIndexOf(".");
            if (pos > 0 && pos < (fileName.length() - 1)) {
                fileName = fileName.substring(0, pos);
            }

            fileHashmap.put(Integer.valueOf(fileName.split("-")[1]), file);
        }

        return null;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
