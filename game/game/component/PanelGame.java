package game.component;

import game.obj.Bullet;
import game.obj.Effect;
import game.obj.HealthPack;
import game.obj.Player;
import game.obj.Rocket;
import game.obj.sound.Sound;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PanelGame extends JComponent {

    private static Hashtable<String, Integer> playerScores = new Hashtable<>();
    private static boolean isLeaderboardLoaded = false;
    private final String LEADERBOARD_FILE = "leaderboard.txt";

    private Graphics2D g2;
    private BufferedImage image;
    private int width;
    private int height;
    private Thread gameLoopThread;
    private boolean start = true;
    private boolean isPause;
    private Key key;
    private int shotTime;
    private Runnable onGameOverCallback;
    private Runnable onGameWinCallback; // PENAMBAHAN: Callback untuk kondisi menang
    private boolean gameOverSaved = false;
    private boolean gameWon = false; // PENAMBAHAN: Flag untuk menandai kemenangan
    private List<BufferedImage> backgrounds = new ArrayList<>();
    private int specialSkillCount = 1;

    private final int FPS = 60;
    private final int TARGET_TIME = 1000000000 / FPS;
    private Sound sound;
    private Player player;
    private List<Bullet> bullets;
    private List<Rocket> rockets;
    private List<Effect> boomEffects;
    private List<HealthPack> healthPacks;
    private int score = 0;
    private String playerName;

    /**
     * Konstruktor untuk PanelGame.
     * @param playerName Nama pemain yang akan bermain.
     * @param onGameOverCallback Aksi (Runnable) yang akan dijalankan saat game berakhir.
     * @param onGameWinCallback Aksi (Runnable) yang akan dijalankan saat game dimenangkan.
     */
    // --- MODIFIKASI: Konstruktor diperbarui untuk menerima callback kemenangan ---
    public PanelGame(String playerName, Runnable onGameOverCallback, Runnable onGameWinCallback) {
        this.playerName = playerName;
        this.onGameOverCallback = onGameOverCallback;
        this.onGameWinCallback = onGameWinCallback; // Simpan callback kemenangan
    }

    /**
     * Memuat data leaderboard dari file leaderboard.txt ke dalam Hashtable.
     * Hanya dijalankan sekali saat game pertama kali dimulai.
     */
    private void loadLeaderboardFromFile() {
        if (isLeaderboardLoaded) return;
        File file = new File(LEADERBOARD_FILE);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String name = parts[0];
                        int scoreValue = Integer.parseInt(parts[1].trim());
                        playerScores.put(name, Math.max(playerScores.getOrDefault(name, 0), scoreValue));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
        isLeaderboardLoaded = true;
    }

    /**
     * Menyimpan seluruh data leaderboard dari Hashtable ke file leaderboard.txt.
     * Menimpa file yang sudah ada.
     */
    private void saveLeaderboardToFile() {
        try (FileWriter writer = new FileWriter(LEADERBOARD_FILE, false)) {
            for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Memperbarui skor pemain di leaderboard jika skor baru lebih tinggi, lalu menyimpan ke file.
     * @param name Nama pemain.
     * @param scoreValue Skor yang didapat.
     */
    private void saveToLeaderboard(String name, int scoreValue) {
        int currentHighScore = playerScores.getOrDefault(name, 0);
        if (scoreValue > currentHighScore) {
            playerScores.put(name, scoreValue);
        }
        saveLeaderboardToFile();
    }

    /**
     * Menghentikan semua thread dan game loop utama.
     */
    public void stop() {
        start = false;
    }

    /**
     * Metode utama untuk memulai permainan.
     * Menginisialisasi semua komponen, variabel, dan memulai semua thread yang dibutuhkan.
     */
    public void start() {
        loadLeaderboardFromFile();
        width = getWidth();
        height = getHeight();
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        loadBackgrounds();
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Game loop utama yang hanya bertanggung jawab untuk menggambar (rendering).
        gameLoopThread = new Thread(() -> {
            while (start) {
                long startTime = System.nanoTime();
                drawBackground();
                drawGame();
                render();
                long time = System.nanoTime() - startTime;
                if (time < TARGET_TIME) {
                    long sleep = (TARGET_TIME - time) / 1000000;
                    sleep(sleep);
                }
            }
        });

        initObjectGame();
        initKeyboard();
        initBullets();
        gameLoopThread.start();
    }

    /**
     * Memuat semua gambar background dari folder resources ke dalam List.
     */
    private void loadBackgrounds() {
        for (int i = 1; i <= 12; i++) {
            try {
                BufferedImage bg = ImageIO.read(getClass().getResourceAsStream("/game/image/background" + i + ".jpg"));
                backgrounds.add(bg);
            } catch (Exception e) {
                System.err.println("Gagal memuat background" + i + " : " + e.getMessage());
                backgrounds.add(null);
            }
        }
    }

    /**
     * Membuat dan menambahkan dua roket baru ke dalam permainan dari sisi kiri dan kanan.
     */
    private void addRocket() {
        if(height <= 50) return;
        Random ran = new Random();
        int locationY = ran.nextInt(height - 50) + 25;
        Rocket rocket = new Rocket();
        rocket.changeLocation(0, locationY);
        rocket.changeAngle(0);
        rockets.add(rocket);
        int locationY2 = ran.nextInt(height - 50) + 25;
        Rocket rocket2 = new Rocket();
        rocket2.changeLocation(width, locationY2);
        rocket2.changeAngle(180);
        rockets.add(rocket2);
    }

    /**
     * Membuat dan menambahkan health pack baru di lokasi acak.
     */
    private void addHealthPack() {
        if(width <= HealthPack.HEALTH_PACK_SIZE || height <= HealthPack.HEALTH_PACK_SIZE) return;
        Random ran = new Random();
        int locationX = ran.nextInt(width - HealthPack.HEALTH_PACK_SIZE);
        int locationY = ran.nextInt(height - HealthPack.HEALTH_PACK_SIZE);
        healthPacks.add(new HealthPack(locationX, locationY));
    }

    /**
     * Menginisialisasi semua objek utama dalam game seperti player, sound,
     * dan memulai thread terpisah untuk spawning roket dan health pack.
     */
    private void initObjectGame() {
        sound = new Sound();
        player = new Player();
        player.changeLocation(150, 150);

        rockets = new CopyOnWriteArrayList<>();
        boomEffects = new CopyOnWriteArrayList<>();
        healthPacks = new CopyOnWriteArrayList<>();

        new Thread(() -> {
            while (start) {
                if (!isPause && player.isAlive()) {
                    addRocket();
                }
                int delay = Math.max(500, 3000 - (score * 100));
                sleep(delay);
            }
        }).start();

        new Thread(() -> {
            while (start) {
                int delay = 20000 + new Random().nextInt(15000);
                sleep(delay);
                if (!isPause && player.isAlive()) {
                    addHealthPack();
                }
            }
        }).start();
    }

    /**
     * Mereset state permainan ke kondisi awal untuk memulai game baru.
     */
    private void resetGame() {
        score = 0;
        specialSkillCount = 1;
        rockets.clear();
        bullets.clear();
        healthPacks.clear();
        boomEffects.clear();
        player.changeLocation(150, 150);
        player.reset();
        gameOverSaved = false;
        gameWon = false; // PENAMBAHAN: Reset status kemenangan
        shotTime = 0;
    }

    /**
     * Mengaktifkan skill spesial: menghancurkan roket di sekitar pemain.
     */
    private void useSpecialSkill() {
        if (!player.isAlive()) return;
        double px = player.getCenterX();
        double py = player.getCenterY();

        boomEffects.add(new Effect(px, py, 30, 30, 420, 0.3f, Color.YELLOW));
        boomEffects.add(new Effect(px, py, 40, 40, 440, 0.5f, Color.ORANGE));
        boomEffects.add(new Effect(px, py, 50, 50, 460, 0.7f, Color.RED));

        List<Rocket> sortedRockets = new ArrayList<>(rockets);
        sortedRockets.sort(Comparator.comparingDouble(r -> distance(px, py, r.getX(), r.getY())));

        new Thread(() -> {
            for (Rocket rocket : sortedRockets) {
                if (rockets.contains(rocket)) {
                    destroyRocket(rocket, true);
                }
                sleep(40);
            }
        }).start();

        sound.soundDestroy();
    }

    /**
     * Menghitung jarak antara dua titik (x1, y1) dan (x2, y2).
     */
    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /**
     * Menghancurkan sebuah roket dan menampilkan efek ledakan.
     * @param rocket Objek roket yang akan dihancurkan.
     * @param fromSkill Menandakan apakah roket hancur karena skill (true) atau tembakan (false).
     */
    private void destroyRocket(Rocket rocket, boolean fromSkill) {
        rockets.remove(rocket);
        sound.soundDestroy();
        double x = rocket.getX() + Rocket.ROCKET_SIZE / 2;
        double y = rocket.getY() + Rocket.ROCKET_SIZE / 2;
        boomEffects.add(new Effect(x, y, 5, 5, 75, 0.05f, new Color(32, 178, 169)));
        boomEffects.add(new Effect(x, y, 10, 10, 100, 0.3f, new Color(230, 207, 105)));
        boomEffects.add(new Effect(x, y, 10, 5, 100, 0.5f, new Color(255, 70, 70)));
        boomEffects.add(new Effect(x, y, 10, 5, 150, 0.2f, new Color(255, 255, 255)));

        if (!fromSkill) {
            score++;
            if (score > 0 && score % 10 == 0) {
                specialSkillCount++;
            }
        }
    }

    /**
     * Aksi yang dijalankan saat game dijeda dan pemain memilih kembali ke menu.
     */
    private void pauseAndGoToMenu() {
        if (!gameOverSaved) {
            saveToLeaderboard(playerName, score);
            gameOverSaved = true;
        }
        stop();
        if (onGameOverCallback != null) {
            onGameOverCallback.run();
        }
    }

    /**
     * Mengecek tabrakan antara pemain dan health pack.
     */
    private void checkPlayerHealthPackCollision() {
        for (HealthPack pack : healthPacks) {
            if (player.isAlive() && player.getShape().getBounds2D().intersects(pack.getShape().getBounds2D())) {
                Area playerArea = new Area(player.getShape());
                playerArea.intersect(new Area(pack.getShape()));
                if (!playerArea.isEmpty()) {
                    player.restoreHealth();
                    healthPacks.remove(pack);
                    break;
                }
            }
        }
    }

    /**
     * Mengatur semua listener keyboard untuk input pemain (pergerakan, menembak, pause, dll).
     * Juga memulai thread terpisah untuk menangani logika pergerakan dan penembakan.
     */
    private void initKeyboard() {
        key = new Key();
        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (isPause) {
                    if (code == KeyEvent.VK_ESCAPE) isPause = false;
                    else if (code == KeyEvent.VK_ENTER) pauseAndGoToMenu();
                    return;
                }
                if (player.isAlive()) {
                    if (code == KeyEvent.VK_A) key.setKey_left(true);
                    else if (code == KeyEvent.VK_D) key.setKey_right(true);
                    else if (code == KeyEvent.VK_W) key.setKey_w(true);
                    else if (code == KeyEvent.VK_J) key.setKey_j(true);
                    else if (code == KeyEvent.VK_K) key.setKey_k(true);
                    else if (code == KeyEvent.VK_ESCAPE) isPause = true;
                    else if (code == KeyEvent.VK_L) {
                        if (specialSkillCount > 0) {
                            useSpecialSkill();
                            specialSkillCount--;
                        }
                    }
                } else {
                    if (code == KeyEvent.VK_R) {
                        resetGame();
                    } else if (code == KeyEvent.VK_ENTER) {
                        if (!gameOverSaved) {
                            saveToLeaderboard(playerName, score);
                            gameOverSaved = true;
                        }
                        if (onGameOverCallback != null) {
                            onGameOverCallback.run();
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) key.setKey_left(false);
                else if (e.getKeyCode() == KeyEvent.VK_D) key.setKey_right(false);
                else if (e.getKeyCode() == KeyEvent.VK_W) key.setKey_w(false);
                else if (e.getKeyCode() == KeyEvent.VK_J) key.setKey_j(false);
                else if (e.getKeyCode() == KeyEvent.VK_K) key.setKey_k(false);
            }
        });

        // Thread terpisah untuk menangani logika pergerakan dan penembakan.
        // Ini adalah logika asli permainan Anda.
        new Thread(() -> {
            float s = 0.5f;
            while (start) {
                if (!isPause) {
                    if (player.isAlive()) {
                        float angle = player.getAngle();
                        if (key.isKey_left()) angle -= s;
                        if (key.isKey_right()) angle += s;

                        if (key.isKey_j() || key.isKey_k()) {
                            if (shotTime == -1) {
                                if (key.isKey_j()) {
                                    bullets.add(0, new Bullet(player.getX(), player.getY(), player.getAngle(), 5, 4f));
                                    shotTime = 40;
                                } else {
                                    bullets.add(0, new Bullet(player.getX(), player.getY(), player.getAngle(), 20, 2f));
                                }
                                sound.soundShoot();
                            }
                            shotTime++;
                            if (shotTime == 60) {
                                shotTime = -1;
                            }
                        } else {
                            shotTime = 0;
                        }

                        if (key.isKey_w()) player.speedUp();
                        else player.speedDown();

                        player.update();
                        player.changeAngle(angle);
                        player.constrainToScreen(width, height);
                        checkPlayerHealthPackCollision();
                    }
                    for (Rocket rocket : rockets) {
                        rocket.update();
                        if (!rocket.check(width, height)) {
                            rockets.remove(rocket);
                        } else if (player.isAlive()) {
                            checkPlayer(rocket);
                        }
                    }
                }
                sleep(5);
            }
        }).start();
    }

    /**
     * Memulai thread terpisah untuk meng-update posisi peluru, efek ledakan, dan
     * memeriksa apakah health pack sudah kedaluwarsa.
     */
    private void initBullets() {
        bullets = new CopyOnWriteArrayList<>();
        new Thread(() -> {
            while (start) {
                if (!isPause) {
                    for (Bullet bullet : bullets) {
                        bullet.update();
                        if (checkBullets(bullet) || !bullet.check(width, height)) {
                            bullets.remove(bullet);
                        }
                    }
                    for (Effect boomEffect : boomEffects) {
                        boomEffect.update();
                        if (!boomEffect.check()) {
                            boomEffects.remove(boomEffect);
                        }
                    }
                    for (HealthPack pack : healthPacks) {
                        if (pack.isExpired()) {
                            healthPacks.remove(pack);
                        }
                    }
                }
                sleep(1);
            }
        }).start();
    }

    /**
     * Mengecek tabrakan antara satu peluru dengan semua roket di layar.
     * @param bullet Objek peluru yang akan dicek.
     * @return true jika peluru mengenai sesuatu, false jika tidak.
     */
    private boolean checkBullets(Bullet bullet) {
        for (Rocket rocket : rockets) {
            if (bullet.getShape().getBounds2D().intersects(rocket.getShape().getBounds2D())) {
                Area bulletArea = new Area(bullet.getShape());
                bulletArea.intersect(new Area(rocket.getShape()));
                if (!bulletArea.isEmpty()) {
                    bullets.remove(bullet);
                    boomEffects.add(new Effect(
                            bullet.getCenterX(), bullet.getCenterY(),
                            3, 5, 60, 0.5f, new Color(230, 207, 105)
                    ));

                    if (!rocket.updateHP(bullet.getSize())) {
                        destroyRocket(rocket, false);
                    } else {
                        sound.soundHit();
                    }
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Mengecek tabrakan antara pemain dengan satu roket.
     * @param rocket Objek roket yang akan dicek.
     */
    private void checkPlayer(Rocket rocket) {
        if (player.isAlive() && player.getShape().getBounds2D().intersects(rocket.getShape().getBounds2D())) {
            Area playerArea = new Area(player.getShape());
            playerArea.intersect(new Area(rocket.getShape()));
            if (!playerArea.isEmpty()) {
                double rocketHp = rocket.getHP();
                if (!rocket.updateHP(player.getHP())) {
                    destroyRocket(rocket, true);
                }
                if (!player.updateHP(rocketHp)) {
                    player.setAlive(false);
                    sound.soundDestroy();
                    double x = player.getCenterX();
                    double y = player.getCenterY();
                    boomEffects.add(new Effect(x, y, 15, 15, 100, 0.3f, new Color(230, 207, 105)));
                    boomEffects.add(new Effect(x, y, 15, 15, 150, 0.5f, new Color(255, 70, 70)));
                    boomEffects.add(new Effect(x, y, 20, 20, 200, 0.2f, new Color(255, 255, 255)));
                }
            }
        }
    }

    /**
     * Menggambar background permainan sesuai dengan level skor.
     */
    private void drawBackground() {
        if(g2 == null) return;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        int index = Math.min(score / 10, backgrounds.size() - 1);
        if (index >= 0) {
            BufferedImage bg = backgrounds.get(index);
            if (bg != null) {
                g2.drawImage(bg, 0, 0, width, height, null);
            }
        }
    }

    /**
     * Menggambar semua objek game (player, peluru, roket, dll) dan HUD (skor, level).
     */
    private void drawGame() {
        if(g2 == null || gameWon) return; // Jangan menggambar jika sudah menang

        if (player.isAlive()) {
            player.draw(g2);
        }
        for (Bullet bullet : bullets) {
            bullet.draw(g2);
        }
        for (Rocket rocket : rockets) {
            rocket.draw(g2);
        }
        for (HealthPack pack : healthPacks) {
            pack.draw(g2);
        }
        for (Effect boomEffect : boomEffects) {
            boomEffect.draw(g2);
        }

        Font hudFont = new Font("Arial", Font.BOLD, 24);
        g2.setFont(hudFont);
        FontMetrics fm = g2.getFontMetrics();

        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 20, 40);

        int level = (score / 10) + 1;
        String levelText = "Level: " + level;
        int levelTextWidth = fm.stringWidth(levelText);
        g2.drawString(levelText, (width - levelTextWidth) / 2, 40);

        // --- AWAL MODIFIKASI: LOGIKA KEMENANGAN ---
        if (level >= 10 && !gameWon) {
            gameWon = true;
            isPause = true;
            saveToLeaderboard(playerName, score);
            stop();
            if (onGameWinCallback != null) {
                onGameWinCallback.run();
            }
            return;
        }
        // --- AKHIR MODIFIKASI ---

        g2.setColor(Color.YELLOW);
        String skillText = "Skill: " + specialSkillCount;
        int skillTextWidth = fm.stringWidth(skillText);
        g2.drawString(skillText, width - skillTextWidth - 20, 40);

        if (!player.isAlive()) {
            if (!gameOverSaved) {
                saveToLeaderboard(playerName, score);
                gameOverSaved = true;
            }
            // --- MODIFIKASI: Pesan diubah ke Bahasa Inggris ---
            String text = "GAME OVER";
            String textKey = "Press [R] to Restart / [Enter] for Menu";
            drawMessageScreen(text, textKey, false);
        }
        if (isPause && !gameWon) {
            // --- MODIFIKASI: Pesan diubah ke Bahasa Inggris ---
            String text = "GAME PAUSED";
            String textKey = "Press [ESC] to Resume / [Enter] for Menu";
            drawMessageScreen(text, textKey, true);
        }
    }

    /**
     * Menggambar layar pesan seperti "GAME OVER" atau "GAME PAUSED".
     * @param title Judul pesan.
     * @param subtitle Sub-judul atau instruksi.
     * @param isPaused Menentukan apakah layar digelapkan (untuk mode jeda).
     */
    private void drawMessageScreen(String title, String subtitle, boolean isPaused) {
        if(g2 == null) return;
        if (isPaused) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, width, height);
        }
        Font largeFont = new Font("sansserif", Font.BOLD, 50);
        Font smallFont = new Font("sansserif", Font.BOLD, 15);

        g2.setFont(largeFont);
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D r2 = fm.getStringBounds(title, g2);
        double x = (width - r2.getWidth()) / 2;
        double y = (height - r2.getHeight()) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(title, (int) x, (int) y + fm.getAscent());

        g2.setFont(smallFont);
        fm = g2.getFontMetrics();
        r2 = fm.getStringBounds(subtitle, g2);
        x = (width - r2.getWidth()) / 2;
        y += 50;
        g2.drawString(subtitle, (int) x, (int) y + fm.getAscent());
    }

    /**
     * Menampilkan gambar yang sudah digambar di buffer ke layar.
     */
    private void render() {
        Graphics g = getGraphics();
        if (g != null && image != null) {
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }
    }

    /**
     * Helper method untuk menghentikan thread sementara (sleep).
     */
    private void sleep(long speed) {
        try {
            Thread.sleep(speed);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}