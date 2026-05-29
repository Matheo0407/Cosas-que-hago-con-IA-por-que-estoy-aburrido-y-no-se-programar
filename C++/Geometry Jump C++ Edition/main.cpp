#include "raylib.h"
#include <algorithm>
#include <cmath>
#include <string>
#include <vector>

static constexpr int WIDTH = 1280;
static constexpr int HEIGHT = 720;
static constexpr int SIDEBAR = 320;
static constexpr int TILE = 40;
static constexpr int PLAYER_SIZE = 32;

struct LevelObject {
    std::string type;
    int col;
    int row;
    int w = 1;
    int h = 1;
};

struct Level {
    std::string id;
    std::string name;
    std::string author;
    std::string theme;
    float speed;
    float gravity;
    float jumpVelocity;
    float orbVelocity;
    float padVelocity;
    int groundRow;
    int rows;
    int cols;
    int goalCol;
    std::vector<LevelObject> objects;
};

struct Player {
    float x;
    float y;
    float vy;
    bool onGround;
    bool alive;
    bool won;
    int coins;
};

struct Session {
    Level level;
    Player player;
    float cameraX = 0.0f;
    float time = 0.0f;
    std::vector<int> usedOrbs;
    std::vector<int> usedCoins;
};

enum class Screen {
    Menu,
    Levels,
    Play,
    Editor,
    Community
};

enum class Tool {
    Block,
    Spike,
    Orb,
    Pad,
    Coin,
    Erase
};

struct App {
    Screen screen = Screen::Menu;
    Tool tool = Tool::Block;
    std::string status = "C++ port ready.";
    std::vector<Level> builtin;
    std::vector<Level> user;
    std::vector<Level> published;
    Level editor;
    Session session;
    bool hasSession = false;
    int editorCameraCol = 0;
};

static Level MakeBlankLevel() {
    return {"untitled-pulse", "Untitled Pulse", "Local Creator", "sunset-drive", 300.0f, 1900.0f, 720.0f, 760.0f, 880.0f, 10, 12, 80, 72, {}};
}

static Level MakePulseRun() {
    Level level = MakeBlankLevel();
    level.id = "pulse-run";
    level.name = "Pulse Run";
    level.author = "Geometry Jump Team";
    level.objects = {
        {"block", 11, 8, 2, 1}, {"spike", 14, 9}, {"orb", 16, 6}, {"block", 18, 7, 3, 1},
        {"coin", 19, 5}, {"pad", 24, 9}, {"block", 28, 6, 2, 1}, {"spike", 31, 9},
        {"spike", 32, 9}, {"block", 35, 8, 4, 1}, {"coin", 37, 6}, {"orb", 40, 5},
        {"block", 44, 7, 3, 1}, {"pad", 50, 9}, {"spike", 54, 9}, {"block", 57, 6, 2, 1},
        {"coin", 58, 4}, {"orb", 61, 5}, {"block", 65, 8, 3, 1}, {"spike", 69, 9}
    };
    return level;
}

static bool Button(Rectangle rect, const char* label, Color fill, Color text) {
    Vector2 mouse = GetMousePosition();
    bool hot = CheckCollisionPointRec(mouse, rect);
    DrawRectangleRounded(rect, 0.25f, 8, hot ? Fade(fill, 0.92f) : fill);
    DrawText(label, static_cast<int>(rect.x + 14), static_cast<int>(rect.y + 12), 20, text);
    return hot && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
}

static bool Contains(const std::vector<int>& list, int value) {
    return std::find(list.begin(), list.end(), value) != list.end();
}

static void ReplaceById(std::vector<Level>& list, const Level& level) {
    for (auto& item : list) {
        if (item.id == level.id) {
            item = level;
            return;
        }
    }
    list.push_back(level);
}

static void StartLevel(App& app, const Level& level) {
    app.session.level = level;
    app.session.player = {3.0f * TILE, static_cast<float>((level.groundRow - 1) * TILE - PLAYER_SIZE), 0.0f, true, true, false, 0};
    app.session.cameraX = 0.0f;
    app.session.time = 0.0f;
    app.session.usedCoins.clear();
    app.session.usedOrbs.clear();
    app.hasSession = true;
    app.screen = Screen::Play;
    app.status = "Playing " + level.name;
}

static void QueueJump(App& app) {
    if (!app.hasSession) return;
    auto& level = app.session.level;
    auto& player = app.session.player;
    if (!player.alive || player.won) return;
    if (player.onGround) {
        player.vy = -level.jumpVelocity;
        player.onGround = false;
        return;
    }
    for (int i = 0; i < static_cast<int>(level.objects.size()); ++i) {
        const auto& obj = level.objects[i];
        if (obj.type != "orb" || Contains(app.session.usedOrbs, i)) continue;
        float dx = (obj.col * TILE + TILE / 2.0f) - (player.x + PLAYER_SIZE / 2.0f);
        float dy = (obj.row * TILE + TILE / 2.0f) - (player.y + PLAYER_SIZE / 2.0f);
        if (dx * dx + dy * dy <= 58.0f * 58.0f) {
            player.vy = -level.orbVelocity;
            app.session.usedOrbs.push_back(i);
            return;
        }
    }
}

static void UpdatePlay(App& app, float dt) {
    if (!app.hasSession) return;
    auto& level = app.session.level;
    auto& player = app.session.player;
    if (!player.alive || player.won) return;
    if (IsKeyPressed(KEY_SPACE) || IsKeyPressed(KEY_W) || IsKeyPressed(KEY_UP) || IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
        QueueJump(app);
    }
    float prevY = player.y;
    player.x += level.speed * dt;
    player.vy += level.gravity * dt;
    player.y += player.vy * dt;
    player.onGround = false;

    std::vector<Rectangle> solids;
    solids.push_back({0.0f, static_cast<float>(level.groundRow * TILE), static_cast<float>(level.cols * TILE), static_cast<float>((level.rows - level.groundRow) * TILE)});
    for (const auto& obj : level.objects) {
        if (obj.type == "block") {
            solids.push_back({static_cast<float>(obj.col * TILE), static_cast<float>(obj.row * TILE), static_cast<float>(obj.w * TILE), static_cast<float>(obj.h * TILE)});
        }
    }

    Rectangle playerRect = {player.x, player.y, static_cast<float>(PLAYER_SIZE), static_cast<float>(PLAYER_SIZE)};
    for (const auto& solid : solids) {
        if (!CheckCollisionRecs(playerRect, solid)) continue;
        if (player.vy >= 0 && prevY + PLAYER_SIZE <= solid.y + 8) {
            player.y = solid.y - PLAYER_SIZE;
            player.vy = 0;
            player.onGround = true;
        } else {
            player.alive = false;
            app.status = "Crashed.";
            return;
        }
        playerRect.y = player.y;
    }

    for (int i = 0; i < static_cast<int>(level.objects.size()); ++i) {
        const auto& obj = level.objects[i];
        Rectangle rect = {static_cast<float>(obj.col * TILE), static_cast<float>(obj.row * TILE), static_cast<float>(obj.w * TILE), static_cast<float>(obj.h * TILE)};
        if (obj.type == "spike" && CheckCollisionRecs(playerRect, rect)) {
            player.alive = false;
            app.status = "Spike hit.";
        }
        if (obj.type == "pad" && CheckCollisionRecs(playerRect, rect) && player.vy >= 0 && prevY + PLAYER_SIZE <= rect.y + 12) {
            player.y = rect.y - PLAYER_SIZE;
            player.vy = -level.padVelocity;
            player.onGround = false;
        }
        if (obj.type == "coin" && !Contains(app.session.usedCoins, i) && CheckCollisionRecs(playerRect, rect)) {
            app.session.usedCoins.push_back(i);
            player.coins += 1;
        }
    }

    if (player.x >= level.goalCol * TILE) {
        player.won = true;
        app.status = "Cleared " + level.name;
    }

    app.session.cameraX = std::max(0.0f, player.x - 220.0f);
    app.session.time += dt;
}

static void DrawBackgroundScene() {
    DrawRectangleGradientV(0, 0, WIDTH, HEIGHT, Color{20, 33, 61, 255}, Color{247, 127, 0, 255});
    DrawCircle(WIDTH - 180, 150, 80.0f, Color{255, 191, 105, 120});
}

static void DrawWorld(const Level& level, float cameraX, const Session* session) {
    int stageWidth = WIDTH - SIDEBAR;
    for (int i = 0; i <= level.cols; ++i) {
        DrawLine(static_cast<int>(i * TILE - cameraX), 0, static_cast<int>(i * TILE - cameraX), HEIGHT, Color{54, 80, 108, 255});
    }
    for (int row = 0; row <= level.rows; ++row) {
        DrawLine(0, row * TILE, stageWidth, row * TILE, Color{54, 80, 108, 255});
    }
    DrawRectangle(0, level.groundRow * TILE, stageWidth, HEIGHT - level.groundRow * TILE, Color{35, 57, 93, 255});
    DrawRectangle(static_cast<int>(level.goalCol * TILE - cameraX), 100, 10, level.groundRow * TILE - 100, Color{111, 255, 233, 255});
    for (int i = 0; i < static_cast<int>(level.objects.size()); ++i) {
        const auto& obj = level.objects[i];
        int x = static_cast<int>(obj.col * TILE - cameraX);
        int y = obj.row * TILE;
        if (obj.type == "block") DrawRectangle(x, y, obj.w * TILE, obj.h * TILE, Color{123, 223, 242, 255});
        if (obj.type == "spike") DrawTriangle({static_cast<float>(x), static_cast<float>(y + TILE)}, {static_cast<float>(x + TILE / 2), static_cast<float>(y)}, {static_cast<float>(x + TILE), static_cast<float>(y + TILE)}, Color{255, 77, 109, 255});
        if (obj.type == "orb") DrawCircle(x + TILE / 2, y + TILE / 2, 14.0f, session && Contains(session->usedOrbs, i) ? Fade(LIGHTGRAY, 0.4f) : Color{46, 196, 182, 255});
        if (obj.type == "pad") DrawRectangle(x + 4, y + TILE - 12, TILE - 8, 12, Color{255, 191, 105, 255});
        if (obj.type == "coin" && !(session && Contains(session->usedCoins, i))) DrawCircle(x + TILE / 2, y + TILE / 2, 9.0f, Color{255, 224, 102, 255});
    }
}

int main() {
    InitWindow(WIDTH, HEIGHT, "Geometry Jump - C++");
    SetTargetFPS(60);

    App app;
    app.builtin.push_back(MakePulseRun());
    app.editor = MakeBlankLevel();

    while (!WindowShouldClose()) {
        if (app.screen == Screen::Play) {
            UpdatePlay(app, GetFrameTime());
        }

        if (app.screen == Screen::Editor) {
            if (IsKeyPressed(KEY_ONE)) app.tool = Tool::Block;
            if (IsKeyPressed(KEY_TWO)) app.tool = Tool::Spike;
            if (IsKeyPressed(KEY_THREE)) app.tool = Tool::Orb;
            if (IsKeyPressed(KEY_FOUR)) app.tool = Tool::Pad;
            if (IsKeyPressed(KEY_FIVE)) app.tool = Tool::Coin;
            if (IsKeyPressed(KEY_DELETE) || IsKeyPressed(KEY_BACKSPACE)) app.tool = Tool::Erase;
            if (IsKeyPressed(KEY_A)) app.editorCameraCol = std::max(0, app.editorCameraCol - 2);
            if (IsKeyPressed(KEY_D)) app.editorCameraCol = std::min(app.editor.cols - 24, app.editorCameraCol + 2);
            if ((IsMouseButtonPressed(MOUSE_BUTTON_LEFT) || IsMouseButtonPressed(MOUSE_BUTTON_RIGHT)) && GetMouseX() < WIDTH - SIDEBAR) {
                int col = GetMouseX() / TILE + app.editorCameraCol;
                int row = GetMouseY() / TILE;
                if (row < app.editor.groundRow && col >= 0 && col < app.editor.cols) {
                    app.editor.objects.erase(std::remove_if(app.editor.objects.begin(), app.editor.objects.end(), [col, row](const LevelObject& obj) {
                        return obj.col == col && obj.row == row;
                    }), app.editor.objects.end());
                    if (!IsMouseButtonPressed(MOUSE_BUTTON_RIGHT) && app.tool != Tool::Erase) {
                        std::string type = "block";
                        if (app.tool == Tool::Spike) type = "spike";
                        if (app.tool == Tool::Orb) type = "orb";
                        if (app.tool == Tool::Pad) type = "pad";
                        if (app.tool == Tool::Coin) type = "coin";
                        app.editor.objects.push_back({type, col, row, 1, 1});
                    }
                }
            }
        }

        BeginDrawing();
        DrawBackgroundScene();

        if (app.screen == Screen::Play && app.hasSession) {
            DrawWorld(app.session.level, app.session.cameraX, &app.session);
            float px = app.session.player.x - app.session.cameraX;
            DrawRectangle(static_cast<int>(px), static_cast<int>(app.session.player.y), PLAYER_SIZE, PLAYER_SIZE, app.session.player.alive ? WHITE : RED);
            DrawText(app.session.level.name.c_str(), 28, 34, 28, WHITE);
            if (Button({330.0f, 24.0f, 110.0f, 38.0f}, "Restart", Color{255, 159, 28, 255}, BLACK)) StartLevel(app, app.session.level);
            if (Button({450.0f, 24.0f, 110.0f, 38.0f}, "Home", WHITE, Color{20, 33, 61, 255})) app.screen = Screen::Menu;
        } else if (app.screen == Screen::Editor) {
            DrawWorld(app.editor, static_cast<float>(app.editorCameraCol * TILE), nullptr);
        } else {
            DrawWorld(app.builtin.front(), 320.0f, nullptr);
        }

        DrawRectangle(WIDTH - SIDEBAR, 0, SIDEBAR, HEIGHT, Color{8, 18, 36, 240});
        DrawText("Status", WIDTH - SIDEBAR + 24, 34, 34, WHITE);
        DrawText(app.status.c_str(), WIDTH - SIDEBAR + 24, 82, 18, WHITE);

        if (app.screen == Screen::Menu) {
            DrawText("Geometry Jump", 100, 130, 46, WHITE);
            DrawText("C++ raylib starter with the same shared level and physics idea.", 100, 185, 22, Color{183, 196, 223, 255});
            if (Button({100.0f, 250.0f, 220.0f, 46.0f}, "Play Demo", Color{255, 159, 28, 255}, BLACK)) app.screen = Screen::Levels;
            if (Button({100.0f, 308.0f, 220.0f, 46.0f}, "Open Editor", Color{46, 196, 182, 255}, BLACK)) app.screen = Screen::Editor;
            if (Button({100.0f, 366.0f, 220.0f, 46.0f}, "Community", WHITE, Color{20, 33, 61, 255})) app.screen = Screen::Community;
        } else if (app.screen == Screen::Levels) {
            DrawText("Level Select", 90, 100, 38, WHITE);
            auto levels = app.builtin;
            levels.insert(levels.end(), app.user.begin(), app.user.end());
            int y = 150;
            for (int i = 0; i < static_cast<int>(levels.size()); ++i) {
                DrawRectangle(90, y, 530, 88, Color{8, 18, 36, 220});
                DrawText(levels[i].name.c_str(), 110, y + 18, 24, WHITE);
                DrawText(levels[i].author.c_str(), 110, y + 50, 16, Color{183, 196, 223, 255});
                if (Button({470.0f, static_cast<float>(y + 18), 70.0f, 36.0f}, "Play", Color{255, 159, 28, 255}, BLACK)) StartLevel(app, levels[i]);
                if (Button({550.0f, static_cast<float>(y + 18), 70.0f, 36.0f}, "Edit", WHITE, Color{20, 33, 61, 255})) { app.editor = levels[i]; app.screen = Screen::Editor; }
                y += 98;
            }
            if (Button({WIDTH - SIDEBAR + 24.0f, 140.0f, 250.0f, 38.0f}, "Home", WHITE, Color{20, 33, 61, 255})) app.screen = Screen::Menu;
        } else if (app.screen == Screen::Community) {
            DrawText("Community Mock", 90, 100, 38, WHITE);
            auto levels = app.builtin;
            levels.insert(levels.end(), app.published.begin(), app.published.end());
            int y = 150;
            for (int i = 0; i < static_cast<int>(levels.size()); ++i) {
                DrawRectangle(90, y, 530, 88, Color{8, 18, 36, 220});
                DrawText(levels[i].name.c_str(), 110, y + 18, 24, WHITE);
                DrawText(levels[i].author.c_str(), 110, y + 50, 16, Color{183, 196, 223, 255});
                if (Button({470.0f, static_cast<float>(y + 18), 150.0f, 36.0f}, "Import", WHITE, Color{20, 33, 61, 255})) { app.editor = levels[i]; app.screen = Screen::Editor; }
                y += 98;
            }
            if (Button({WIDTH - SIDEBAR + 24.0f, 140.0f, 250.0f, 38.0f}, "Home", WHITE, Color{20, 33, 61, 255})) app.screen = Screen::Menu;
        } else if (app.screen == Screen::Editor) {
            DrawText("Editor", WIDTH - SIDEBAR + 24, 132, 34, WHITE);
            DrawText(app.editor.name.c_str(), WIDTH - SIDEBAR + 24, 176, 20, WHITE);
            DrawText("Keys 1-5 swap tools, A/D scroll, right click erases.", WIDTH - SIDEBAR + 24, 208, 16, Color{183, 196, 223, 255});
            if (Button({WIDTH - SIDEBAR + 24.0f, 250.0f, 120.0f, 36.0f}, "Playtest", Color{255, 159, 28, 255}, BLACK)) StartLevel(app, app.editor);
            if (Button({WIDTH - SIDEBAR + 154.0f, 250.0f, 120.0f, 36.0f}, "Save", Color{46, 196, 182, 255}, BLACK)) ReplaceById(app.user, app.editor);
            if (Button({WIDTH - SIDEBAR + 24.0f, 298.0f, 250.0f, 36.0f}, "Publish", WHITE, Color{20, 33, 61, 255})) ReplaceById(app.published, app.editor);
            if (Button({WIDTH - SIDEBAR + 24.0f, 346.0f, 250.0f, 36.0f}, "New Blank", WHITE, Color{20, 33, 61, 255})) { app.editor = MakeBlankLevel(); app.editorCameraCol = 0; }
            if (Button({WIDTH - SIDEBAR + 24.0f, 394.0f, 250.0f, 36.0f}, "Home", WHITE, Color{20, 33, 61, 255})) app.screen = Screen::Menu;
        }

        EndDrawing();
    }

    CloseWindow();
    return 0;
}
