package geometryjump;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeometryJump extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int SIDEBAR = 320;
    private static final int TILE = 40;
    private static final int PLAYER_SIZE = 32;
    private static final Font TITLE_FONT = new Font("Trebuchet MS", Font.BOLD, 42);
    private static final Font SUBTITLE_FONT = new Font("Trebuchet MS", Font.BOLD, 24);
    private static final Font BODY_FONT = new Font("Trebuchet MS", Font.PLAIN, 18);
    private static final Font SMALL_FONT = new Font("Trebuchet MS", Font.PLAIN, 14);

    private enum Screen {
        MENU,
        LEVELS,
        PLAY,
        EDITOR,
        COMMUNITY
    }

    private enum Tool {
        BLOCK,
        SPIKE,
        ORB,
        PAD,
        COIN,
        ERASE
    }

    private static final class LevelObject {
        String type;
        int col;
        int row;
        int w;
        int h;

        LevelObject(String type, int col, int row) {
            this(type, col, row, 1, 1);
        }

        LevelObject(String type, int col, int row, int w, int h) {
            this.type = type;
            this.col = col;
            this.row = row;
            this.w = w;
            this.h = h;
        }

        LevelObject copy() {
            return new LevelObject(type, col, row, w, h);
        }
    }

    private static final class Level {
        String id;
        String name;
        String author;
        String theme;
        double speed;
        double gravity;
        double jumpVelocity;
        double orbVelocity;
        double padVelocity;
        int groundRow;
        int rows;
        int cols;
        int goalCol;
        List<LevelObject> objects = new ArrayList<LevelObject>();

        Level copy() {
            Level result = new Level();
            result.id = id;
            result.name = name;
            result.author = author;
            result.theme = theme;
            result.speed = speed;
            result.gravity = gravity;
            result.jumpVelocity = jumpVelocity;
            result.orbVelocity = orbVelocity;
            result.padVelocity = padVelocity;
            result.groundRow = groundRow;
            result.rows = rows;
            result.cols = cols;
            result.goalCol = goalCol;
            for (LevelObject object : objects) {
                result.objects.add(object.copy());
            }
            return result;
        }
    }

    private static final class Player {
        double x;
        double y;
        double vy;
        boolean onGround;
        boolean alive;
        boolean won;
        int coins;
    }

    private static final class Session {
        Level level;
        Player player;
        double cameraX;
        double time;
        Set<Integer> usedOrbs = new HashSet<Integer>();
        Set<Integer> usedCoins = new HashSet<Integer>();
    }

    private static final class Box {
        double x;
        double y;
        double w;
        double h;

        Box(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private static final class UiButton {
        String id;
        Rectangle bounds;

        UiButton(String id, Rectangle bounds) {
            this.id = id;
            this.bounds = bounds;
        }
    }

    private final Timer timer;
    private final List<Level> builtinLevels = new ArrayList<Level>();
    private final List<Level> userLevels = new ArrayList<Level>();
    private final List<Level> publishedLevels = new ArrayList<Level>();
    private final List<UiButton> uiButtons = new ArrayList<UiButton>();

    private Screen screen = Screen.MENU;
    private Tool editorTool = Tool.BLOCK;
    private Level editorLevel;
    private Session session;
    private int editorCameraCol = 0;
    private boolean jumpQueued = false;
    private String status = "Ready.";

    public GeometryJump() {
        setFocusable(true);
        setBackground(new Color(13, 19, 33));
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        builtinLevels.add(createPulseRun());
        editorLevel = createBlankLevel();

        timer = new Timer(16, this);
        timer.start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Geometry Jump");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new GeometryJump());
        frame.setSize(WIDTH, HEIGHT);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private Level createPulseRun() {
        Level level = createBlankLevel();
        level.id = "pulse-run";
        level.name = "Pulse Run";
        level.author = "Geometry Jump Team";
        level.objects.add(new LevelObject("block", 11, 8, 2, 1));
        level.objects.add(new LevelObject("spike", 14, 9));
        level.objects.add(new LevelObject("orb", 16, 6));
        level.objects.add(new LevelObject("block", 18, 7, 3, 1));
        level.objects.add(new LevelObject("coin", 19, 5));
        level.objects.add(new LevelObject("pad", 24, 9));
        level.objects.add(new LevelObject("block", 28, 6, 2, 1));
        level.objects.add(new LevelObject("spike", 31, 9));
        level.objects.add(new LevelObject("spike", 32, 9));
        level.objects.add(new LevelObject("block", 35, 8, 4, 1));
        level.objects.add(new LevelObject("coin", 37, 6));
        level.objects.add(new LevelObject("orb", 40, 5));
        level.objects.add(new LevelObject("block", 44, 7, 3, 1));
        level.objects.add(new LevelObject("pad", 50, 9));
        level.objects.add(new LevelObject("spike", 54, 9));
        level.objects.add(new LevelObject("block", 57, 6, 2, 1));
        level.objects.add(new LevelObject("coin", 58, 4));
        level.objects.add(new LevelObject("orb", 61, 5));
        level.objects.add(new LevelObject("block", 65, 8, 3, 1));
        level.objects.add(new LevelObject("spike", 69, 9));
        return level;
    }

    private Level createBlankLevel() {
        Level level = new Level();
        level.id = "untitled-pulse";
        level.name = "Untitled Pulse";
        level.author = "Local Creator";
        level.theme = "sunset-drive";
        level.speed = 300;
        level.gravity = 1900;
        level.jumpVelocity = 720;
        level.orbVelocity = 760;
        level.padVelocity = 880;
        level.groundRow = 10;
        level.rows = 12;
        level.cols = 80;
        level.goalCol = 72;
        return level;
    }

    private void goTo(Screen next) {
        screen = next;
        if (next != Screen.PLAY) {
            session = null;
        }
        if (next == Screen.MENU) {
            status = "Home screen ready.";
        } else if (next == Screen.LEVELS) {
            status = "Choose a level.";
        } else if (next == Screen.EDITOR) {
            status = "Editor ready. Left click to place, right click to erase.";
        } else if (next == Screen.COMMUNITY) {
            status = "Community mock uses local app memory only.";
        }
        repaint();
    }

    private List<Level> allLevels() {
        List<Level> levels = new ArrayList<Level>();
        levels.addAll(builtinLevels);
        levels.addAll(userLevels);
        return levels;
    }

    private void startLevel(Level source) {
        Level level = source.copy();
        Player player = new Player();
        player.x = 3 * TILE;
        player.y = (level.groundRow - 1) * TILE - PLAYER_SIZE;
        player.vy = 0;
        player.onGround = true;
        player.alive = true;
        player.won = false;
        player.coins = 0;

        session = new Session();
        session.level = level;
        session.player = player;
        session.cameraX = 0;
        session.time = 0;
        screen = Screen.PLAY;
        status = "Playing " + level.name + ". Jump with Space, W, Up, or click.";
    }

    private void restartLevel() {
        if (session != null) {
            startLevel(session.level);
        }
    }

    private List<Box> getSolids(Level level) {
        List<Box> solids = new ArrayList<Box>();
        solids.add(new Box(0, level.groundRow * TILE, level.cols * TILE, (level.rows - level.groundRow) * TILE));
        for (LevelObject object : level.objects) {
            if ("block".equals(object.type)) {
                solids.add(new Box(object.col * TILE, object.row * TILE, object.w * TILE, object.h * TILE));
            }
        }
        return solids;
    }

    private Box levelRect(LevelObject object) {
        return new Box(object.col * TILE, object.row * TILE, object.w * TILE, object.h * TILE);
    }

    private boolean intersects(Box a, Box b) {
        return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    private void queueJump() {
        if (session == null) {
            return;
        }
        Player player = session.player;
        Level level = session.level;
        if (!player.alive || player.won) {
            return;
        }
        if (player.onGround) {
            player.vy = -level.jumpVelocity;
            player.onGround = false;
            status = "Jump.";
            return;
        }
        for (int i = 0; i < level.objects.size(); i++) {
            LevelObject object = level.objects.get(i);
            if (!"orb".equals(object.type) || session.usedOrbs.contains(i)) {
                continue;
            }
            double orbX = object.col * TILE + (TILE / 2.0);
            double orbY = object.row * TILE + (TILE / 2.0);
            double playerX = player.x + (PLAYER_SIZE / 2.0);
            double playerY = player.y + (PLAYER_SIZE / 2.0);
            double dx = orbX - playerX;
            double dy = orbY - playerY;
            if ((dx * dx) + (dy * dy) <= (58 * 58)) {
                player.vy = -level.orbVelocity;
                session.usedOrbs.add(i);
                status = "Orb boost.";
                return;
            }
        }
    }

    private void killPlayer(String message) {
        if (session == null) {
            return;
        }
        session.player.alive = false;
        status = message;
    }

    private void updatePlay(double dt) {
        if (session == null) {
            return;
        }
        Level level = session.level;
        Player player = session.player;
        if (!player.alive || player.won) {
            return;
        }

        if (jumpQueued) {
            queueJump();
        }

        double prevY = player.y;
        player.x += level.speed * dt;
        player.vy += level.gravity * dt;
        player.y += player.vy * dt;
        player.onGround = false;

        for (Box solid : getSolids(level)) {
            Box playerBox = new Box(player.x, player.y, PLAYER_SIZE, PLAYER_SIZE);
            if (!intersects(playerBox, solid)) {
                continue;
            }
            double prevBottom = prevY + PLAYER_SIZE;
            if (player.vy >= 0 && prevBottom <= solid.y + 8) {
                player.y = solid.y - PLAYER_SIZE;
                player.vy = 0;
                player.onGround = true;
            } else {
                killPlayer("Crashed into a block.");
                return;
            }
        }

        for (int i = 0; i < level.objects.size(); i++) {
            LevelObject object = level.objects.get(i);
            Box playerBox = new Box(player.x, player.y, PLAYER_SIZE, PLAYER_SIZE);
            Box rect = levelRect(object);
            if ("spike".equals(object.type) && intersects(playerBox, rect)) {
                killPlayer("Spike hit.");
                return;
            }
            if ("pad".equals(object.type) && intersects(playerBox, rect) && player.vy >= 0 && prevY + PLAYER_SIZE <= rect.y + 12) {
                player.y = rect.y - PLAYER_SIZE;
                player.vy = -level.padVelocity;
                player.onGround = false;
                status = "Pad launch.";
            }
            if ("coin".equals(object.type) && !session.usedCoins.contains(i) && intersects(playerBox, rect)) {
                session.usedCoins.add(i);
                player.coins += 1;
                status = "Coins: " + player.coins;
            }
        }

        if (player.y > HEIGHT + 180) {
            killPlayer("Fell out of the level.");
            return;
        }

        if (player.x >= level.goalCol * TILE) {
            player.won = true;
            status = "Level clear: " + level.name;
        }

        session.cameraX = Math.max(0, player.x - 220);
        session.time += dt;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        uiButtons.clear();

        if (screen == Screen.PLAY && session != null) {
            drawBackground(g, session.cameraX);
            drawWorld(g, session.level, session.cameraX, session);
            drawPlayer(g, session);
            drawHud(g);
        } else if (screen == Screen.EDITOR) {
            drawBackground(g, editorCameraCol * TILE);
            drawWorld(g, editorLevel, editorCameraCol * TILE, null);
            drawEditorSidebar(g);
        } else {
            drawBackground(g, 180);
            drawWorld(g, builtinLevels.get(0), 320, null);
            if (screen == Screen.MENU) {
                drawMenu(g);
            } else if (screen == Screen.LEVELS) {
                drawLevelSelect(g);
            } else if (screen == Screen.COMMUNITY) {
                drawCommunity(g);
            }
        }
    }

    private void drawBackground(Graphics2D g, double cameraX) {
        GradientPaint paint = new GradientPaint(0, 0, new Color(20, 33, 61), 0, HEIGHT, new Color(247, 127, 0));
        g.setPaint(paint);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 12; i++) {
            int x = (int) ((i * 220 - (cameraX * 0.25) % 240) - 60);
            g.fillRect(x, 110 + ((i % 3) * 35), 160, 12);
        }

        g.setColor(new Color(255, 191, 105, 60));
        g.fillOval(WIDTH - 260, 60, 160, 160);
    }

    private void drawWorld(Graphics2D g, Level level, double cameraX, Session activeSession) {
        int stageWidth = WIDTH - SIDEBAR;
        int groundTop = level.groundRow * TILE;

        g.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i <= level.cols; i++) {
            int x = (int) (i * TILE - cameraX);
            g.fillRect(x, 0, 1, HEIGHT);
        }
        for (int row = 0; row <= level.rows; row++) {
            g.fillRect(0, row * TILE, stageWidth, 1);
        }

        g.setColor(new Color(35, 57, 93));
        g.fillRect(0, groundTop, stageWidth, HEIGHT - groundTop);

        g.setColor(new Color(111, 255, 233));
        g.fillRect((int) (level.goalCol * TILE - cameraX), 100, 10, groundTop - 100);

        for (int i = 0; i < level.objects.size(); i++) {
            LevelObject object = level.objects.get(i);
            int x = (int) (object.col * TILE - cameraX);
            int y = object.row * TILE;

            if ("block".equals(object.type)) {
                g.setColor(new Color(123, 223, 242));
                g.fillRect(x, y, object.w * TILE, object.h * TILE);
                g.setColor(new Color(0, 0, 0, 40));
                g.drawRect(x, y, object.w * TILE, object.h * TILE);
            }
            if ("spike".equals(object.type)) {
                g.setColor(new Color(255, 77, 109));
                int[] xs = {x, x + (TILE / 2), x + TILE};
                int[] ys = {y + TILE, y, y + TILE};
                g.fillPolygon(xs, ys, 3);
            }
            if ("orb".equals(object.type)) {
                boolean used = activeSession != null && activeSession.usedOrbs.contains(i);
                g.setColor(used ? new Color(255, 255, 255, 70) : new Color(46, 196, 182));
                g.fillOval(x + 10, y + 10, 20, 20);
                g.setColor(new Color(216, 255, 248));
                g.setStroke(new BasicStroke(3));
                g.drawOval(x + 10, y + 10, 20, 20);
                g.setStroke(new BasicStroke(1));
            }
            if ("pad".equals(object.type)) {
                g.setColor(new Color(255, 191, 105));
                g.fillRect(x + 4, y + TILE - 12, TILE - 8, 12);
            }
            if ("coin".equals(object.type)) {
                boolean used = activeSession != null && activeSession.usedCoins.contains(i);
                if (!used) {
                    g.setColor(new Color(255, 224, 102));
                    g.fillOval(x + 12, y + 12, 16, 16);
                }
            }
        }
    }

    private void drawPlayer(Graphics2D g, Session activeSession) {
        Player player = activeSession.player;
        double screenX = player.x - activeSession.cameraX;
        AffineTransform old = g.getTransform();
        g.translate(screenX + (PLAYER_SIZE / 2.0), player.y + (PLAYER_SIZE / 2.0));
        g.rotate(player.onGround ? 0 : activeSession.time * 10);
        g.setColor(player.alive ? Color.WHITE : new Color(255, 77, 109));
        g.fillRect(-(PLAYER_SIZE / 2), -(PLAYER_SIZE / 2), PLAYER_SIZE, PLAYER_SIZE);
        g.setColor(new Color(20, 33, 61));
        g.fillRect(-8, -6, 5, 5);
        g.fillRect(3, -6, 5, 5);
        g.setTransform(old);
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(20, 20, 280, 115, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(SUBTITLE_FONT);
        g.drawString(session.level.name, 38, 55);
        g.setFont(BODY_FONT);
        g.drawString("Coins: " + session.player.coins, 38, 85);
        int progress = (int) Math.min(100, (session.player.x / (session.level.goalCol * TILE)) * 100);
        g.drawString("Progress: " + progress + "%", 38, 112);

        drawButton(g, "restart", "Restart", 330, 24, 120, 42, new Color(255, 159, 28), Color.BLACK);
        drawButton(g, "play-levels", "Levels", 462, 24, 110, 42, new Color(255, 255, 255, 35), Color.WHITE);

        g.setColor(new Color(8, 12, 24, 210));
        g.fillRoundRect(WIDTH - SIDEBAR, 0, SIDEBAR, HEIGHT, 0, 0);
        g.setColor(Color.WHITE);
        g.setFont(SUBTITLE_FONT);
        g.drawString("Session", WIDTH - SIDEBAR + 26, 86);
        g.setFont(BODY_FONT);
        g.drawString(status, WIDTH - SIDEBAR + 26, 122);
        g.drawString("Click or press Space to jump.", WIDTH - SIDEBAR + 26, 152);
        g.drawString("Move current level to the editor", WIDTH - SIDEBAR + 26, 182);
        drawButton(g, "edit-current", "Send To Editor", WIDTH - SIDEBAR + 26, 212, SIDEBAR - 52, 46, new Color(46, 196, 182), Color.BLACK);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(8, 12, 24, 210));
        g.fillRoundRect(110, 110, 540, 430, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        g.drawString("Geometry Jump", 150, 185);
        g.setFont(BODY_FONT);
        g.drawString("Original multi-language starter inspired by the genre.", 150, 225);
        g.drawString("Not a literal copy of Geometry Dash assets or servers.", 150, 255);

        drawButton(g, "menu-play", "Play Demo", 150, 310, 220, 50, new Color(255, 159, 28), Color.BLACK);
        drawButton(g, "menu-editor", "Open Editor", 150, 374, 220, 50, new Color(46, 196, 182), Color.BLACK);
        drawButton(g, "menu-community", "Community", 150, 438, 220, 50, new Color(255, 255, 255, 35), Color.WHITE);

        drawRightInfoPanel(g, "Status", status,
            "Java port includes menu, level select, gameplay, editor, and a local community mock.",
            "Use the browser version in html/ for the quickest zero-setup preview.");
    }

    private void drawLevelSelect(Graphics2D g) {
        g.setColor(new Color(8, 12, 24, 210));
        g.fillRoundRect(70, 70, 600, 560, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        g.drawString("Level Select", 110, 135);

        List<Level> levels = allLevels();
        int y = 180;
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRoundRect(110, y, 520, 90, 20, 20);
            g.setColor(Color.WHITE);
            g.setFont(SUBTITLE_FONT);
            g.drawString(level.name, 130, y + 34);
            g.setFont(SMALL_FONT);
            g.drawString("By " + level.author + "  |  speed " + (int) level.speed, 130, y + 60);
            drawButton(g, "play-" + i, "Play", 470, y + 18, 70, 40, new Color(255, 159, 28), Color.BLACK);
            drawButton(g, "copy-" + i, "Edit", 550, y + 18, 70, 40, new Color(255, 255, 255, 35), Color.WHITE);
            y += 104;
        }

        drawRightInfoPanel(g, "Level Browser", status,
            "Built-in and locally saved levels show up here.",
            "Copy any level into the editor before tuning it.");
        drawButton(g, "back-home", "Back Home", WIDTH - SIDEBAR + 26, 210, SIDEBAR - 52, 46, new Color(255, 255, 255, 35), Color.WHITE);
    }

    private void drawCommunity(Graphics2D g) {
        g.setColor(new Color(8, 12, 24, 210));
        g.fillRoundRect(70, 70, 600, 560, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        g.drawString("Community Mock", 110, 135);
        g.setFont(BODY_FONT);
        g.drawString("This tab uses local runtime memory only.", 110, 172);

        List<Level> featured = new ArrayList<Level>();
        featured.addAll(builtinLevels);
        featured.addAll(publishedLevels);

        int y = 210;
        for (int i = 0; i < featured.size(); i++) {
            Level level = featured.get(i);
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRoundRect(110, y, 520, 90, 20, 20);
            g.setColor(Color.WHITE);
            g.setFont(SUBTITLE_FONT);
            g.drawString(level.name, 130, y + 34);
            g.setFont(SMALL_FONT);
            g.drawString("By " + level.author + "  |  theme " + level.theme, 130, y + 60);
            drawButton(g, "community-play-" + i, "Play", 470, y + 18, 70, 40, new Color(255, 159, 28), Color.BLACK);
            drawButton(g, "community-copy-" + i, "Import", 550, y + 18, 70, 40, new Color(255, 255, 255, 35), Color.WHITE);
            y += 104;
        }

        drawRightInfoPanel(g, "Community", status,
            "Publish from the editor to push a level into this mock feed.",
            "A real online service is still a future step.");
        drawButton(g, "back-home", "Back Home", WIDTH - SIDEBAR + 26, 210, SIDEBAR - 52, 46, new Color(255, 255, 255, 35), Color.WHITE);
    }

    private void drawEditorSidebar(Graphics2D g) {
        g.setColor(new Color(8, 12, 24, 220));
        g.fillRect(WIDTH - SIDEBAR, 0, SIDEBAR, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        g.drawString("Editor", WIDTH - SIDEBAR + 26, 70);
        g.setFont(BODY_FONT);
        g.drawString(editorLevel.name, WIDTH - SIDEBAR + 26, 108);
        g.drawString("by " + editorLevel.author, WIDTH - SIDEBAR + 26, 134);
        g.drawString("Tool: " + editorTool.name(), WIDTH - SIDEBAR + 26, 168);
        g.drawString("Camera col: " + editorCameraCol, WIDTH - SIDEBAR + 26, 194);

        drawButton(g, "tool-block", "1 Block", WIDTH - SIDEBAR + 26, 230, 130, 40, editorTool == Tool.BLOCK ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.BLOCK ? Color.BLACK : Color.WHITE);
        drawButton(g, "tool-spike", "2 Spike", WIDTH - SIDEBAR + 164, 230, 130, 40, editorTool == Tool.SPIKE ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.SPIKE ? Color.BLACK : Color.WHITE);
        drawButton(g, "tool-orb", "3 Orb", WIDTH - SIDEBAR + 26, 280, 130, 40, editorTool == Tool.ORB ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.ORB ? Color.BLACK : Color.WHITE);
        drawButton(g, "tool-pad", "4 Pad", WIDTH - SIDEBAR + 164, 280, 130, 40, editorTool == Tool.PAD ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.PAD ? Color.BLACK : Color.WHITE);
        drawButton(g, "tool-coin", "5 Coin", WIDTH - SIDEBAR + 26, 330, 130, 40, editorTool == Tool.COIN ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.COIN ? Color.BLACK : Color.WHITE);
        drawButton(g, "tool-erase", "Del Erase", WIDTH - SIDEBAR + 164, 330, 130, 40, editorTool == Tool.ERASE ? new Color(46, 196, 182) : new Color(255, 255, 255, 35), editorTool == Tool.ERASE ? Color.BLACK : Color.WHITE);

        drawButton(g, "rename-level", "Rename Level", WIDTH - SIDEBAR + 26, 392, SIDEBAR - 52, 42, new Color(255, 255, 255, 35), Color.WHITE);
        drawButton(g, "rename-author", "Rename Author", WIDTH - SIDEBAR + 26, 444, SIDEBAR - 52, 42, new Color(255, 255, 255, 35), Color.WHITE);
        drawButton(g, "playtest", "Playtest", WIDTH - SIDEBAR + 26, 506, SIDEBAR - 52, 46, new Color(255, 159, 28), Color.BLACK);
        drawButton(g, "save-level", "Save Local", WIDTH - SIDEBAR + 26, 562, SIDEBAR - 52, 42, new Color(46, 196, 182), Color.BLACK);
        drawButton(g, "publish-level", "Publish", WIDTH - SIDEBAR + 26, 614, SIDEBAR - 52, 42, new Color(255, 255, 255, 35), Color.WHITE);
        drawButton(g, "new-level", "New Blank", WIDTH - SIDEBAR + 26, 666, 130, 34, new Color(255, 255, 255, 35), Color.WHITE);
        drawButton(g, "back-home", "Home", WIDTH - SIDEBAR + 164, 666, 130, 34, new Color(255, 255, 255, 35), Color.WHITE);

        g.setFont(SMALL_FONT);
        g.drawString("A / D scroll the camera.", WIDTH - SIDEBAR + 26, 704);
    }

    private void drawRightInfoPanel(Graphics2D g, String title, String line1, String line2, String line3) {
        g.setColor(new Color(8, 12, 24, 210));
        g.fillRect(WIDTH - SIDEBAR, 0, SIDEBAR, HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        g.drawString(title, WIDTH - SIDEBAR + 26, 74);
        g.setFont(BODY_FONT);
        g.drawString(line1, WIDTH - SIDEBAR + 26, 120);
        g.drawString(line2, WIDTH - SIDEBAR + 26, 154);
        g.drawString(line3, WIDTH - SIDEBAR + 26, 188);
    }

    private void drawButton(Graphics2D g, String id, String label, int x, int y, int w, int h, Color fill, Color text) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(text);
        g.setFont(BODY_FONT);
        g.drawString(label, x + 14, y + 27);
        uiButtons.add(new UiButton(id, new Rectangle(x, y, w, h)));
    }

    private void handleButton(String id) {
        if ("menu-play".equals(id)) {
            goTo(Screen.LEVELS);
        } else if ("menu-editor".equals(id)) {
            goTo(Screen.EDITOR);
        } else if ("menu-community".equals(id)) {
            goTo(Screen.COMMUNITY);
        } else if ("back-home".equals(id)) {
            goTo(Screen.MENU);
        } else if ("restart".equals(id)) {
            restartLevel();
        } else if ("play-levels".equals(id)) {
            goTo(Screen.LEVELS);
        } else if ("edit-current".equals(id) && session != null) {
            editorLevel = session.level.copy();
            goTo(Screen.EDITOR);
        } else if ("rename-level".equals(id)) {
            String name = JOptionPane.showInputDialog(this, "Level name", editorLevel.name);
            if (name != null && !name.trim().isEmpty()) {
                editorLevel.name = name.trim();
                editorLevel.id = slugify(editorLevel.name);
                status = "Level renamed.";
            }
        } else if ("rename-author".equals(id)) {
            String author = JOptionPane.showInputDialog(this, "Author", editorLevel.author);
            if (author != null && !author.trim().isEmpty()) {
                editorLevel.author = author.trim();
                status = "Author renamed.";
            }
        } else if ("playtest".equals(id)) {
            startLevel(editorLevel);
        } else if ("save-level".equals(id)) {
            saveUserLevel(editorLevel.copy());
        } else if ("publish-level".equals(id)) {
            publishLevel(editorLevel.copy());
        } else if ("new-level".equals(id)) {
            editorLevel = createBlankLevel();
            editorTool = Tool.BLOCK;
            editorCameraCol = 0;
            status = "New blank level created.";
        } else if ("tool-block".equals(id)) {
            editorTool = Tool.BLOCK;
        } else if ("tool-spike".equals(id)) {
            editorTool = Tool.SPIKE;
        } else if ("tool-orb".equals(id)) {
            editorTool = Tool.ORB;
        } else if ("tool-pad".equals(id)) {
            editorTool = Tool.PAD;
        } else if ("tool-coin".equals(id)) {
            editorTool = Tool.COIN;
        } else if ("tool-erase".equals(id)) {
            editorTool = Tool.ERASE;
        } else if (id.startsWith("play-")) {
            int index = Integer.parseInt(id.substring("play-".length()));
            startLevel(allLevels().get(index));
        } else if (id.startsWith("copy-")) {
            int index = Integer.parseInt(id.substring("copy-".length()));
            editorLevel = allLevels().get(index).copy();
            goTo(Screen.EDITOR);
        } else if (id.startsWith("community-play-")) {
            int index = Integer.parseInt(id.substring("community-play-".length()));
            List<Level> featured = new ArrayList<Level>();
            featured.addAll(builtinLevels);
            featured.addAll(publishedLevels);
            startLevel(featured.get(index));
        } else if (id.startsWith("community-copy-")) {
            int index = Integer.parseInt(id.substring("community-copy-".length()));
            List<Level> featured = new ArrayList<Level>();
            featured.addAll(builtinLevels);
            featured.addAll(publishedLevels);
            editorLevel = featured.get(index).copy();
            goTo(Screen.EDITOR);
        }
        repaint();
    }

    private void saveUserLevel(Level level) {
        replaceById(userLevels, level);
        status = "Saved " + level.name + " locally.";
    }

    private void publishLevel(Level level) {
        replaceById(publishedLevels, level);
        status = "Published " + level.name + " to the local community mock.";
    }

    private void replaceById(List<Level> list, Level level) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(level.id)) {
                list.set(i, level);
                return;
            }
        }
        list.add(level);
    }

    private String slugify(String value) {
        String lower = value.toLowerCase();
        StringBuilder builder = new StringBuilder();
        boolean hyphen = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
                hyphen = false;
            } else if (!hyphen) {
                builder.append('-');
                hyphen = true;
            }
        }
        String result = builder.toString().replaceAll("^-+|-+$", "");
        return result.isEmpty() ? "untitled-pulse" : result;
    }

    private void placeEditorObject(int x, int y, boolean erase) {
        if (x >= WIDTH - SIDEBAR) {
            return;
        }
        int col = (x / TILE) + editorCameraCol;
        int row = y / TILE;
        if (row >= editorLevel.groundRow || col < 0 || col >= editorLevel.cols || row < 0 || row >= editorLevel.rows) {
            return;
        }

        List<LevelObject> next = new ArrayList<LevelObject>();
        for (LevelObject object : editorLevel.objects) {
            if (!(object.col == col && object.row == row)) {
                next.add(object);
            }
        }
        editorLevel.objects = next;

        if (!erase && editorTool != Tool.ERASE) {
            String type = editorTool.name().toLowerCase();
            next.add(new LevelObject(type, col, row));
        }
        status = "Edited cell " + col + ", " + row + ".";
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (screen == Screen.PLAY) {
            updatePlay(1.0 / 60.0);
        }
        jumpQueued = false;
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_UP || event.getKeyCode() == KeyEvent.VK_W) {
            jumpQueued = true;
        }
        if (screen == Screen.EDITOR) {
            if (event.getKeyCode() == KeyEvent.VK_1) editorTool = Tool.BLOCK;
            if (event.getKeyCode() == KeyEvent.VK_2) editorTool = Tool.SPIKE;
            if (event.getKeyCode() == KeyEvent.VK_3) editorTool = Tool.ORB;
            if (event.getKeyCode() == KeyEvent.VK_4) editorTool = Tool.PAD;
            if (event.getKeyCode() == KeyEvent.VK_5) editorTool = Tool.COIN;
            if (event.getKeyCode() == KeyEvent.VK_DELETE || event.getKeyCode() == KeyEvent.VK_BACK_SPACE) editorTool = Tool.ERASE;
            if (event.getKeyCode() == KeyEvent.VK_A) editorCameraCol = Math.max(0, editorCameraCol - 2);
            if (event.getKeyCode() == KeyEvent.VK_D) editorCameraCol = Math.min(editorLevel.cols - 24, editorCameraCol + 2);
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        requestFocusInWindow();
        for (UiButton button : uiButtons) {
            if (button.bounds.contains(event.getPoint())) {
                handleButton(button.id);
                return;
            }
        }
        if (screen == Screen.PLAY && event.getButton() == MouseEvent.BUTTON1) {
            jumpQueued = true;
        } else if (screen == Screen.EDITOR) {
            placeEditorObject(event.getX(), event.getY(), event.getButton() == MouseEvent.BUTTON3);
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    @Override
    public void keyTyped(KeyEvent event) {
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }

    @Override
    public void mouseDragged(MouseEvent event) {
    }

    @Override
    public void mouseMoved(MouseEvent event) {
    }
}
