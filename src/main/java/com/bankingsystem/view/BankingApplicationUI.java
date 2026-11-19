package com.bankingsystem.view;

import com.bankingsystem.dao.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BankingApplicationUI extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer.initialize();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/banking-system.fxml"));
        Scene scene = new Scene(loader.load(), 1160, 740);
        stage.setTitle("FNBB Banking System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.show();
    }
}
