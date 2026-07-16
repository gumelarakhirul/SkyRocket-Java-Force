package game.component;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

public class VideoPlayer extends JFrame {

    private final EmbeddedMediaPlayer mediaPlayer;
    private boolean videoIsStopped = false;

    // Konstruktor utama untuk memutar video fullscreen dan jalankan callback setelah selesai
    public VideoPlayer(String videoResourcePath, String windowTitle, Runnable onFinish) {
        setTitle(windowTitle);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        Canvas videoCanvas = new Canvas();
        videoCanvas.setBackground(Color.black);
        setLayout(new BorderLayout());
        add(videoCanvas, BorderLayout.CENTER);

        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");
        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
        mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.videoSurface().set(mediaPlayerFactory.videoSurfaces().newVideoSurface(videoCanvas));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ESCAPE) {
                    SwingUtilities.invokeLater(() -> stopAndExit(onFinish));
                }
            }
        });

        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> stopAndExit(onFinish));
            }

            @Override
            public void error(MediaPlayer mp) {
                System.err.println("Terjadi error saat memutar video.");
                SwingUtilities.invokeLater(() -> stopAndExit(onFinish));
            }
        });

        setFocusable(true);
        pack();
        setVisible(true);
        requestFocusInWindow();
        playVideo(videoResourcePath, onFinish);
    }

    // Memutar video dari resource path
    private void playVideo(String videoResourcePath, Runnable onFinish) {
        URL videoUrl = getClass().getResource(videoResourcePath);
        if (videoUrl == null) {
            System.err.println("File video tidak ditemukan: " + videoResourcePath);
            SwingUtilities.invokeLater(() -> stopAndExit(onFinish));
            return;
        }

        try {
            String mediaPath = new File(videoUrl.toURI()).getAbsolutePath();
            mediaPlayer.media().play(mediaPath);
        } catch (Exception e) {
            System.err.println("Gagal mengonversi path video: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> stopAndExit(onFinish));
        }
    }

    // Menghentikan video dan menutup jendela, lalu menjalankan callback
    private void stopAndExit(Runnable onFinish) {
        if (videoIsStopped) return;
        videoIsStopped = true;

        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().stop();
        }
        mediaPlayer.release();
        dispose();

        if (onFinish != null) {
            onFinish.run();
        }
    }
}
