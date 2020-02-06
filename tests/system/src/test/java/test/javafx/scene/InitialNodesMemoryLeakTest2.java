package test.javafx.scene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

// TODO Make Controls FX v11.0.0 jar available, until a local way is found to reproduce the same behaviour.
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

public class InitialNodesMemoryLeakTest2 {

    static CountDownLatch startupLatch;
    static Stage stage;
    static BorderPane borderPane;
    static GridPane gridPane;
    static WeakReference<GridPane> gridPaneWRef;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;

            borderPane = new BorderPane();
            stage.setScene(new Scene(borderPane));

            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
                startupLatch.countDown();
            });

            stage.show();
        }
    }

    @BeforeClass
    public static void initFX() {
        startupLatch = new CountDownLatch(1);
        new Thread(() -> Application.launch(Main.TestApp.class, (String[])null)).start();
        try {
            if (!startupLatch.await(15, TimeUnit.SECONDS)) {
                fail("Timeout waiting for FX runtime to start");
            }
        } catch (InterruptedException ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @Test
    public void testRootNodeMemoryLeak2() throws InterruptedException {
        Util.runAndWait(() -> {
            // TODO Replace with another way to reproduce the issue without relying on ControlsFX,
            //  unless it's ControlsFX itself at issue
            ValidationSupport validationSupport = new ValidationSupport();

            gridPane = new GridPane();

            gridPane.add(new Label("Field 1"), 0, 0);
            TextField field1 = new TextField();
            validationSupport.registerValidator(field1, Validator.createEmptyValidator("Required"));
            gridPane.add(field1, 1, 0);

            // If you comment out the 4 lines below the test passes
            gridPane.add(new Label("Field 2"), 0, 1);
            TextField field2 = new TextField();
            validationSupport.registerValidator(field2, Validator.createEmptyValidator("Required"));
            gridPane.add(field2, 1, 1);

            gridPane.add(new Label("Field 3"), 0, 2);
            TextField field3 = new TextField();
            validationSupport.registerValidator(field3, Validator.createEmptyValidator("Required"));
            gridPane.add(field3, 1, 2);

            gridPaneWRef = new WeakReference<>(gridPane);
            borderPane.setCenter(gridPane);
        });

        Util.runAndWait(() -> borderPane.setCenter(null));
        gridPane = null;

        for (int j = 0; j < 10; j++) {
            System.gc();
            System.runFinalization();

            if (gridPaneWRef.get() == null)
                break;

            Util.sleep(500);
        }
        Assert.assertNull("Couldn't collect GridPane", gridPaneWRef.get());
    }

    @AfterClass
    public static void teardownOnce() {
        Platform.runLater(() -> {
            stage.hide();
            Platform.exit();
        });
    }
}
