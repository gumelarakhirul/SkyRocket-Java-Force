package game.main;

import game.component.VideoPlayer;
import game.component.PanelGame;
import game.component.PanelMenu;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private PanelMenu panelMenu;
    private PanelGame panelGame;

    public Main() {
        initUI();
        playIntroVideo();
    }

    // Inisialisasi tampilan utama aplikasi
    private void initUI() {
        setTitle("Sky Rocket Java Force");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        panelMenu = new PanelMenu(this, this::startGame);
        mainPanel.add(panelMenu, "menu");

        add(mainPanel);
        setVisible(true);
    }

    // Memutar video intro sebelum menampilkan menu utama
    private void playIntroVideo() {
        Runnable afterIntroAction = () -> {
            cardLayout.show(mainPanel, "menu");
            panelMenu.requestFocusInWindow();
        };
        new VideoPlayer("/game/image/History.mp4", "Game Intro", afterIntroAction);
    }

    // Menampilkan kembali menu utama setelah game selesai
    private void showMenu() {
        panelMenu.resetMenuState();
        cardLayout.show(mainPanel, "menu");
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    // Memulai permainan dengan nama pemain yang diberikan
    private void startGame(String playerName) {
        // --- AWAL PENAMBAHAN: Menambahkan callback untuk menang dan kalah ---
        panelGame = new PanelGame(playerName, this::handleGameOver, this::handleGameWin);
        // --- AKHIR PENAMBAHAN ---
        mainPanel.add(panelGame, "game");
        cardLayout.show(mainPanel, "game");
        panelGame.requestFocusInWindow();
        panelGame.start();
    }

    // Menangani logika saat permainan berakhir (kalah) dan menampilkan video akhir
    private void handleGameOver() {
        if (panelGame != null) {
            panelGame.stop();
        }
        Runnable afterEndVideoAction = this::showMenu;
        new VideoPlayer("/game/image/End_Game.mp4", "Game Over", afterEndVideoAction);
    }

    // --- AWAL PENAMBAHAN: Metode baru untuk menangani kondisi menang ---
    /**
     * Menangani logika saat permainan dimenangkan dan menampilkan video kemenangan.
     */
    private void handleGameWin() {
        if (panelGame != null) {
            panelGame.stop();
        }
        Runnable afterWinVideoAction = this::showMenu;
        // Pastikan video menang.mp4 ada di path /game/image/
        new VideoPlayer("/game/image/menang.mp4", "Victory!", afterWinVideoAction);
    }
    // --- AKHIR PENAMBAHAN ---

    // Entry point aplikasi
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}