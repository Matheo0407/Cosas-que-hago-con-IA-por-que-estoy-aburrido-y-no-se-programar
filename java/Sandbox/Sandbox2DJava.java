import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sandbox2DJava extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
    static final int CELL = 8;
    static final int GRID_W = 120;
    static final int GRID_H = 70;
    static final int MENU_H = 140;
    static final int WIDTH = GRID_W * CELL;
    static final int HEIGHT = GRID_H * CELL + MENU_H;

    enum Element {
        EMPTY("Vacío", new Color(18, 18, 24), false, "Herramientas"),
        STONE("Piedra", new Color(120, 120, 130), true, "Naturaleza"),
        DIRT("Tierra", new Color(120, 80, 45), true, "Naturaleza"),
        WOOD("Madera", new Color(140, 95, 45), true, "Naturaleza"),
        WATER("Agua", new Color(70, 130, 235), false, "Naturaleza"),
        LEAF("Hojas", new Color(50, 160, 70), true, "Naturaleza"),
        BOMB("Bomba", new Color(235, 70, 70), true, "Explosivos"),
        NUKE("Bomba Nuclear", new Color(255, 210, 60), true, "Explosivos"),
        HUMAN_WASD("Humano WASD", new Color(255, 225, 190), true, "Vida"),
        HUMAN_ARROWS("Humano Flechas", new Color(190, 225, 255), true, "Vida");

        final String label;
        final Color color;
        final boolean solid;
        final String category;

        Element(String label, Color color, boolean solid, String category) {
            this.label = label;
            this.color = color;
            this.solid = solid;
            this.category = category;
        }
    }

    static class Human {
        int x, y;
        final boolean wasd;
        boolean left, right, up;
        float vy = 0;

        Human(int x, int y, boolean wasd) {
            this.x = x;
            this.y = y;
            this.wasd = wasd;
        }

        Element type() {
            return wasd ? Element.HUMAN_WASD : Element.HUMAN_ARROWS;
        }
    }

    static class Btn {
        Rectangle rect;
        String text;
        Runnable onClick;
        boolean active;

        Btn(int x, int y, int w, int h, String text, Runnable onClick) {
            this.rect = new Rectangle(x, y, w, h);
            this.text = text;
            this.onClick = onClick;
        }

        void draw(Graphics2D g) {
            g.setColor(active ? new Color(95, 120, 255) : new Color(55, 60, 72));
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);
            g.setColor(Color.WHITE);
            g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);
            FontMetrics fm = g.getFontMetrics();
            int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
            int ty = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(text, tx, ty);
        }
    }

    Element[][] grid = new Element[GRID_H][GRID_W];
    java.util.List<Human> humans = new ArrayList<>();
    java.util.List<Btn> categoryButtons = new ArrayList<>();
    java.util.List<Btn> elementButtons = new ArrayList<>();
    java.util.List<Btn> actionButtons = new ArrayList<>();

    String currentCategory = "Naturaleza";
    Element selected = Element.DIRT;
    boolean placing = false;
    int mouseX, mouseY;
    javax.swing.Timer timer;
    Random rng = new Random();

    public Sandbox2DJava() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);

        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) grid[y][x] = Element.EMPTY;
        }

        buildButtons();
        timer = new javax.swing.Timer(16, this);
        timer.start();
    }

    void buildButtons() {
        categoryButtons.clear();
        elementButtons.clear();
        actionButtons.clear();

        String[] cats = {"Naturaleza", "Explosivos", "Vida", "Herramientas"};
        int cx = 10;
        int cy = GRID_H * CELL + 10;
        for (String cat : cats) {
            final String chosen = cat;
            Btn b = new Btn(cx, cy, 130, 32, cat, () -> {
                currentCategory = chosen;
                rebuildElementButtons();
            });
            categoryButtons.add(b);
            cx += 140;
        }

        int ay = GRID_H * CELL + 96;
        actionButtons.add(new Btn(10, ay, 120, 32, "Limpiar", this::clearWorld));
        actionButtons.add(new Btn(140, ay, 120, 32, "Guardar", this::saveWorld));
        actionButtons.add(new Btn(270, ay, 120, 32, "Cargar", this::loadWorld));
        actionButtons.add(new Btn(400, ay, 160, 32, "Exportar JSON", this::exportWorld));

        rebuildElementButtons();
    }

    void rebuildElementButtons() {
        elementButtons.clear();
        int ex = 10;
        int ey = GRID_H * CELL + 52;
        for (Element e : Element.values()) {
            if (e.category.equals(currentCategory)) {
                Btn b = new Btn(ex, ey, 150, 32, e.label, () -> selected = e);
                elementButtons.add(b);
                ex += 160;
            }
        }
    }

    void clearWorld() {
        humans.clear();
        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) grid[y][x] = Element.EMPTY;
        }
    }

    boolean inBounds(int x, int y) {
        return x >= 0 && x < GRID_W && y >= 0 && y < GRID_H;
    }

    boolean humanAt(int x, int y) {
        for (Human h : humans) {
            if (h.x == x && h.y == y) return true;
        }
        return false;
    }

    Human getHumanAt(int x, int y) {
        for (Human h : humans) {
            if (h.x == x && h.y == y) return h;
        }
        return null;
    }

    void placeAt(int mx, int my, boolean erase) {
        if (my >= GRID_H * CELL) return;
        int gx = mx / CELL;
        int gy = my / CELL;
        if (!inBounds(gx, gy)) return;

        if (erase) {
            grid[gy][gx] = Element.EMPTY;
            Human h = getHumanAt(gx, gy);
            if (h != null) humans.remove(h);
            return;
        }

        if (selected == Element.HUMAN_WASD || selected == Element.HUMAN_ARROWS) {
            if (!humanAt(gx, gy) && grid[gy][gx] == Element.EMPTY) {
                humans.add(new Human(gx, gy, selected == Element.HUMAN_WASD));
            }
            return;
        }

        Human h = getHumanAt(gx, gy);
        if (h != null) humans.remove(h);
        grid[gy][gx] = selected;
    }

    void explode(int cx, int cy, int radius) {
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (!inBounds(x, y)) continue;
                int dx = x - cx;
                int dy = y - cy;
                if (dx * dx + dy * dy <= radius * radius) {
                    if (rng.nextFloat() < 0.88f) grid[y][x] = Element.EMPTY;
                    Human h = getHumanAt(x, y);
                    if (h != null) humans.remove(h);
                }
            }
        }
    }

    void simulate() {
        for (int y = GRID_H - 2; y >= 0; y--) {
            for (int x = 0; x < GRID_W; x++) {
                Element e = grid[y][x];
                if (e == Element.WATER) {
                    if (grid[y + 1][x] == Element.EMPTY && !humanAt(x, y + 1)) {
                        grid[y + 1][x] = e;
                        grid[y][x] = Element.EMPTY;
                    } else {
                        int dir = rng.nextBoolean() ? -1 : 1;
                        int nx = x + dir;
                        if (inBounds(nx, y) && grid[y][nx] == Element.EMPTY && !humanAt(nx, y)) {
                            grid[y][nx] = e;
                            grid[y][x] = Element.EMPTY;
                        } else {
                            nx = x - dir;
                            if (inBounds(nx, y) && grid[y][nx] == Element.EMPTY && !humanAt(nx, y)) {
                                grid[y][nx] = e;
                                grid[y][x] = Element.EMPTY;
                            }
                        }
                    }
                } else if (e == Element.BOMB) {
                    if (y + 1 < GRID_H && grid[y + 1][x] == Element.EMPTY && !humanAt(x, y + 1)) {
                        grid[y + 1][x] = e;
                        grid[y][x] = Element.EMPTY;
                    } else if (rng.nextFloat() < 0.0015f) {
                        explode(x, y, 6);
                    }
                } else if (e == Element.NUKE) {
                    if (y + 1 < GRID_H && grid[y + 1][x] == Element.EMPTY && !humanAt(x, y + 1)) {
                        grid[y + 1][x] = e;
                        grid[y][x] = Element.EMPTY;
                    } else if (rng.nextFloat() < 0.001f) {
                        explode(x, y, 16);
                    }
                }
            }
        }

        for (Human h : humans) {
            int move = 0;
            if (h.left && !h.right) move = -1;
            if (h.right && !h.left) move = 1;
            if (move != 0) {
                int nx = h.x + move;
                if (inBounds(nx, h.y) && grid[h.y][nx] == Element.EMPTY && !humanAt(nx, h.y)) {
                    h.x = nx;
                }
            }

            boolean grounded = (h.y + 1 >= GRID_H) || grid[h.y + 1][h.x] != Element.EMPTY || humanAt(h.x, h.y + 1);
            if (grounded && h.up) h.vy = -2.8f;
            h.vy += 0.18f;
            if (h.vy > 2.5f) h.vy = 2.5f;

            int steps = Math.max(1, (int) Math.ceil(Math.abs(h.vy)));
            for (int i = 0; i < steps; i++) {
                if (h.vy > 0) {
                    if (h.y + 1 < GRID_H && grid[h.y + 1][h.x] == Element.EMPTY && !humanAt(h.x, h.y + 1)) {
                        h.y += 1;
                    } else {
                        h.vy = 0;
                        break;
                    }
                } else if (h.vy < 0) {
                    if (h.y - 1 >= 0 && grid[h.y - 1][h.x] == Element.EMPTY && !humanAt(h.x, h.y - 1)) {
                        h.y -= 1;
                    } else {
                        h.vy = 0;
                        break;
                    }
                }
            }
        }
    }

    void saveWorld() {
        writeTextFile("sandbox_save.json", worldToJson());
    }

    void exportWorld() {
        writeTextFile("sandbox_export.json", worldToJson());
    }

    void loadWorld() {
        File f = new File("sandbox_save.json");
        if (!f.exists()) return;
        try {
            String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            loadFromJson(json);
        } catch (Exception ignored) {
        }
    }

    void writeTextFile(String name, String content) {
        try (FileOutputStream fos = new FileOutputStream(name)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    String worldToJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"gridW\": ").append(GRID_W).append(",\n");
        sb.append("  \"gridH\": ").append(GRID_H).append(",\n");
        sb.append("  \"cells\": [\n");
        boolean first = true;
        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) {
                if (grid[y][x] != Element.EMPTY) {
                    if (!first) sb.append(",\n");
                    first = false;
                    sb.append("    {\"x\":").append(x)
                      .append(",\"y\":").append(y)
                      .append(",\"type\":\"").append(grid[y][x].name()).append("\"}");
                }
            }
        }
        sb.append("\n  ],\n");
        sb.append("  \"humans\": [\n");
        for (int i = 0; i < humans.size(); i++) {
            Human h = humans.get(i);
            if (i > 0) sb.append(",\n");
            sb.append("    {\"x\":").append(h.x)
              .append(",\"y\":").append(h.y)
              .append(",\"control\":\"").append(h.wasd ? "WASD" : "ARROWS").append("\"}");
        }
        sb.append("\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    void loadFromJson(String json) {
        clearWorld();
        Matcher cellMatcher = Pattern.compile("\\{\\\"x\\\":(\\d+),\\\"y\\\":(\\d+),\\\"type\\\":\\\"([A-Z_]+)\\\"\\}").matcher(json);
        while (cellMatcher.find()) {
            int x = Integer.parseInt(cellMatcher.group(1));
            int y = Integer.parseInt(cellMatcher.group(2));
            String type = cellMatcher.group(3);
            if (inBounds(x, y)) {
                try {
                    grid[y][x] = Element.valueOf(type);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        Matcher humanMatcher = Pattern.compile("\\{\\\"x\\\":(\\d+),\\\"y\\\":(\\d+),\\\"control\\\":\\\"(WASD|ARROWS)\\\"\\}").matcher(json);
        while (humanMatcher.find()) {
            int x = Integer.parseInt(humanMatcher.group(1));
            int y = Integer.parseInt(humanMatcher.group(2));
            boolean wasd = humanMatcher.group(3).equals("WASD");
            if (inBounds(x, y) && grid[y][x] == Element.EMPTY && !humanAt(x, y)) {
                humans.add(new Human(x, y, wasd));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(18, 18, 24));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) {
                Element e = grid[y][x];
                if (e != Element.EMPTY) {
                    g.setColor(e.color);
                    g.fillRect(x * CELL, y * CELL, CELL, CELL);
                }
            }
        }

        for (Human h : humans) {
            int px = h.x * CELL;
            int py = h.y * CELL;
            g.setColor(h.type().color);
            g.fillRect(px, py, CELL, CELL);
            g.setColor(Color.BLACK);
            g.fillRect(px + 2, py + 2, 1, 1);
            g.fillRect(px + 5, py + 2, 1, 1);
        }

        g.setColor(new Color(28, 33, 44));
        g.fillRect(0, GRID_H * CELL, WIDTH, MENU_H);
        g.setColor(Color.WHITE);
        g.drawLine(0, GRID_H * CELL, WIDTH, GRID_H * CELL);

        for (Btn b : categoryButtons) {
            b.active = b.text.equals(currentCategory);
            b.draw(g);
        }
        for (Btn b : elementButtons) {
            b.active = b.text.equals(selected.label);
            b.draw(g);
        }
        for (Btn b : actionButtons) {
            b.active = false;
            b.draw(g);
        }

        g.setColor(Color.WHITE);
        g.drawString("Click o arrastra para colocar. Click derecho para borrar. 1-9 cambia elemento. WASD / Flechas mueven humanos.", 10, HEIGHT - 18);
    }

    void handleUiClick(int mx, int my) {
        for (Btn b : categoryButtons) if (b.rect.contains(mx, my)) { b.onClick.run(); repaint(); return; }
        for (Btn b : elementButtons) if (b.rect.contains(mx, my)) { b.onClick.run(); repaint(); return; }
        for (Btn b : actionButtons) if (b.rect.contains(mx, my)) { b.onClick.run(); repaint(); return; }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        simulate();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        mouseX = e.getX();
        mouseY = e.getY();
        if (mouseY >= GRID_H * CELL) {
            handleUiClick(mouseX, mouseY);
            return;
        }
        placing = true;
        placeAt(mouseX, mouseY, SwingUtilities.isRightMouseButton(e));
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) { placing = false; }
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (placing) {
            placeAt(mouseX, mouseY, SwingUtilities.isRightMouseButton(e));
            repaint();
        }
    }
    @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        for (Human h : humans) {
            if (h.wasd) {
                if (code == KeyEvent.VK_A) h.left = true;
                if (code == KeyEvent.VK_D) h.right = true;
                if (code == KeyEvent.VK_W) h.up = true;
            } else {
                if (code == KeyEvent.VK_LEFT) h.left = true;
                if (code == KeyEvent.VK_RIGHT) h.right = true;
                if (code == KeyEvent.VK_UP) h.up = true;
            }
        }

        Element[] placeables = {
                Element.STONE, Element.DIRT, Element.WOOD, Element.WATER,
                Element.LEAF, Element.BOMB, Element.NUKE, Element.HUMAN_WASD, Element.HUMAN_ARROWS
        };
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
            int idx = code - KeyEvent.VK_1;
            if (idx >= 0 && idx < placeables.length) selected = placeables[idx];
        }
        if (code == KeyEvent.VK_S) saveWorld();
        if (code == KeyEvent.VK_L) loadWorld();
        if (code == KeyEvent.VK_E) exportWorld();
        if (code == KeyEvent.VK_DELETE) clearWorld();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        for (Human h : humans) {
            if (h.wasd) {
                if (code == KeyEvent.VK_A) h.left = false;
                if (code == KeyEvent.VK_D) h.right = false;
                if (code == KeyEvent.VK_W) h.up = false;
            } else {
                if (code == KeyEvent.VK_LEFT) h.left = false;
                if (code == KeyEvent.VK_RIGHT) h.right = false;
                if (code == KeyEvent.VK_UP) h.up = false;
            }
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sandbox 2D Java");
            Sandbox2DJava panel = new Sandbox2DJava();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
