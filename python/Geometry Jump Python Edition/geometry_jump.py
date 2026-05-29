import json
import tkinter as tk

WIDTH = 1280
HEIGHT = 720
SIDEBAR = 320
TILE = 40
PLAYER_SIZE = 32


def clone(value):
    return json.loads(json.dumps(value))


def make_blank_level():
    return {
        "id": "untitled-pulse",
        "name": "Untitled Pulse",
        "author": "Local Creator",
        "theme": "sunset-drive",
        "speed": 300,
        "gravity": 1900,
        "jumpVelocity": 720,
        "orbVelocity": 760,
        "padVelocity": 880,
        "groundRow": 10,
        "rows": 12,
        "cols": 80,
        "goalCol": 72,
        "objects": []
    }


def make_pulse_run():
    level = make_blank_level()
    level["id"] = "pulse-run"
    level["name"] = "Pulse Run"
    level["author"] = "Geometry Jump Team"
    level["objects"] = [
        {"type": "block", "col": 11, "row": 8, "w": 2, "h": 1},
        {"type": "spike", "col": 14, "row": 9},
        {"type": "orb", "col": 16, "row": 6},
        {"type": "block", "col": 18, "row": 7, "w": 3, "h": 1},
        {"type": "coin", "col": 19, "row": 5},
        {"type": "pad", "col": 24, "row": 9},
        {"type": "block", "col": 28, "row": 6, "w": 2, "h": 1},
        {"type": "spike", "col": 31, "row": 9},
        {"type": "spike", "col": 32, "row": 9},
        {"type": "block", "col": 35, "row": 8, "w": 4, "h": 1},
        {"type": "coin", "col": 37, "row": 6},
        {"type": "orb", "col": 40, "row": 5},
        {"type": "block", "col": 44, "row": 7, "w": 3, "h": 1},
        {"type": "pad", "col": 50, "row": 9},
        {"type": "spike", "col": 54, "row": 9},
        {"type": "block", "col": 57, "row": 6, "w": 2, "h": 1},
        {"type": "coin", "col": 58, "row": 4},
        {"type": "orb", "col": 61, "row": 5},
        {"type": "block", "col": 65, "row": 8, "w": 3, "h": 1},
        {"type": "spike", "col": 69, "row": 9}
    ]
    return level


class GeometryJumpApp:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Geometry Jump - Python")
        self.canvas = tk.Canvas(self.root, width=WIDTH, height=HEIGHT, bg="#0d1321", highlightthickness=0)
        self.canvas.pack()

        self.screen = "menu"
        self.status = "Python port ready."
        self.builtin_levels = [make_pulse_run()]
        self.user_levels = []
        self.published_levels = []
        self.editor_level = make_blank_level()
        self.editor_tool = "block"
        self.editor_camera_col = 0
        self.session = None
        self.jump_queued = False
        self.buttons = []

        self.root.bind("<space>", self.on_jump_key)
        self.root.bind("<Up>", self.on_jump_key)
        self.root.bind("<w>", self.on_jump_key)
        self.root.bind("<KeyPress>", self.on_key)
        self.canvas.bind("<Button-1>", self.on_left_click)
        self.canvas.bind("<Button-3>", self.on_right_click)

        self.loop()
        self.root.mainloop()

    def on_jump_key(self, _event):
        self.jump_queued = True

    def on_key(self, event):
        if self.screen != "editor":
            return
        mapping = {"1": "block", "2": "spike", "3": "orb", "4": "pad", "5": "coin"}
        if event.keysym in mapping:
            self.editor_tool = mapping[event.keysym]
        if event.keysym in ("Delete", "BackSpace"):
            self.editor_tool = "erase"
        if event.keysym.lower() == "a":
            self.editor_camera_col = max(0, self.editor_camera_col - 2)
        if event.keysym.lower() == "d":
            self.editor_camera_col = min(self.editor_level["cols"] - 24, self.editor_camera_col + 2)

    def all_levels(self):
        return self.builtin_levels + self.user_levels

    def start_level(self, level):
        data = clone(level)
        self.session = {
            "level": data,
            "player": {
                "x": 3 * TILE,
                "y": (data["groundRow"] - 1) * TILE - PLAYER_SIZE,
                "vy": 0,
                "onGround": True,
                "alive": True,
                "won": False,
                "coins": 0
            },
            "cameraX": 0,
            "time": 0,
            "usedOrbs": set(),
            "usedCoins": set()
        }
        self.screen = "play"
        self.status = f"Playing {data['name']}"

    def queue_jump(self):
        if not self.session:
            return
        player = self.session["player"]
        level = self.session["level"]
        if not player["alive"] or player["won"]:
            return
        if player["onGround"]:
            player["vy"] = -level["jumpVelocity"]
            player["onGround"] = False
            return
        for index, obj in enumerate(level["objects"]):
            if obj["type"] != "orb" or index in self.session["usedOrbs"]:
                continue
            orb_x = obj["col"] * TILE + TILE / 2
            orb_y = obj["row"] * TILE + TILE / 2
            player_x = player["x"] + PLAYER_SIZE / 2
            player_y = player["y"] + PLAYER_SIZE / 2
            if (orb_x - player_x) ** 2 + (orb_y - player_y) ** 2 <= 58 ** 2:
                player["vy"] = -level["orbVelocity"]
                self.session["usedOrbs"].add(index)
                break

    def update_play(self):
        if not self.session:
            return
        level = self.session["level"]
        player = self.session["player"]
        if not player["alive"] or player["won"]:
            return
        if self.jump_queued:
            self.queue_jump()

        prev_y = player["y"]
        player["x"] += level["speed"] / 60.0
        player["vy"] += level["gravity"] / 60.0
        player["y"] += player["vy"] / 60.0
        player["onGround"] = False

        solids = [{"x": 0, "y": level["groundRow"] * TILE, "w": level["cols"] * TILE, "h": (level["rows"] - level["groundRow"]) * TILE}]
        for obj in level["objects"]:
            if obj["type"] == "block":
                solids.append({"x": obj["col"] * TILE, "y": obj["row"] * TILE, "w": obj.get("w", 1) * TILE, "h": obj.get("h", 1) * TILE})

        player_box = {"x": player["x"], "y": player["y"], "w": PLAYER_SIZE, "h": PLAYER_SIZE}
        for solid in solids:
            if not self.intersects(player_box, solid):
                continue
            if player["vy"] >= 0 and prev_y + PLAYER_SIZE <= solid["y"] + 8:
                player["y"] = solid["y"] - PLAYER_SIZE
                player["vy"] = 0
                player["onGround"] = True
            else:
                player["alive"] = False
                self.status = "Crashed."
                return

        for index, obj in enumerate(level["objects"]):
            rect = {"x": obj["col"] * TILE, "y": obj["row"] * TILE, "w": obj.get("w", 1) * TILE, "h": obj.get("h", 1) * TILE}
            if obj["type"] == "spike" and self.intersects(player_box, rect):
                player["alive"] = False
                self.status = "Spike hit."
            if obj["type"] == "pad" and self.intersects(player_box, rect) and player["vy"] >= 0 and prev_y + PLAYER_SIZE <= rect["y"] + 12:
                player["y"] = rect["y"] - PLAYER_SIZE
                player["vy"] = -level["padVelocity"]
                player["onGround"] = False
            if obj["type"] == "coin" and index not in self.session["usedCoins"] and self.intersects(player_box, rect):
                self.session["usedCoins"].add(index)
                player["coins"] += 1

        if player["x"] >= level["goalCol"] * TILE:
            player["won"] = True
            self.status = f"Cleared {level['name']}"

        self.session["cameraX"] = max(0, player["x"] - 220)
        self.session["time"] += 1 / 60

    def intersects(self, a, b):
        return a["x"] < b["x"] + b["w"] and a["x"] + a["w"] > b["x"] and a["y"] < b["y"] + b["h"] and a["y"] + a["h"] > b["y"]

    def on_left_click(self, event):
        for button in self.buttons:
            x0, y0, x1, y1, action = button
            if x0 <= event.x <= x1 and y0 <= event.y <= y1:
                self.handle_action(action)
                return
        if self.screen == "play":
            self.jump_queued = True
        elif self.screen == "editor":
            self.place_editor_object(event.x, event.y, erase=False)

    def on_right_click(self, event):
        if self.screen == "editor":
            self.place_editor_object(event.x, event.y, erase=True)

    def place_editor_object(self, x, y, erase):
        if x >= WIDTH - SIDEBAR:
            return
        col = x // TILE + self.editor_camera_col
        row = y // TILE
        if row >= self.editor_level["groundRow"] or col < 0 or col >= self.editor_level["cols"]:
            return
        self.editor_level["objects"] = [obj for obj in self.editor_level["objects"] if not (obj["col"] == col and obj["row"] == row)]
        if not erase and self.editor_tool != "erase":
            new_obj = {"type": self.editor_tool, "col": col, "row": row}
            if self.editor_tool == "block":
                new_obj["w"] = 1
                new_obj["h"] = 1
            self.editor_level["objects"].append(new_obj)
        self.status = f"Edited {col}, {row}"

    def handle_action(self, action):
        if action == "menu-play":
            self.screen = "levels"
        elif action == "menu-editor":
            self.screen = "editor"
        elif action == "menu-community":
            self.screen = "community"
        elif action == "home":
            self.screen = "menu"
        elif action == "restart" and self.session:
            self.start_level(self.session["level"])
        elif action == "save":
            self.user_levels = [level for level in self.user_levels if level["id"] != self.editor_level["id"]]
            self.user_levels.append(clone(self.editor_level))
        elif action == "publish":
            self.published_levels = [level for level in self.published_levels if level["id"] != self.editor_level["id"]]
            self.published_levels.append(clone(self.editor_level))
        elif action == "new":
            self.editor_level = make_blank_level()
        elif action == "playtest":
            self.start_level(self.editor_level)
        elif action.startswith("play:"):
            self.start_level(self.all_levels()[int(action.split(":")[1])])
        elif action.startswith("edit:"):
            self.editor_level = clone(self.all_levels()[int(action.split(":")[1])])
            self.screen = "editor"
        elif action.startswith("community:"):
            featured = self.builtin_levels + self.published_levels
            self.editor_level = clone(featured[int(action.split(":")[1])])
            self.screen = "editor"

    def button(self, x, y, w, h, label, action, fill="#ffffff", text="#0d1321"):
        self.canvas.create_rectangle(x, y, x + w, y + h, fill=fill, outline="")
        self.canvas.create_text(x + 14, y + 23, text=label, fill=text, anchor="w", font=("Trebuchet MS", 15, "bold"))
        self.buttons.append((x, y, x + w, y + h, action))

    def draw_background(self):
        self.canvas.create_rectangle(0, 0, WIDTH, HEIGHT, fill="#14213d", outline="")
        self.canvas.create_rectangle(0, HEIGHT // 2, WIDTH, HEIGHT, fill="#f77f00", outline="", stipple="gray25")
        self.canvas.create_oval(WIDTH - 260, 60, WIDTH - 100, 220, fill="#ffbf69", outline="")

    def draw_world(self, level, camera_x, session=None):
        stage_width = WIDTH - SIDEBAR
        ground_top = level["groundRow"] * TILE
        for i in range(level["cols"] + 1):
            x = i * TILE - camera_x
            self.canvas.create_line(x, 0, x, HEIGHT, fill="#36506c")
        for row in range(level["rows"] + 1):
            self.canvas.create_line(0, row * TILE, stage_width, row * TILE, fill="#36506c")
        self.canvas.create_rectangle(0, ground_top, stage_width, HEIGHT, fill="#23395d", outline="")
        self.canvas.create_rectangle(level["goalCol"] * TILE - camera_x, 100, level["goalCol"] * TILE - camera_x + 10, ground_top, fill="#6fffe9", outline="")
        for index, obj in enumerate(level["objects"]):
            x = obj["col"] * TILE - camera_x
            y = obj["row"] * TILE
            if obj["type"] == "block":
                self.canvas.create_rectangle(x, y, x + obj.get("w", 1) * TILE, y + obj.get("h", 1) * TILE, fill="#7bdff2", outline="")
            elif obj["type"] == "spike":
                self.canvas.create_polygon(x, y + TILE, x + TILE / 2, y, x + TILE, y + TILE, fill="#ff4d6d", outline="")
            elif obj["type"] == "orb":
                color = "#2ec4b6" if not session or index not in session["usedOrbs"] else "#7a7a7a"
                self.canvas.create_oval(x + 10, y + 10, x + 30, y + 30, fill=color, outline="#d8fff8", width=2)
            elif obj["type"] == "pad":
                self.canvas.create_rectangle(x + 4, y + 28, x + 36, y + 40, fill="#ffbf69", outline="")
            elif obj["type"] == "coin" and (not session or index not in session["usedCoins"]):
                self.canvas.create_oval(x + 12, y + 12, x + 28, y + 28, fill="#ffe066", outline="")

    def render(self):
        self.canvas.delete("all")
        self.buttons = []
        self.draw_background()

        if self.screen == "play" and self.session:
            self.draw_world(self.session["level"], self.session["cameraX"], self.session)
            player = self.session["player"]
            px = player["x"] - self.session["cameraX"]
            self.canvas.create_rectangle(px, player["y"], px + PLAYER_SIZE, player["y"] + PLAYER_SIZE, fill="#ffffff", outline="")
            self.canvas.create_text(38, 40, text=self.session["level"]["name"], fill="white", anchor="w", font=("Trebuchet MS", 20, "bold"))
            self.button(330, 24, 110, 38, "Restart", "restart", "#ff9f1c")
            self.button(450, 24, 110, 38, "Home", "home", "#ffffff", "#14213d")
        elif self.screen == "editor":
            self.draw_world(self.editor_level, self.editor_camera_col * TILE)
            self.canvas.create_rectangle(WIDTH - SIDEBAR, 0, WIDTH, HEIGHT, fill="#081224", outline="")
            self.canvas.create_text(WIDTH - SIDEBAR + 24, 50, text="Editor", fill="white", anchor="w", font=("Trebuchet MS", 28, "bold"))
            self.canvas.create_text(WIDTH - SIDEBAR + 24, 95, text=self.editor_level["name"], fill="white", anchor="w", font=("Trebuchet MS", 16, "bold"))
            self.canvas.create_text(WIDTH - SIDEBAR + 24, 125, text=f"Tool: {self.editor_tool}", fill="white", anchor="w", font=("Trebuchet MS", 14))
            self.button(WIDTH - SIDEBAR + 24, 160, 120, 36, "Playtest", "playtest", "#ff9f1c")
            self.button(WIDTH - SIDEBAR + 154, 160, 120, 36, "Save", "save", "#2ec4b6")
            self.button(WIDTH - SIDEBAR + 24, 206, 250, 36, "Publish", "publish", "#ffffff", "#14213d")
            self.button(WIDTH - SIDEBAR + 24, 252, 250, 36, "New Blank", "new", "#ffffff", "#14213d")
            self.button(WIDTH - SIDEBAR + 24, 298, 250, 36, "Home", "home", "#ffffff", "#14213d")
        elif self.screen == "levels":
            self.canvas.create_text(90, 100, text="Level Select", fill="white", anchor="w", font=("Trebuchet MS", 34, "bold"))
            y = 150
            for index, level in enumerate(self.all_levels()):
                self.canvas.create_rectangle(90, y, 620, y + 88, fill="#081224", outline="")
                self.canvas.create_text(110, y + 30, text=level["name"], fill="white", anchor="w", font=("Trebuchet MS", 18, "bold"))
                self.canvas.create_text(110, y + 58, text=f"By {level['author']}", fill="#b7c4df", anchor="w", font=("Trebuchet MS", 12))
                self.button(470, y + 18, 70, 36, "Play", f"play:{index}", "#ff9f1c")
                self.button(550, y + 18, 70, 36, "Edit", f"edit:{index}", "#ffffff", "#14213d")
                y += 98
            self.button(WIDTH - SIDEBAR + 24, 140, 250, 38, "Home", "home", "#ffffff", "#14213d")
        elif self.screen == "community":
            self.canvas.create_text(90, 100, text="Community Mock", fill="white", anchor="w", font=("Trebuchet MS", 34, "bold"))
            y = 150
            for index, level in enumerate(self.builtin_levels + self.published_levels):
                self.canvas.create_rectangle(90, y, 620, y + 88, fill="#081224", outline="")
                self.canvas.create_text(110, y + 30, text=level["name"], fill="white", anchor="w", font=("Trebuchet MS", 18, "bold"))
                self.canvas.create_text(110, y + 58, text=f"By {level['author']}", fill="#b7c4df", anchor="w", font=("Trebuchet MS", 12))
                self.button(470, y + 18, 150, 36, "Import", f"community:{index}", "#ffffff", "#14213d")
                y += 98
            self.button(WIDTH - SIDEBAR + 24, 140, 250, 38, "Home", "home", "#ffffff", "#14213d")
        else:
            self.canvas.create_text(100, 140, text="Geometry Jump", fill="white", anchor="w", font=("Trebuchet MS", 42, "bold"))
            self.canvas.create_text(100, 188, text="Python prototype with the same core model and physics.", fill="#b7c4df", anchor="w", font=("Trebuchet MS", 18))
            self.button(100, 250, 220, 46, "Play Demo", "menu-play", "#ff9f1c")
            self.button(100, 308, 220, 46, "Open Editor", "menu-editor", "#2ec4b6")
            self.button(100, 366, 220, 46, "Community", "menu-community", "#ffffff", "#14213d")

        self.canvas.create_rectangle(WIDTH - SIDEBAR, 0, WIDTH, HEIGHT, fill="#081224", outline="")
        self.canvas.create_text(WIDTH - SIDEBAR + 24, 46, text="Status", fill="white", anchor="w", font=("Trebuchet MS", 26, "bold"))
        self.canvas.create_text(WIDTH - SIDEBAR + 24, 90, text=self.status, fill="white", anchor="w", font=("Trebuchet MS", 14))
        self.canvas.create_text(WIDTH - SIDEBAR + 24, 132, text="This Python port uses Tkinter only.", fill="#b7c4df", anchor="w", font=("Trebuchet MS", 12))

    def loop(self):
        if self.screen == "play":
            self.update_play()
        self.render()
        self.jump_queued = False
        self.root.after(16, self.loop)


if __name__ == "__main__":
    GeometryJumpApp()
