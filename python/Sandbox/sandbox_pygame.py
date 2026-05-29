
import json
import math
import os
import random
import pygame

pygame.init()
pygame.display.set_caption("Sandbox 2D - Python/Pygame")

CELL = 32
COLS = 40
ROWS = 18
WORLD_W = COLS * CELL
WORLD_H = ROWS * CELL
MENU_H = 144
SCREEN_W = WORLD_W
SCREEN_H = WORLD_H + MENU_H
FPS = 60

EMPTY, STONE, WOOD, WATER, DIRT, LEAF, BOMB, NUKE = range(8)

COLORS = {
    EMPTY: (18, 20, 28),
    STONE: (120, 125, 135),
    WOOD: (124, 78, 45),
    WATER: (65, 145, 255),
    DIRT: (121, 82, 51),
    LEAF: (65, 165, 78),
    BOMB: (230, 112, 66),
    NUKE: (122, 255, 122),
}

UI_BG = (26, 28, 38)
UI_PANEL = (36, 40, 52)
UI_ACCENT = (90, 130, 255)
UI_TEXT = (235, 240, 255)
UI_MUTED = (160, 170, 190)

CATEGORIES = ["Materiales", "Naturaleza", "Explosivos"]
ITEMS = {
    "Materiales": [("Piedra", STONE), ("Tierra", DIRT), ("Madera", WOOD)],
    "Naturaleza": [("Agua", WATER), ("Hojas", LEAF), ("Humano WASD", "human_wasd"), ("Humano Flechas", "human_arrows")],
    "Explosivos": [("Bomba", BOMB), ("Bomba Nuclear", NUKE)],
}

SAVE_FILE = "world_save_python.json"

screen = pygame.display.set_mode((SCREEN_W, SCREEN_H))
clock = pygame.time.Clock()
font = pygame.font.SysFont("arial", 18)
font_small = pygame.font.SysFont("arial", 14)

grid = [[EMPTY for _ in range(COLS)] for _ in range(ROWS)]
timers = [[0 for _ in range(COLS)] for _ in range(ROWS)]

selected_category = "Materiales"
selected_item = STONE
frame_flip = False

class Human:
    def __init__(self, scheme, x, y):
        self.scheme = scheme  # 0 WASD, 1 arrows
        self.x = x
        self.y = y
        self.vx = 0.0
        self.vy = 0.0
        self.alive = True
        self.w = 20
        self.h = 38

    @property
    def color(self):
        return (255, 210, 90) if self.scheme == 0 else (120, 230, 255)

humans = [None, None]

def in_bounds(cx, cy):
    return 0 <= cx < COLS and 0 <= cy < ROWS

def is_empty(cx, cy):
    return in_bounds(cx, cy) and grid[cy][cx] == EMPTY

def solid_at_cell(cx, cy):
    if not in_bounds(cx, cy):
        return True
    return grid[cy][cx] in (STONE, WOOD, DIRT, LEAF, BOMB, NUKE)

def move_cell(x1, y1, x2, y2):
    grid[y2][x2] = grid[y1][x1]
    timers[y2][x2] = timers[y1][x1]
    grid[y1][x1] = EMPTY
    timers[y1][x1] = 0

def explode(cx, cy, radius_cells):
    r2 = radius_cells * radius_cells
    for yy in range(max(0, cy - radius_cells), min(ROWS, cy + radius_cells + 1)):
        for xx in range(max(0, cx - radius_cells), min(COLS, cx + radius_cells + 1)):
            dx = xx - cx
            dy = yy - cy
            if dx * dx + dy * dy <= r2:
                grid[yy][xx] = EMPTY
                timers[yy][xx] = 0

    for i, h in enumerate(humans):
        if h and h.alive:
            hx = int((h.x + h.w * 0.5) // CELL)
            hy = int((h.y + h.h * 0.5) // CELL)
            dx = hx - cx
            dy = hy - cy
            if dx * dx + dy * dy <= r2:
                humans[i] = None

def serialize_world():
    return {
        "cols": COLS,
        "rows": ROWS,
        "grid": [cell for row in grid for cell in row],
        "timers": [cell for row in timers for cell in row],
        "humans": [
            None if h is None else {
                "scheme": h.scheme,
                "x": h.x, "y": h.y, "vx": h.vx, "vy": h.vy, "alive": h.alive
            }
            for h in humans
        ],
        "version": 1
    }

def deserialize_world(data):
    flat_grid = data.get("grid", [])
    flat_timers = data.get("timers", [])
    if len(flat_grid) != COLS * ROWS:
        return
    idx = 0
    for y in range(ROWS):
        for x in range(COLS):
            grid[y][x] = int(flat_grid[idx])
            timers[y][x] = int(flat_timers[idx]) if idx < len(flat_timers) else 0
            idx += 1

    hlist = data.get("humans", [])
    for i in range(2):
        humans[i] = None
        if i < len(hlist) and hlist[i]:
            item = hlist[i]
            h = Human(int(item.get("scheme", i)), float(item.get("x", 100)), float(item.get("y", 100)))
            h.vx = float(item.get("vx", 0))
            h.vy = float(item.get("vy", 0))
            h.alive = bool(item.get("alive", True))
            humans[i] = h

def save_world():
    with open(SAVE_FILE, "w", encoding="utf-8") as f:
        json.dump(serialize_world(), f, indent=2)

def load_world():
    if not os.path.exists(SAVE_FILE):
        return
    with open(SAVE_FILE, "r", encoding="utf-8") as f:
        deserialize_world(json.load(f))

def export_world():
    with open("world_export_python.json", "w", encoding="utf-8") as f:
        json.dump(serialize_world(), f, indent=2)

def spawn_human(scheme, cx, cy):
    idx = 0 if scheme == 0 else 1
    humans[idx] = Human(scheme, cx * CELL + 6, cy * CELL - 6)

def place_at_mouse(mx, my, erase=False):
    global selected_item
    if my >= WORLD_H:
        return
    cx = mx // CELL
    cy = my // CELL
    if not in_bounds(cx, cy):
        return
    if erase:
        grid[cy][cx] = EMPTY
        timers[cy][cx] = 0
        return
    if selected_item == "human_wasd":
        spawn_human(0, cx, cy)
        return
    if selected_item == "human_arrows":
        spawn_human(1, cx, cy)
        return
    grid[cy][cx] = selected_item
    timers[cy][cx] = 180 if selected_item == BOMB else 320 if selected_item == NUKE else 0

def rect_hit(rect, pos):
    return rect.collidepoint(pos)

def draw_button(rect, label, active=False, small=False):
    color = UI_ACCENT if active else UI_PANEL
    pygame.draw.rect(screen, color, rect, border_radius=10)
    pygame.draw.rect(screen, (12, 14, 20), rect, 2, border_radius=10)
    surf = (font_small if small else font).render(label, True, UI_TEXT)
    screen.blit(surf, surf.get_rect(center=rect.center))

def simulate_particles():
    global frame_flip
    x_range = range(COLS) if frame_flip else range(COLS - 1, -1, -1)
    frame_flip = not frame_flip

    for y in range(ROWS - 2, -1, -1):
        for x in x_range:
            v = grid[y][x]
            if v == EMPTY:
                continue

            if v in (DIRT, LEAF, BOMB, NUKE):
                if is_empty(x, y + 1):
                    move_cell(x, y, x, y + 1)
                    continue
                dirs = [(-1, 1), (1, 1)]
                random.shuffle(dirs)
                moved = False
                for dx, dy in dirs:
                    nx, ny = x + dx, y + dy
                    if is_empty(nx, ny):
                        move_cell(x, y, nx, ny)
                        moved = True
                        break
                if moved:
                    continue

            if v == WATER:
                if is_empty(x, y + 1):
                    move_cell(x, y, x, y + 1)
                    continue
                dirs = [(-1, 1), (1, 1), (-1, 0), (1, 0)]
                random.shuffle(dirs)
                for dx, dy in dirs:
                    nx, ny = x + dx, y + dy
                    if is_empty(nx, ny):
                        move_cell(x, y, nx, ny)
                        break

    to_blow = []
    for y in range(ROWS):
        for x in range(COLS):
            if grid[y][x] in (BOMB, NUKE):
                timers[y][x] -= 1
                if timers[y][x] <= 0:
                    to_blow.append((x, y, 3 if grid[y][x] == BOMB else 7))
    for x, y, radius in to_blow:
        if in_bounds(x, y) and grid[y][x] in (BOMB, NUKE):
            explode(x, y, radius)

def rect_collides(x, y, w, h):
    left = max(0, int(x // CELL))
    right = min(COLS - 1, int((x + w - 1) // CELL))
    top = max(0, int(y // CELL))
    bottom = min(ROWS - 1, int((y + h - 1) // CELL))
    for cy in range(top, bottom + 1):
        for cx in range(left, right + 1):
            if solid_at_cell(cx, cy):
                return True
    if x < 0 or x + w > WORLD_W or y + h > WORLD_H:
        return True
    return False

def update_humans(keys):
    for h in humans:
        if not h or not h.alive:
            continue

        move = 0
        jump = False
        if h.scheme == 0:
            if keys[pygame.K_a]:
                move -= 1
            if keys[pygame.K_d]:
                move += 1
            jump = keys[pygame.K_w] or keys[pygame.K_SPACE]
        else:
            if keys[pygame.K_LEFT]:
                move -= 1
            if keys[pygame.K_RIGHT]:
                move += 1
            jump = keys[pygame.K_UP]

        h.vx = move * 3.0
        h.vy = min(h.vy + 0.45, 10)

        on_ground = rect_collides(h.x, h.y + 1, h.w, h.h)
        if jump and on_ground:
            h.vy = -8.5

        new_x = h.x + h.vx
        if not rect_collides(new_x, h.y, h.w, h.h):
            h.x = new_x
        else:
            step = 1 if h.vx > 0 else -1
            while step != 0 and not rect_collides(h.x + step, h.y, h.w, h.h):
                h.x += step

        new_y = h.y + h.vy
        if not rect_collides(h.x, new_y, h.w, h.h):
            h.y = new_y
        else:
            if h.vy > 0:
                while not rect_collides(h.x, h.y + 1, h.w, h.h):
                    h.y += 1
            else:
                while not rect_collides(h.x, h.y - 1, h.w, h.h):
                    h.y -= 1
            h.vy = 0

def draw_world():
    screen.fill(COLORS[EMPTY])
    for y in range(ROWS):
        for x in range(COLS):
            v = grid[y][x]
            if v != EMPTY:
                pygame.draw.rect(screen, COLORS[v], (x * CELL, y * CELL, CELL, CELL))
    for x in range(COLS + 1):
        pygame.draw.line(screen, (34, 36, 44), (x * CELL, 0), (x * CELL, WORLD_H), 1)
    for y in range(ROWS + 1):
        pygame.draw.line(screen, (34, 36, 44), (0, y * CELL), (WORLD_W, y * CELL), 1)

    for h in humans:
        if h and h.alive:
            pygame.draw.rect(screen, h.color, (h.x, h.y, h.w, h.h), border_radius=8)
            eye_y = h.y + 10
            pygame.draw.circle(screen, (20, 20, 20), (int(h.x + 6), int(eye_y)), 2)
            pygame.draw.circle(screen, (20, 20, 20), (int(h.x + h.w - 6), int(eye_y)), 2)

def draw_ui(mouse_pos):
    pygame.draw.rect(screen, UI_BG, (0, WORLD_H, SCREEN_W, MENU_H))
    pygame.draw.line(screen, (8, 10, 16), (0, WORLD_H), (SCREEN_W, WORLD_H), 3)

    category_rects = []
    for i, name in enumerate(CATEGORIES):
        rect = pygame.Rect(20 + i * 170, WORLD_H + 12, 150, 34)
        category_rects.append((rect, name))
        draw_button(rect, name, active=(name == selected_category))

    item_rects = []
    items = ITEMS[selected_category]
    for i, (label, value) in enumerate(items):
        rect = pygame.Rect(20 + i * 150, WORLD_H + 60, 135, 52)
        item_rects.append((rect, value))
        draw_button(rect, label, active=(value == selected_item), small=True)

    utility = [
        (pygame.Rect(SCREEN_W - 360, WORLD_H + 12, 100, 34), "Guardar"),
        (pygame.Rect(SCREEN_W - 250, WORLD_H + 12, 100, 34), "Cargar"),
        (pygame.Rect(SCREEN_W - 140, WORLD_H + 12, 120, 34), "Export JSON"),
    ]
    for rect, label in utility:
        draw_button(rect, label, small=True)

    hint = "Pinta con click/arrastre. Borrá con click derecho. H1: WASD/ESPACIO | H2: Flechas"
    screen.blit(font_small.render(hint, True, UI_MUTED), (20, WORLD_H + 122))

    mx, my = mouse_pos
    if my < WORLD_H:
        preview = pygame.Rect((mx // CELL) * CELL, (my // CELL) * CELL, CELL, CELL)
        pygame.draw.rect(screen, (255, 255, 255), preview, 2)

    return category_rects, item_rects, utility

running = True
painting = False
erasing = False

while running:
    mouse_pos = pygame.mouse.get_pos()
    category_rects, item_rects, utility_rects = draw_ui(mouse_pos)
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False

        elif event.type == pygame.MOUSEBUTTONDOWN:
            if event.button == 1:
                handled = False
                for rect, name in category_rects:
                    if rect_hit(rect, event.pos):
                        selected_category = name
                        selected_item = ITEMS[selected_category][0][1]
                        handled = True
                        break
                if not handled:
                    for rect, value in item_rects:
                        if rect_hit(rect, event.pos):
                            selected_item = value
                            handled = True
                            break
                if not handled:
                    if rect_hit(utility_rects[0][0], event.pos):
                        save_world()
                        handled = True
                    elif rect_hit(utility_rects[1][0], event.pos):
                        load_world()
                        handled = True
                    elif rect_hit(utility_rects[2][0], event.pos):
                        export_world()
                        handled = True

                if not handled and event.pos[1] < WORLD_H:
                    painting = True
                    place_at_mouse(*event.pos, erase=False)

            elif event.button == 3 and event.pos[1] < WORLD_H:
                erasing = True
                place_at_mouse(*event.pos, erase=True)

        elif event.type == pygame.MOUSEBUTTONUP:
            if event.button == 1:
                painting = False
            elif event.button == 3:
                erasing = False

        elif event.type == pygame.KEYDOWN:
            if event.key == pygame.K_s:
                save_world()
            elif event.key == pygame.K_l:
                load_world()
            elif event.key == pygame.K_j:
                export_world()

    if painting:
        place_at_mouse(*mouse_pos, erase=False)
    if erasing:
        place_at_mouse(*mouse_pos, erase=True)

    simulate_particles()
    update_humans(pygame.key.get_pressed())
    draw_world()
    category_rects, item_rects, utility_rects = draw_ui(mouse_pos)
    pygame.display.flip()
    clock.tick(FPS)

pygame.quit()
