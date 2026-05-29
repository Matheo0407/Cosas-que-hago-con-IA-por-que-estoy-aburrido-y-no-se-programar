const WIDTH = 1280;
const HEIGHT = 720;
const TILE = 40;
const PLAYER_SIZE = 32;

const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");
const overlay = document.getElementById("overlay");
const toolsPanel = document.getElementById("toolsPanel");
const statusText = document.getElementById("statusText");

const app = {
  screen: "menu",
  session: null,
  status: "Boot sequence complete. Select a screen.",
  builtinLevels: [makePulseRun()],
  userLevels: loadCollection("gj-user-levels"),
  publishedLevels: loadCollection("gj-published-levels"),
  editorLevel: makeBlankLevel(),
  editorTool: "block",
  editorCameraCol: 0,
  jumpQueued: false,
  lastTimestamp: 0
};

document.querySelectorAll("[data-screen]").forEach((button) => {
  button.addEventListener("click", () => goTo(button.dataset.screen));
});

canvas.addEventListener("mousedown", (event) => {
  const point = canvasPoint(event);
  if (app.screen === "play") {
    app.jumpQueued = true;
    return;
  }
  if (app.screen !== "editor") {
    return;
  }
  handleEditorPointer(point, event.button === 2);
});

canvas.addEventListener("contextmenu", (event) => event.preventDefault());

window.addEventListener("keydown", (event) => {
  if (["Space", "ArrowUp", "KeyW"].includes(event.code)) {
    app.jumpQueued = true;
    event.preventDefault();
  }
  if (app.screen === "editor") {
    if (event.code === "Digit1") app.editorTool = "block";
    if (event.code === "Digit2") app.editorTool = "spike";
    if (event.code === "Digit3") app.editorTool = "orb";
    if (event.code === "Digit4") app.editorTool = "pad";
    if (event.code === "Digit5") app.editorTool = "coin";
    if (event.code === "KeyA") app.editorCameraCol = Math.max(0, app.editorCameraCol - 2);
    if (event.code === "KeyD") app.editorCameraCol = Math.min(app.editorLevel.cols - 32, app.editorCameraCol + 2);
    if (event.code === "Delete" || event.code === "Backspace") app.editorTool = "erase";
    renderUi();
  }
});

function makePulseRun() {
  return {
    id: "pulse-run",
    name: "Pulse Run",
    author: "Geometry Jump Team",
    theme: "sunset-drive",
    speed: 300,
    gravity: 1900,
    jumpVelocity: 720,
    orbVelocity: 760,
    padVelocity: 880,
    groundRow: 10,
    rows: 12,
    cols: 80,
    goalCol: 72,
    objects: [
      { type: "block", col: 11, row: 8, w: 2, h: 1 },
      { type: "spike", col: 14, row: 9 },
      { type: "orb", col: 16, row: 6 },
      { type: "block", col: 18, row: 7, w: 3, h: 1 },
      { type: "coin", col: 19, row: 5 },
      { type: "pad", col: 24, row: 9 },
      { type: "block", col: 28, row: 6, w: 2, h: 1 },
      { type: "spike", col: 31, row: 9 },
      { type: "spike", col: 32, row: 9 },
      { type: "block", col: 35, row: 8, w: 4, h: 1 },
      { type: "coin", col: 37, row: 6 },
      { type: "orb", col: 40, row: 5 },
      { type: "block", col: 44, row: 7, w: 3, h: 1 },
      { type: "pad", col: 50, row: 9 },
      { type: "spike", col: 54, row: 9 },
      { type: "block", col: 57, row: 6, w: 2, h: 1 },
      { type: "coin", col: 58, row: 4 },
      { type: "orb", col: 61, row: 5 },
      { type: "block", col: 65, row: 8, w: 3, h: 1 },
      { type: "spike", col: 69, row: 9 }
    ]
  };
}

function makeBlankLevel() {
  return {
    id: `custom-${Date.now()}`,
    name: "Untitled Pulse",
    author: "Local Creator",
    theme: "sunset-drive",
    speed: 300,
    gravity: 1900,
    jumpVelocity: 720,
    orbVelocity: 760,
    padVelocity: 880,
    groundRow: 10,
    rows: 12,
    cols: 80,
    goalCol: 72,
    objects: []
  };
}

function deepClone(value) {
  return JSON.parse(JSON.stringify(value));
}

function loadCollection(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || "[]");
  } catch (_error) {
    return [];
  }
}

function saveCollection(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

function goTo(screen) {
  app.screen = screen;
  if (screen === "editor" && !app.editorLevel) {
    app.editorLevel = makeBlankLevel();
  }
  if (screen !== "play") {
    app.session = null;
  }
  setStatus({
    menu: "Welcome home. Pick a mode from the left panel.",
    levels: "Choose a level and launch a run.",
    editor: "Left click places objects. Right click erases.",
    community: "This mock community uses local storage only."
  }[screen] || "Ready.");
  renderUi();
}

function setStatus(message) {
  app.status = message;
  statusText.textContent = message;
}

function allLevels() {
  return [...app.builtinLevels, ...app.userLevels];
}

function startLevel(level) {
  const data = deepClone(level);
  const playerStartY = (data.groundRow - 1) * TILE - PLAYER_SIZE;
  app.session = {
    level: data,
    player: {
      x: 3 * TILE,
      y: playerStartY,
      vy: 0,
      onGround: true,
      alive: true,
      won: false,
      coins: 0
    },
    cameraX: 0,
    time: 0,
    usedOrbs: new Set(),
    usedCoins: new Set()
  };
  app.screen = "play";
  app.jumpQueued = false;
  setStatus(`Playing ${data.name}. Space or click to jump.`);
  renderUi();
}

function restartLevel() {
  if (!app.session) {
    return;
  }
  startLevel(app.session.level);
}

function levelRect(object) {
  return {
    x: object.col * TILE,
    y: object.row * TILE,
    w: (object.w || 1) * TILE,
    h: (object.h || 1) * TILE
  };
}

function getSolidRects(level) {
  const solids = [
    { x: 0, y: level.groundRow * TILE, w: level.cols * TILE, h: (level.rows - level.groundRow) * TILE }
  ];
  level.objects.forEach((object) => {
    if (object.type === "block") {
      solids.push(levelRect(object));
    }
  });
  return solids;
}

function rectsIntersect(a, b) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
}

function queueJump() {
  const session = app.session;
  if (!session || !session.player.alive || session.player.won) {
    return;
  }
  const player = session.player;
  const level = session.level;
  if (player.onGround) {
    player.vy = -level.jumpVelocity;
    player.onGround = false;
    setStatus("Jump.");
    return;
  }
  for (let i = 0; i < level.objects.length; i += 1) {
    const object = level.objects[i];
    if (object.type !== "orb" || session.usedOrbs.has(i)) {
      continue;
    }
    const centerX = object.col * TILE + TILE / 2;
    const centerY = object.row * TILE + TILE / 2;
    const playerCenterX = player.x + PLAYER_SIZE / 2;
    const playerCenterY = player.y + PLAYER_SIZE / 2;
    const dx = centerX - playerCenterX;
    const dy = centerY - playerCenterY;
    if (dx * dx + dy * dy <= 58 * 58) {
      player.vy = -level.orbVelocity;
      session.usedOrbs.add(i);
      setStatus("Orb boost.");
      return;
    }
  }
}

function killPlayer(message) {
  if (!app.session) {
    return;
  }
  app.session.player.alive = false;
  setStatus(message);
}

function updatePlay(dt) {
  const session = app.session;
  if (!session) {
    return;
  }
  const level = session.level;
  const player = session.player;
  if (!player.alive || player.won) {
    return;
  }
  if (app.jumpQueued) {
    queueJump();
  }

  const prevX = player.x;
  const prevY = player.y;
  player.x += level.speed * dt;
  player.vy += level.gravity * dt;
  player.y += player.vy * dt;
  player.onGround = false;

  getSolidRects(level).forEach((solid) => {
    const playerRect = { x: player.x, y: player.y, w: PLAYER_SIZE, h: PLAYER_SIZE };
    if (!rectsIntersect(playerRect, solid)) {
      return;
    }
    const prevBottom = prevY + PLAYER_SIZE;
    if (player.vy >= 0 && prevBottom <= solid.y + 8) {
      player.y = solid.y - PLAYER_SIZE;
      player.vy = 0;
      player.onGround = true;
      return;
    }
    killPlayer("Crashed into a block.");
  });

  level.objects.forEach((object, index) => {
    const playerRect = { x: player.x, y: player.y, w: PLAYER_SIZE, h: PLAYER_SIZE };
    const rect = levelRect(object);
    if (object.type === "spike" && rectsIntersect(playerRect, rect)) {
      killPlayer("Spike hit.");
    }
    if (object.type === "pad") {
      const prevBottom = prevY + PLAYER_SIZE;
      if (rectsIntersect(playerRect, rect) && player.vy >= 0 && prevBottom <= rect.y + 12) {
        player.y = rect.y - PLAYER_SIZE;
        player.vy = -level.padVelocity;
        player.onGround = false;
        setStatus("Pad launch.");
      }
    }
    if (object.type === "coin" && !session.usedCoins.has(index) && rectsIntersect(playerRect, rect)) {
      session.usedCoins.add(index);
      player.coins += 1;
      setStatus(`Coin collected: ${player.coins}`);
    }
  });

  if (player.y > HEIGHT + 180) {
    killPlayer("Fell out of the level.");
  }

  if (player.x >= level.goalCol * TILE) {
    player.won = true;
    setStatus(`Level clear: ${level.name}`);
  }

  session.cameraX = Math.max(0, player.x - 220);
  session.time += dt;
}

function drawBackground(theme, cameraX) {
  const gradient = ctx.createLinearGradient(0, 0, 0, HEIGHT);
  if (theme === "sunset-drive") {
    gradient.addColorStop(0, "#14213d");
    gradient.addColorStop(0.45, "#2d5f73");
    gradient.addColorStop(1, "#f77f00");
  } else {
    gradient.addColorStop(0, "#0b132b");
    gradient.addColorStop(1, "#1c2541");
  }
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  for (let i = 0; i < 12; i += 1) {
    const x = (i * 220 - (cameraX * 0.25) % 240) - 60;
    ctx.fillStyle = "rgba(255,255,255,0.05)";
    ctx.fillRect(x, 110 + (i % 3) * 35, 160, 12);
  }

  ctx.fillStyle = "rgba(255, 191, 105, 0.28)";
  ctx.beginPath();
  ctx.arc(WIDTH - 180, 150, 80, 0, Math.PI * 2);
  ctx.fill();
}

function drawWorld(level, cameraX, session) {
  const groundTop = level.groundRow * TILE;
  ctx.fillStyle = "rgba(5, 9, 18, 0.22)";
  for (let i = 0; i <= level.cols; i += 1) {
    const x = i * TILE - cameraX;
    ctx.fillRect(x, 0, 1, HEIGHT);
  }
  for (let row = 0; row <= level.rows; row += 1) {
    const y = row * TILE;
    ctx.fillRect(0, y, WIDTH, 1);
  }

  ctx.fillStyle = "#23395d";
  ctx.fillRect(-cameraX, groundTop, level.cols * TILE, HEIGHT - groundTop);

  ctx.fillStyle = "#6fffe9";
  ctx.fillRect(level.goalCol * TILE - cameraX, 100, 10, groundTop - 100);

  level.objects.forEach((object, index) => {
    const x = object.col * TILE - cameraX;
    const y = object.row * TILE;
    if (object.type === "block") {
      ctx.fillStyle = "#7bdff2";
      ctx.fillRect(x, y, (object.w || 1) * TILE, (object.h || 1) * TILE);
      ctx.strokeStyle = "rgba(0,0,0,0.22)";
      ctx.strokeRect(x, y, (object.w || 1) * TILE, (object.h || 1) * TILE);
    }
    if (object.type === "spike") {
      ctx.fillStyle = "#ff4d6d";
      ctx.beginPath();
      ctx.moveTo(x, y + TILE);
      ctx.lineTo(x + TILE / 2, y);
      ctx.lineTo(x + TILE, y + TILE);
      ctx.closePath();
      ctx.fill();
    }
    if (object.type === "orb") {
      const used = session && session.usedOrbs.has(index);
      ctx.fillStyle = used ? "rgba(255,255,255,0.18)" : "#2ec4b6";
      ctx.beginPath();
      ctx.arc(x + TILE / 2, y + TILE / 2, 15, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = "#d8fff8";
      ctx.lineWidth = 3;
      ctx.stroke();
      ctx.lineWidth = 1;
    }
    if (object.type === "pad") {
      ctx.fillStyle = "#ffbf69";
      ctx.fillRect(x + 4, y + TILE - 12, TILE - 8, 12);
    }
    if (object.type === "coin") {
      const collected = session && session.usedCoins.has(index);
      if (!collected) {
        ctx.fillStyle = "#ffe066";
        ctx.beginPath();
        ctx.arc(x + TILE / 2, y + TILE / 2, 11, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  });
}

function drawPlayer(session) {
  const player = session.player;
  const screenX = player.x - session.cameraX;
  const angle = player.onGround ? 0 : session.time * 10;
  ctx.save();
  ctx.translate(screenX + PLAYER_SIZE / 2, player.y + PLAYER_SIZE / 2);
  ctx.rotate(angle);
  ctx.fillStyle = player.alive ? "#ffffff" : "#ff4d6d";
  ctx.fillRect(-PLAYER_SIZE / 2, -PLAYER_SIZE / 2, PLAYER_SIZE, PLAYER_SIZE);
  ctx.fillStyle = "#14213d";
  ctx.fillRect(-8, -6, 5, 5);
  ctx.fillRect(3, -6, 5, 5);
  ctx.restore();
}

function drawPlay() {
  const { level } = app.session;
  drawBackground(level.theme, app.session.cameraX);
  drawWorld(level, app.session.cameraX, app.session);
  drawPlayer(app.session);

  ctx.fillStyle = "rgba(0,0,0,0.25)";
  ctx.fillRect(24, 24, 250, 104);
  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 22px Trebuchet MS";
  ctx.fillText(level.name, 42, 58);
  ctx.font = "16px Trebuchet MS";
  ctx.fillText(`Coins: ${app.session.player.coins}`, 42, 84);
  ctx.fillText(`Progress: ${Math.min(100, Math.floor((app.session.player.x / (level.goalCol * TILE)) * 100))}%`, 42, 108);
}

function drawEditor() {
  drawBackground(app.editorLevel.theme, app.editorCameraCol * TILE);
  drawWorld(app.editorLevel, app.editorCameraCol * TILE, null);
  ctx.fillStyle = "rgba(255,255,255,0.9)";
  ctx.font = "18px Trebuchet MS";
  ctx.fillText(`Editor camera col: ${app.editorCameraCol}`, 30, 44);
  ctx.fillText(`Tool: ${app.editorTool}`, 30, 70);
}

function drawMenuPreview() {
  drawBackground("sunset-drive", app.lastTimestamp * 0.03);
  drawWorld(makePulseRun(), 320, null);
}

function frame(timestamp) {
  const dt = Math.min(0.033, (timestamp - app.lastTimestamp) / 1000 || 0.016);
  app.lastTimestamp = timestamp;
  app.jumpQueued = Boolean(app.jumpQueued);

  if (app.screen === "play") {
    updatePlay(dt);
    drawPlay();
  } else if (app.screen === "editor") {
    drawEditor();
  } else {
    drawMenuPreview();
  }

  app.jumpQueued = false;
  requestAnimationFrame(frame);
}

function renderUi() {
  renderOverlay();
  renderTools();
}

function renderOverlay() {
  if (app.screen === "menu") {
    overlay.innerHTML = `
      <section class="overlay-card">
        <div class="overlay-grid">
          <span class="tag">Original build, not a literal clone</span>
          <h2>Geometry Jump starts with real code, not placeholders.</h2>
          <p>This repo gives you a browser version, a Java desktop port, and ports in Python, C++, and Lua using the same core level model.</p>
          <div class="tool-stack">
            <button class="primary" data-action="open-levels">Play demo level</button>
            <button class="secondary" data-action="open-editor">Open the editor</button>
            <button data-action="open-community">Open community mock</button>
          </div>
        </div>
      </section>
    `;
  } else if (app.screen === "levels") {
    overlay.innerHTML = `
      <section class="overlay-card">
        <div class="overlay-grid">
          <span class="tag">Level Select</span>
          <h2>Pick a run.</h2>
          <div class="level-list">
            ${allLevels().map((level, index) => `
              <article class="level-card">
                <header>
                  <div>
                    <h3>${level.name}</h3>
                    <p class="meta">By ${level.author}</p>
                  </div>
                  <span class="tag">${level.speed}px/s</span>
                </header>
                <div class="level-actions">
                  <button class="primary" data-action="play-level" data-index="${index}">Play</button>
                  <button data-action="edit-level" data-index="${index}">Copy into editor</button>
                </div>
              </article>
            `).join("")}
          </div>
        </div>
      </section>
    `;
  } else if (app.screen === "community") {
    const communityLevels = [...app.builtinLevels, ...app.publishedLevels];
    overlay.innerHTML = `
      <section class="overlay-card">
        <div class="overlay-grid">
          <span class="tag">Local community mock</span>
          <h2>Featured uploads</h2>
          <p>This screen simulates a community tab with local storage only. There is no real backend yet.</p>
          <div class="level-list">
            ${communityLevels.map((level, index) => `
              <article class="level-card">
                <header>
                  <div>
                    <h3>${level.name}</h3>
                    <p class="meta">By ${level.author}</p>
                  </div>
                  <span class="tag">${level.theme}</span>
                </header>
                <div class="level-actions">
                  <button class="primary" data-action="community-play" data-index="${index}">Play</button>
                  <button data-action="community-import" data-index="${index}">Import into editor</button>
                </div>
              </article>
            `).join("")}
          </div>
        </div>
      </section>
    `;
  } else if (app.screen === "play") {
    const player = app.session.player;
    const title = player.won ? "Level cleared" : player.alive ? "In motion" : "Run failed";
    const description = player.won
      ? "You reached the goal. Restart, edit the level, or go back to select another run."
      : player.alive
        ? "Space, W, Up Arrow, or mouse click to jump."
        : "Restart the run or move the level into the editor for tuning.";
    overlay.innerHTML = `
      <section class="overlay-card" style="align-self:flex-start; margin-top: 18px; width: min(440px, 100%);">
        <div class="overlay-grid">
          <span class="tag">${title}</span>
          <p>${description}</p>
          <div class="tool-stack">
            <button class="primary" data-action="restart-level">Restart</button>
            <button data-action="open-levels">Back to levels</button>
            <button data-action="edit-current-level">Send to editor</button>
          </div>
        </div>
      </section>
    `;
  } else {
    overlay.innerHTML = "";
  }

  overlay.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", handleAction);
  });
}

function renderTools() {
  if (app.screen === "editor") {
    toolsPanel.innerHTML = `
      <div class="card">
        <h2>Editor</h2>
        <p>Edit a simple tile-based level. Use keys 1-5 to swap tools, A/D to scroll, and right click to erase.</p>
      </div>
      <div class="card">
        <label>Level name</label>
        <input id="levelName" value="${escapeHtml(app.editorLevel.name)}">
      </div>
      <div class="card">
        <label>Author</label>
        <input id="levelAuthor" value="${escapeHtml(app.editorLevel.author)}">
      </div>
      <div class="card">
        <h3>Tools</h3>
        <div class="tool-stack">
          ${["block", "spike", "orb", "pad", "coin", "erase"].map((tool) => `
            <button class="${tool === app.editorTool ? "active" : ""}" data-tool="${tool}">${tool}</button>
          `).join("")}
        </div>
      </div>
      <div class="card">
        <div class="tool-stack">
          <button class="primary" data-action="playtest-editor">Playtest</button>
          <button class="secondary" data-action="save-editor">Save local level</button>
          <button data-action="publish-editor">Publish to community mock</button>
          <button data-action="new-editor">New blank level</button>
        </div>
      </div>
      <div class="card">
        <label>Export JSON</label>
        <textarea readonly>${escapeHtml(JSON.stringify(app.editorLevel, null, 2))}</textarea>
      </div>
      <p class="fineprint">This editor is intentionally simple. It is a starter, not a full Geometry Dash-style production editor.</p>
    `;

    const nameInput = toolsPanel.querySelector("#levelName");
    const authorInput = toolsPanel.querySelector("#levelAuthor");
    nameInput.addEventListener("input", (event) => {
      app.editorLevel.name = event.target.value || "Untitled Pulse";
      app.editorLevel.id = slugify(app.editorLevel.name);
    });
    authorInput.addEventListener("input", (event) => {
      app.editorLevel.author = event.target.value || "Local Creator";
    });
    toolsPanel.querySelectorAll("[data-tool]").forEach((button) => {
      button.addEventListener("click", () => {
        app.editorTool = button.dataset.tool;
        renderTools();
      });
    });
    toolsPanel.querySelectorAll("[data-action]").forEach((button) => {
      button.addEventListener("click", handleAction);
    });
    return;
  }

  toolsPanel.innerHTML = `
    <div class="card">
      <h2>About this build</h2>
      <p>This is an original project starter. It does not include copyrighted Geometry Dash assets or a real online backend.</p>
    </div>
    <div class="card">
      <h3>Shortcuts</h3>
      <p class="fineprint">Jump: Space, W, Up Arrow, or click. In the editor: 1-5 changes tool, A/D scroll, Delete switches to erase.</p>
    </div>
    <div class="card">
      <h3>Workspace</h3>
      <p class="fineprint">The browser version is the easiest one to try immediately. The Java, Python, C++, and Lua ports are included in their own folders.</p>
    </div>
  `;
}

function handleAction(event) {
  const action = event.currentTarget.dataset.action;
  if (action === "open-levels") goTo("levels");
  if (action === "open-editor") goTo("editor");
  if (action === "open-community") goTo("community");
  if (action === "restart-level") restartLevel();
  if (action === "play-level") startLevel(allLevels()[Number(event.currentTarget.dataset.index)]);
  if (action === "community-play") startLevel([...app.builtinLevels, ...app.publishedLevels][Number(event.currentTarget.dataset.index)]);
  if (action === "edit-level") {
    app.editorLevel = deepClone(allLevels()[Number(event.currentTarget.dataset.index)]);
    goTo("editor");
  }
  if (action === "community-import") {
    app.editorLevel = deepClone([...app.builtinLevels, ...app.publishedLevels][Number(event.currentTarget.dataset.index)]);
    goTo("editor");
  }
  if (action === "edit-current-level" && app.session) {
    app.editorLevel = deepClone(app.session.level);
    goTo("editor");
  }
  if (action === "playtest-editor") {
    startLevel(app.editorLevel);
  }
  if (action === "new-editor") {
    app.editorLevel = makeBlankLevel();
    app.editorTool = "block";
    app.editorCameraCol = 0;
    setStatus("New blank level ready.");
    renderUi();
  }
  if (action === "save-editor") {
    const saved = allLevels().filter((level) => !app.builtinLevels.some((builtin) => builtin.id === level.id));
    const next = saved.filter((level) => level.id !== app.editorLevel.id);
    next.push(deepClone(app.editorLevel));
    app.userLevels = next;
    saveCollection("gj-user-levels", app.userLevels);
    setStatus(`Saved ${app.editorLevel.name} locally.`);
    renderUi();
  }
  if (action === "publish-editor") {
    const next = app.publishedLevels.filter((level) => level.id !== app.editorLevel.id);
    next.push(deepClone(app.editorLevel));
    app.publishedLevels = next;
    saveCollection("gj-published-levels", app.publishedLevels);
    setStatus(`Published ${app.editorLevel.name} to the local community mock.`);
    renderUi();
  }
}

function handleEditorPointer(point, erase) {
  const col = Math.floor(point.x / TILE) + app.editorCameraCol;
  const row = Math.floor(point.y / TILE);
  if (row >= app.editorLevel.groundRow || col < 0 || col >= app.editorLevel.cols || row < 0 || row >= app.editorLevel.rows) {
    return;
  }
  const type = erase ? "erase" : app.editorTool;
  app.editorLevel.objects = app.editorLevel.objects.filter((object) => {
    const sameCell = object.col === col && object.row === row;
    return !(sameCell && (type === "erase" || object.type === type || object.type !== "block"));
  });
  if (type !== "erase") {
    const object = { type, col, row };
    if (type === "block") {
      object.w = 1;
      object.h = 1;
    }
    app.editorLevel.objects.push(object);
  }
  setStatus(`Placed ${type} at ${col}, ${row}.`);
  renderTools();
}

function slugify(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "") || `custom-${Date.now()}`;
}

function canvasPoint(event) {
  const rect = canvas.getBoundingClientRect();
  const scaleX = WIDTH / rect.width;
  const scaleY = HEIGHT / rect.height;
  return {
    x: (event.clientX - rect.left) * scaleX,
    y: (event.clientY - rect.top) * scaleY
  };
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

goTo("menu");
requestAnimationFrame(frame);
