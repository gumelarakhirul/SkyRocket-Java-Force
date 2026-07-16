package game.obj;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import javax.swing.ImageIcon;
import java.awt.Image;

public class HealthPack {

    public static final int HEALTH_PACK_SIZE = 30;
    private int x;
    private int y;
    private Image image;

    // --- PENAMBAHAN UNTUK LOGIKA WAKTU ---
    private long spawnTime; // Waktu saat health pack ini dibuat
    private static final long LIFESPAN = 10000; // 10000 milidetik = 10 detik
    // --- AKHIR PENAMBAHAN ---

    public HealthPack(int x, int y) {
        this.x = x;
        this.y = y;
        this.image = new ImageIcon(getClass().getResource("/game/image/health_pack.png")).getImage();

        // Catat waktu pembuatan health pack
        this.spawnTime = System.currentTimeMillis();
    }

    // Metode baru untuk memeriksa apakah health pack sudah kedaluwarsa
    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > LIFESPAN;
    }

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, HEALTH_PACK_SIZE, HEALTH_PACK_SIZE, null);
    }

    public Shape getShape() {
        return new Rectangle2D.Double(x, y, HEALTH_PACK_SIZE, HEALTH_PACK_SIZE);
    }
}