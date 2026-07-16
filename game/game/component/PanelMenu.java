package game.component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class PanelMenu extends JPanel {
    private JPanel mainPanel;
    private JPanel leaderboardPanel;
    private JTextField nameField;
    private JLabel labelNama;
    private Consumer<String> startGameCallback;
    private BufferedImage backgroundImage;
    private JPanel leaderboardBox;
    private JLabel errorLabel;

    public PanelMenu(JFrame frame, Consumer<String> startGameCallback) {
        this.startGameCallback = startGameCallback;
        setLayout(new CardLayout());

        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/game/image/Background Game.png"));
        } catch (IOException e) {
            // MODIFICATION: Changed error message to English
            System.err.println("Failed to load background: " + e.getMessage());
        }

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 20, 5, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridy = 0;
        mainPanel.add(Box.createRigidArea(new Dimension(0, 180)), gbc);

        JButton startButton = createImageButton("/game/image/Button_Mulai.png");
        startButton.setPreferredSize(new Dimension(400, 100));
        gbc.gridy = 1;
        mainPanel.add(startButton, gbc);

        JButton leaderboardButton = createImageButton("/game/image/Button_leaderboard.png");
        leaderboardButton.setPreferredSize(new Dimension(400, 100));
        gbc.gridy = 2;
        mainPanel.add(leaderboardButton, gbc);

        JButton exitButton = createImageButton("/game/image/Button_Keluar.png");
        exitButton.setPreferredSize(new Dimension(400, 100));
        gbc.gridy = 3;
        mainPanel.add(exitButton, gbc);

        gbc.gridy = 4;
        // MODIFICATION: Changed label text to English
        labelNama = new JLabel("Enter Your Name:");
        labelNama.setForeground(Color.WHITE);
        labelNama.setFont(new Font("Monospaced", Font.BOLD, 18));
        labelNama.setVisible(false);
        mainPanel.add(labelNama, gbc);

        gbc.gridy = 5;
        nameField = new JTextField();
        nameField.setFont(new Font("Monospaced", Font.BOLD, 22));
        nameField.setPreferredSize(new Dimension(400, 50));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setCaretColor(Color.BLUE);
        nameField.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        nameField.setVisible(false);
        mainPanel.add(nameField, gbc);

        gbc.gridy = 6;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        errorLabel.setBackground(Color.WHITE);
        errorLabel.setOpaque(false);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(errorLabel, gbc);

        nameField.addActionListener(e -> {
            String playerName = nameField.getText().trim();
            // MODIFICATION: Changed error messages to English
            if (playerName.isEmpty()) {
                showError("Name cannot be empty!");
                return;
            }
            if (playerName.length() > 20) {
                showError("Name cannot be longer than 20 characters!");
                return;
            }
            if (playerName.matches("\\d+")) {
                showError("Name cannot consist of only numbers!");
                return;
            }
            if (!playerName.matches("[a-zA-Z0-9_ ]+")) {
                showError("Name can only contain letters, numbers, spaces, and _");
                return;
            }

            hideError();
            startGameCallback.accept(playerName);
        });

        startButton.addActionListener(e -> {
            hideError();
            labelNama.setVisible(true);
            nameField.setVisible(true);
            nameField.requestFocusInWindow();
        });

        leaderboardButton.addActionListener(e -> refreshAndShowLeaderboard());
        exitButton.addActionListener(e -> frame.dispose());

        add(mainPanel, "main");
        buildLeaderboardPanel();
        add(leaderboardPanel, "leaderboard");
    }

    // Displays an error message on the error label
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setOpaque(true);
    }

    // Hides the error message
    private void hideError() {
        errorLabel.setText(" ");
        errorLabel.setOpaque(false);
    }

    // Resets the menu display to its initial state
    public void resetMenuState() {
        labelNama.setVisible(false);
        nameField.setVisible(false);
        nameField.setText("");
        hideError();
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, "main");
    }

    // Creates an image button from a file path
    private JButton createImageButton(String path) {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream(path));
            Image scaled = img.getScaledInstance(350, -1, Image.SCALE_SMOOTH);
            JButton button = new JButton(new ImageIcon(scaled));
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(350, scaled.getHeight(null)));
            return button;
        } catch (IOException | IllegalArgumentException e) {
            // MODIFICATION: Changed error message and fallback text to English
            System.err.println("Failed to load button image: " + path);
            return new JButton("Button");
        }
    }

    // Builds the leaderboard panel with its view and back button
    private void buildLeaderboardPanel() {
        leaderboardPanel = new JPanel(new GridBagLayout());
        leaderboardPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(250, 0, 20, 0);

        leaderboardBox = new JPanel(new GridLayout(0, 3, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };
        leaderboardBox.setOpaque(false);
        leaderboardBox.setPreferredSize(new Dimension(600, 300));
        leaderboardBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 3),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        leaderboardPanel.add(leaderboardBox, gbc);

        JButton backButton;
        try {
            BufferedImage backImg = ImageIO.read(getClass().getResourceAsStream("/game/image/kembali.png"));
            Image scaledBackImg = backImg.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            backButton = new JButton(new ImageIcon(scaledBackImg));
            backButton.setPreferredSize(new Dimension(400, scaledBackImg.getHeight(null)));
            backButton.setBorderPainted(false);
            backButton.setContentAreaFilled(false);
            backButton.setFocusPainted(false);
            backButton.setOpaque(false);
        } catch (IOException | IllegalArgumentException e) {
            // MODIFICATION: Changed error message and fallback text to English
            System.err.println("Failed to load back button image");
            backButton = new JButton("← Back");
            backButton.setFont(new Font("SansSerif", Font.BOLD, 16));
            backButton.setPreferredSize(new Dimension(400, 50));
        }

        gbc.gridy = 1;
        gbc.insets = new Insets(20, 0, 0, 0);
        leaderboardPanel.add(backButton, gbc);

        backButton.addActionListener(e -> {
            CardLayout cl = (CardLayout) getLayout();
            cl.show(this, "main");
        });
    }

    // Updates the leaderboard view with data from the file
    private void updateLeaderboardView() {
        leaderboardBox.removeAll();
        leaderboardBox.add(createHeaderLabel("RANK"));
        leaderboardBox.add(createHeaderLabel("NAME"));
        leaderboardBox.add(createHeaderLabel("SCORE"));

        Font rowFont = new Font("Monospaced", Font.PLAIN, 20);
        List<String[]> data = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("leaderboard.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    data.add(parts);
                }
            }
        } catch (IOException e) {
            // No action needed, will just show an empty leaderboard
        }

        Comparator<String[]> scoreComparator = Comparator.comparingInt(entry -> {
            try {
                return Integer.parseInt(entry[1].trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        });

        data.sort(scoreComparator.reversed());

        for (int i = 0; i < Math.min(5, data.size()); i++) {
            String[] entry = data.get(i);
            leaderboardBox.add(createLabel(String.valueOf(i + 1), rowFont, Color.WHITE));
            leaderboardBox.add(createLabel(entry[0], rowFont, Color.WHITE));
            leaderboardBox.add(createLabel(entry[1], rowFont, Color.CYAN));
        }

        leaderboardBox.revalidate();
        leaderboardBox.repaint();
    }

    // Shows the leaderboard panel and updates its data
    private void refreshAndShowLeaderboard() {
        updateLeaderboardView();
        CardLayout cl = (CardLayout) getLayout();
        cl.show(this, "leaderboard");
    }

    // Creates a header label for the leaderboard
    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", Font.BOLD, 26));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(new Color(30, 30, 30, 200));
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.BLUE));
        label.setPreferredSize(new Dimension(200, 40));
        return label;
    }

    // Creates a regular label for leaderboard content
    private JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    // Paints the menu background
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}