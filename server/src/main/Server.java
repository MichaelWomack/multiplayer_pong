package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server extends Application implements PongConstants {
    private int sessionNo = 1;
    private TextArea textArea = new TextArea();

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new ScrollPane(textArea));
        primaryStage.setTitle("Pong Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> textArea.appendText("Server running at port 8000\n"));

                while (true) {
                    Platform.runLater(() -> textArea.appendText(new Date()
                            + " : Waiting for players to join session " + sessionNo + '\n'));

                    Socket player1 = serverSocket.accept();
                    Platform.runLater(() -> textArea.appendText("Player 1's IP address: "
                            + player1.getInetAddress().getHostAddress() + '\n'));

                    Socket player2 = serverSocket.accept();
                    Platform.runLater(() -> textArea.appendText("Player 2's IP address: "
                            + player2.getInetAddress().getHostAddress() + '\n'));

                    Platform.runLater(() -> textArea.appendText(new Date() +
                            ": Starting a new thread for Session " + ++sessionNo + '\n'));

                    new Thread(new SessionHandler(player1, player2)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    class SessionHandler implements Runnable, PongConstants {
        private Socket player1, player2;

        private ObjectOutputStream toPlayer1;
        private ObjectInputStream fromPlayer1;
        private ObjectOutputStream toPlayer2;
        private ObjectInputStream fromPlayer2;

        public SessionHandler(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;
        }


        @Override
        public void run() {

            try {
                toPlayer1 = new ObjectOutputStream(player1.getOutputStream());
                toPlayer2 = new ObjectOutputStream(player2.getOutputStream());

                fromPlayer1 = new ObjectInputStream(player1.getInputStream());
                fromPlayer2 = new ObjectInputStream(player2.getInputStream());

                toPlayer1.writeObject(PLAYER1);
                toPlayer2.writeObject(PLAYER2);

                String p1Name = (String) fromPlayer1.readObject();
                String p2Name = (String) fromPlayer2.readObject();

                toPlayer1.writeObject(p2Name);
                toPlayer2.writeObject(p1Name);

                GameState dataFromPlayer1, dataFromPlayer2;
                int gameStatus = 0;
                while (gameStatus != PLAYER1_WON || gameStatus != PLAYER2_WON) {
                    /* Read Opponent Coordinates from both and Ball Data from Player 1 */
                    dataFromPlayer1 = (GameState) fromPlayer1.readObject();
                    dataFromPlayer2 = (GameState) fromPlayer2.readObject();

                    toPlayer1.writeObject(dataFromPlayer2);
                    toPlayer2.writeObject(dataFromPlayer1);

                    gameStatus = dataFromPlayer1.getGameStatus();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
