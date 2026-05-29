
// Sandbox 2D - C++ / raylib
// Requiere raylib instalado.
// Compilar en MinGW/Windows (ejemplo):
// g++ sandbox_raylib.cpp -o sandbox_raylib.exe -std=c++17 -lraylib -lopengl32 -lgdi32 -lwinmm
#include "raylib.h"
#include <algorithm>
#include <cmath>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

static const int CELL = 32;
static const int COLS = 40;
static const int ROWS = 18;
static const int WORLD_W = COLS * CELL;
static const int WORLD_H = ROWS * CELL;
static const int MENU_H = 144;
static const int SCREEN_W = WORLD_W;
static const int SCREEN_H = WORLD_H + MENU_H;

enum Element { EMPTY = 0, STONE, WOOD, WATER, DIRT, LEAF, BOMB, NUKE };

struct Human {
    int scheme = 0; // 0 WASD, 1 flechas
    float x = 0;
    float y = 0;
    float vx = 0;
    float vy = 0;
    bool alive = true;
    float w = 20;
    float h = 38;
};

struct Button {
    Rectangle rect;
    std::string label;
    int kind; // 0 cat, 1 item, 2 action
    int valueInt = 0;
    std::string valueStr;
};

static int grid[ROWS][COLS] = {0};
static int timers_[ROWS][COLS] = {0};
static Human* humans[2] = { nullptr, nullptr };
static std::string selectedCategory = "Materiales";
static std::string selectedItemStr = "";
static int selectedItemInt = STONE;
static bool selectedIsHuman = false;
static bool frameFlip = false;
static const char* SAVE_FILE = "world_save_cpp.json";

Color colorFor(int e) {
    switch (e) {
        case STONE: return Color{127, 133, 146, 255};
        case WOOD:  return Color{136, 84, 47, 255};
        case WATER: return Color{76, 149, 255, 255};
        case DIRT:  return Color{136, 82, 48, 255};
        case LEAF:  return Color{77, 177, 95, 255};
        case BOMB:  return Color{240, 135, 87, 255};
        case NUKE:  return Color{138, 255, 138, 255};
        default:    return Color{20, 24, 34, 255};
    }
}

bool inBounds(int x, int y) {
    return x >= 0 && y >= 0 && x < COLS && y < ROWS;
}
bool isEmptyCell(int x, int y) {
    return inBounds(x, y) && grid[y][x] == EMPTY;
}
bool solidAtCell(int x, int y) {
    if (!inBounds(x, y)) return true;
    int v = grid[y][x];
    return v == STONE || v == WOOD || v == DIRT || v == LEAF || v == BOMB || v == NUKE;
}
void moveCell(int x1, int y1, int x2, int y2) {
    grid[y2][x2] = grid[y1][x1];
    timers_[y2][x2] = timers_[y1][x1];
    grid[y1][x1] = EMPTY;
    timers_[y1][x1] = 0;
}
void explode(int cx, int cy, int radiusCells) {
    int r2 = radiusCells * radiusCells;
    for (int y = std::max(0, cy - radiusCells); y <= std::min(ROWS - 1, cy + radiusCells); y++) {
        for (int x = std::max(0, cx - radiusCells); x <= std::min(COLS - 1, cx + radiusCells); x++) {
            int dx = x - cx;
            int dy = y - cy;
            if (dx * dx + dy * dy <= r2) {
                grid[y][x] = EMPTY;
                timers_[y][x] = 0;
            }
        }
    }
    for (int i = 0; i < 2; i++) {
        if (humans[i] && humans[i]->alive) {
            int hx = (int)((humans[i]->x + humans[i]->w * 0.5f) / CELL);
            int hy = (int)((humans[i]->y + humans[i]->h * 0.5f) / CELL);
            int dx = hx - cx;
            int dy = hy - cy;
            if (dx * dx + dy * dy <= r2) {
                delete humans[i];
                humans[i] = nullptr;
            }
        }
    }
}

void spawnHuman(int scheme, int cx, int cy) {
    if (humans[scheme]) delete humans[scheme];
    humans[scheme] = new Human();
    humans[scheme]->scheme = scheme;
    humans[scheme]->x = cx * CELL + 6;
    humans[scheme]->y = cy * CELL - 6;
}

void placeAt(int px, int py, bool erase) {
    if (py >= WORLD_H) return;
    int cx = px / CELL;
    int cy = py / CELL;
    if (!inBounds(cx, cy)) return;
    if (erase) {
        grid[cy][cx] = EMPTY;
        timers_[cy][cx] = 0;
        return;
    }
    if (selectedIsHuman) {
        if (selectedItemStr == "human_wasd") spawnHuman(0, cx, cy);
        else spawnHuman(1, cx, cy);
        return;
    }
    grid[cy][cx] = selectedItemInt;
    timers_[cy][cx] = selectedItemInt == BOMB ? 180 : selectedItemInt == NUKE ? 320 : 0;
}

bool rectCollides(float x, float y, float w, float h) {
    int left = std::max(0, (int)std::floor(x / CELL));
    int right = std::min(COLS - 1, (int)std::floor((x + w - 1) / CELL));
    int top = std::max(0, (int)std::floor(y / CELL));
    int bottom = std::min(ROWS - 1, (int)std::floor((y + h - 1) / CELL));
    for (int cy = top; cy <= bottom; cy++) {
        for (int cx = left; cx <= right; cx++) {
            if (solidAtCell(cx, cy)) return true;
        }
    }
    return x < 0 || x + w > WORLD_W || y + h > WORLD_H;
}

void updateHumans() {
    for (int i = 0; i < 2; i++) {
        Human* h = humans[i];
        if (!h || !h->alive) continue;

        int move = 0;
        bool jump = false;
        if (h->scheme == 0) {
            if (IsKeyDown(KEY_A)) move--;
            if (IsKeyDown(KEY_D)) move++;
            jump = IsKeyDown(KEY_W) || IsKeyDown(KEY_SPACE);
        } else {
            if (IsKeyDown(KEY_LEFT)) move--;
            if (IsKeyDown(KEY_RIGHT)) move++;
            jump = IsKeyDown(KEY_UP);
        }

        h->vx = move * 3.0f;
        h->vy = std::min(h->vy + 0.45f, 10.0f);
        bool onGround = rectCollides(h->x, h->y + 1, h->w, h->h);
        if (jump && onGround) h->vy = -8.5f;

        float nx = h->x + h->vx;
        if (!rectCollides(nx, h->y, h->w, h->h)) h->x = nx;

        float ny = h->y + h->vy;
        if (!rectCollides(h->x, ny, h->w, h->h)) {
            h->y = ny;
        } else {
            h->vy = 0;
        }
    }
}

void simulateParticles() {
    std::vector<int> xs;
    xs.reserve(COLS);
    if (frameFlip) {
        for (int i = 0; i < COLS; i++) xs.push_back(i);
    } else {
        for (int i = COLS - 1; i >= 0; i--) xs.push_back(i);
    }
    frameFlip = !frameFlip;

    for (int y = ROWS - 2; y >= 0; y--) {
        for (int xi = 0; xi < (int)xs.size(); xi++) {
            int x = xs[xi];
            int v = grid[y][x];
            if (v == EMPTY) continue;

            if (v == DIRT || v == LEAF || v == BOMB || v == NUKE) {
                if (isEmptyCell(x, y + 1)) {
                    moveCell(x, y, x, y + 1);
                    continue;
                }
                int a = GetRandomValue(0, 1) ? -1 : 1;
                int b = -a;
                if (isEmptyCell(x + a, y + 1)) { moveCell(x, y, x + a, y + 1); continue; }
                if (isEmptyCell(x + b, y + 1)) { moveCell(x, y, x + b, y + 1); continue; }
            }

            if (v == WATER) {
                if (isEmptyCell(x, y + 1)) {
                    moveCell(x, y, x, y + 1);
                    continue;
                }
                int a = GetRandomValue(0, 1) ? -1 : 1;
                int b = -a;
                if (isEmptyCell(x + a, y + 1)) { moveCell(x, y, x + a, y + 1); continue; }
                if (isEmptyCell(x + b, y + 1)) { moveCell(x, y, x + b, y + 1); continue; }
                if (isEmptyCell(x + a, y)) { moveCell(x, y, x + a, y); continue; }
                if (isEmptyCell(x + b, y)) { moveCell(x, y, x + b, y); continue; }
            }
        }
    }

    struct Blast { int x, y, r; };
    std::vector<Blast> blasts;
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            if (grid[y][x] == BOMB || grid[y][x] == NUKE) {
                timers_[y][x]--;
                if (timers_[y][x] <= 0) {
                    blasts.push_back({x, y, grid[y][x] == BOMB ? 3 : 7});
                }
            }
        }
    }
    for (const auto& b : blasts) {
        if (inBounds(b.x, b.y) && (grid[b.y][b.x] == BOMB || grid[b.y][b.x] == NUKE)) {
            explode(b.x, b.y, b.r);
        }
    }
}

std::string serializeWorld() {
    std::ostringstream out;
    out << "{\n";
    out << "  \"version\": 1,\n";
    out << "  \"cols\": " << COLS << ",\n";
    out << "  \"rows\": " << ROWS << ",\n";
    out << "  \"grid\": [";
    bool first = true;
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            if (!first) out << ",";
            out << grid[y][x];
            first = false;
        }
    }
    out << "],\n";
    out << "  \"timers\": [";
    first = true;
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            if (!first) out << ",";
            out << timers_[y][x];
            first = false;
        }
    }
    out << "],\n";
    out << "  \"humans\": [";
    for (int i = 0; i < 2; i++) {
        if (i > 0) out << ",";
        if (!humans[i]) {
            out << "null";
        } else {
            out << "{";
            out << "\"scheme\":" << humans[i]->scheme << ",";
            out << "\"x\":" << humans[i]->x << ",";
            out << "\"y\":" << humans[i]->y << ",";
            out << "\"vx\":" << humans[i]->vx << ",";
            out << "\"vy\":" << humans[i]->vy << ",";
            out << "\"alive\":" << (humans[i]->alive ? 1 : 0);
            out << "}";
        }
    }
    out << "]\n";
    out << "}\n";
    return out.str();
}

std::vector<double> extractNumbers(const std::string& text) {
    std::vector<double> nums;
    std::string cur;
    for (char c : text) {
        if ((c >= '0' && c <= '9') || c == '-' || c == '.') {
            cur.push_back(c);
        } else if (!cur.empty()) {
            nums.push_back(std::stod(cur));
            cur.clear();
        }
    }
    if (!cur.empty()) nums.push_back(std::stod(cur));
    return nums;
}

void deserializeWorld(const std::string& text) {
    auto nums = extractNumbers(text);
    // version, cols, rows, grid..., timers..., humans...
    if ((int)nums.size() < 3 + COLS * ROWS * 2) return;
    int idx = 3;
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            grid[y][x] = (int)nums[idx++];
        }
    }
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            timers_[y][x] = (int)nums[idx++];
        }
    }
    for (int i = 0; i < 2; i++) {
        if (humans[i]) { delete humans[i]; humans[i] = nullptr; }
    }
    int remain = (int)nums.size() - idx;
    if (remain >= 12) {
        for (int i = 0; i < 2; i++) {
            int scheme = (int)nums[idx++];
            float x = (float)nums[idx++];
            float y = (float)nums[idx++];
            float vx = (float)nums[idx++];
            float vy = (float)nums[idx++];
            int alive = (int)nums[idx++];
            if (alive == 1) {
                humans[i] = new Human();
                humans[i]->scheme = scheme;
                humans[i]->x = x;
                humans[i]->y = y;
                humans[i]->vx = vx;
                humans[i]->vy = vy;
                humans[i]->alive = alive == 1;
            }
        }
    }
}

void saveWorld(const char* filename) {
    std::ofstream out(filename, std::ios::binary);
    out << serializeWorld();
}
void loadWorld(const char* filename) {
    std::ifstream in(filename, std::ios::binary);
    if (!in.good()) return;
    std::stringstream buffer;
    buffer << in.rdbuf();
    deserializeWorld(buffer.str());
}

std::vector<Button> getButtons() {
    std::vector<Button> btns;
    std::vector<std::string> cats = {"Materiales", "Naturaleza", "Explosivos"};
    for (int i = 0; i < (int)cats.size(); i++) {
        btns.push_back({Rectangle{20.0f + i * 170.0f, (float)WORLD_H + 12, 150, 34}, cats[i], 0, 0, cats[i]});
    }

    std::vector<Button> items;
    if (selectedCategory == "Materiales") {
        items.push_back({Rectangle{20, (float)WORLD_H + 60, 135, 52}, "Piedra", 1, STONE, ""});
        items.push_back({Rectangle{170, (float)WORLD_H + 60, 135, 52}, "Tierra", 1, DIRT, ""});
        items.push_back({Rectangle{320, (float)WORLD_H + 60, 135, 52}, "Madera", 1, WOOD, ""});
    } else if (selectedCategory == "Naturaleza") {
        items.push_back({Rectangle{20, (float)WORLD_H + 60, 135, 52}, "Agua", 1, WATER, ""});
        items.push_back({Rectangle{170, (float)WORLD_H + 60, 135, 52}, "Hojas", 1, LEAF, ""});
        items.push_back({Rectangle{320, (float)WORLD_H + 60, 135, 52}, "Humano WASD", 1, 0, "human_wasd"});
        items.push_back({Rectangle{470, (float)WORLD_H + 60, 135, 52}, "Humano Flechas", 1, 0, "human_arrows"});
    } else {
        items.push_back({Rectangle{20, (float)WORLD_H + 60, 135, 52}, "Bomba", 1, BOMB, ""});
        items.push_back({Rectangle{170, (float)WORLD_H + 60, 135, 52}, "Bomba Nuclear", 1, NUKE, ""});
    }
    btns.insert(btns.end(), items.begin(), items.end());

    btns.push_back({Rectangle{(float)SCREEN_W - 360, (float)WORLD_H + 12, 100, 34}, "Guardar", 2, 1, ""});
    btns.push_back({Rectangle{(float)SCREEN_W - 250, (float)WORLD_H + 12, 100, 34}, "Cargar", 2, 2, ""});
    btns.push_back({Rectangle{(float)SCREEN_W - 140, (float)WORLD_H + 12, 120, 34}, "Export JSON", 2, 3, ""});
    return btns;
}

void drawButton(const Rectangle& r, const std::string& label, bool active, int fontSize = 18) {
    DrawRectangleRounded(r, 0.25f, 8, active ? Color{95, 136, 255, 255} : Color{35, 42, 58, 255});
    DrawRectangleRoundedLinesEx(r, 0.25f, 8, 2, Color{12, 16, 23, 255});
    int tw = MeasureText(label.c_str(), fontSize);
    DrawText(label.c_str(), (int)(r.x + r.width * 0.5f - tw * 0.5f), (int)(r.y + r.height * 0.5f - fontSize * 0.5f), fontSize, Color{239, 243, 255, 255});
}

int main() {
    InitWindow(SCREEN_W, SCREEN_H, "Sandbox 2D - C++ / raylib");
    SetTargetFPS(60);

    while (!WindowShouldClose()) {
        Vector2 mouse = GetMousePosition();
        bool leftPress = IsMouseButtonPressed(MOUSE_LEFT_BUTTON);
        bool leftDown = IsMouseButtonDown(MOUSE_LEFT_BUTTON);
        bool rightDown = IsMouseButtonDown(MOUSE_RIGHT_BUTTON);
        auto btns = getButtons();

        if (leftPress) {
            bool handled = false;
            for (const auto& b : btns) {
                if (CheckCollisionPointRec(mouse, b.rect)) {
                    handled = true;
                    if (b.kind == 0) {
                        selectedCategory = b.valueStr;
                        selectedIsHuman = false;
                        selectedItemStr = "";
                        selectedItemInt = selectedCategory == "Materiales" ? STONE : selectedCategory == "Naturaleza" ? WATER : BOMB;
                    } else if (b.kind == 1) {
                        if (!b.valueStr.empty()) {
                            selectedIsHuman = true;
                            selectedItemStr = b.valueStr;
                        } else {
                            selectedIsHuman = false;
                            selectedItemStr = "";
                            selectedItemInt = b.valueInt;
                        }
                    } else if (b.kind == 2) {
                        if (b.valueInt == 1) saveWorld(SAVE_FILE);
                        else if (b.valueInt == 2) loadWorld(SAVE_FILE);
                        else if (b.valueInt == 3) saveWorld("world_export_cpp.json");
                    }
                    break;
                }
            }
            if (!handled && mouse.y < WORLD_H) placeAt((int)mouse.x, (int)mouse.y, false);
        }

        if (leftDown && mouse.y < WORLD_H) placeAt((int)mouse.x, (int)mouse.y, false);
        if (rightDown && mouse.y < WORLD_H) placeAt((int)mouse.x, (int)mouse.y, true);

        if (IsKeyPressed(KEY_S)) saveWorld(SAVE_FILE);
        if (IsKeyPressed(KEY_L)) loadWorld(SAVE_FILE);
        if (IsKeyPressed(KEY_J)) saveWorld("world_export_cpp.json");

        simulateParticles();
        updateHumans();

        BeginDrawing();
        ClearBackground(Color{20, 24, 34, 255});

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int v = grid[y][x];
                if (v != EMPTY) DrawRectangle(x * CELL, y * CELL, CELL, CELL, colorFor(v));
            }
        }

        for (int x = 0; x <= COLS; x++) DrawLine(x * CELL, 0, x * CELL, WORLD_H, Color{43, 48, 64, 255});
        for (int y = 0; y <= ROWS; y++) DrawLine(0, y * CELL, WORLD_W, y * CELL, Color{43, 48, 64, 255});

        for (int i = 0; i < 2; i++) {
            if (humans[i] && humans[i]->alive) {
                Color hc = humans[i]->scheme == 0 ? Color{255, 214, 92, 255} : Color{114, 229, 255, 255};
                DrawRectangleRounded(Rectangle{humans[i]->x, humans[i]->y, humans[i]->w, humans[i]->h}, 0.3f, 6, hc);
                DrawCircle((int)(humans[i]->x + 6), (int)(humans[i]->y + 10), 2, Color{26, 27, 32, 255});
                DrawCircle((int)(humans[i]->x + humans[i]->w - 6), (int)(humans[i]->y + 10), 2, Color{26, 27, 32, 255});
            }
        }

        DrawRectangle(0, WORLD_H, SCREEN_W, MENU_H, Color{27, 31, 43, 255});
        DrawRectangle(0, WORLD_H, SCREEN_W, 3, Color{13, 16, 23, 255});

        for (const auto& b : btns) {
            bool active = false;
            if (b.kind == 0 && b.valueStr == selectedCategory) active = true;
            if (b.kind == 1) {
                if (!b.valueStr.empty()) active = (selectedIsHuman && b.valueStr == selectedItemStr);
                else active = (!selectedIsHuman && b.valueInt == selectedItemInt);
            }
            drawButton(b.rect, b.label, active, b.kind == 1 ? 14 : 18);
        }

        DrawText("Pintar: click/arrastre. Borrar: click derecho. H1 = WASD / H2 = Flechas. S guardar, L cargar, J exportar JSON.", 20, WORLD_H + 122, 13, Color{166, 176, 199, 255});

        if (mouse.y < WORLD_H) {
            int gx = ((int)mouse.x / CELL) * CELL;
            int gy = ((int)mouse.y / CELL) * CELL;
            DrawRectangleLinesEx(Rectangle{(float)gx, (float)gy, (float)CELL, (float)CELL}, 2, WHITE);
        }

        EndDrawing();
    }

    for (int i = 0; i < 2; i++) if (humans[i]) delete humans[i];
    CloseWindow();
    return 0;
}
