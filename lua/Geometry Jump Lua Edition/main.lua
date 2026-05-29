local WIDTH, HEIGHT, SIDEBAR = 1280, 720, 320
local TILE, PLAYER_SIZE = 40, 32

local function clone(value)
  local copy = {}
  for key, item in pairs(value) do
    if type(item) == "table" then
      copy[key] = clone(item)
    else
      copy[key] = item
    end
  end
  return copy
end

local function makeBlankLevel()
  return {
    id = "untitled-pulse",
    name = "Untitled Pulse",
    author = "Local Creator",
    theme = "sunset-drive",
    speed = 300,
    gravity = 1900,
    jumpVelocity = 720,
    orbVelocity = 760,
    padVelocity = 880,
    groundRow = 10,
    rows = 12,
    cols = 80,
    goalCol = 72,
    objects = {}
  }
end

local function makePulseRun()
  local level = makeBlankLevel()
  level.id = "pulse-run"
  level.name = "Pulse Run"
  level.author = "Geometry Jump Team"
  level.objects = {
    { type = "block", col = 11, row = 8, w = 2, h = 1 },
    { type = "spike", col = 14, row = 9 },
    { type = "orb", col = 16, row = 6 },
    { type = "block", col = 18, row = 7, w = 3, h = 1 },
    { type = "coin", col = 19, row = 5 },
    { type = "pad", col = 24, row = 9 },
    { type = "block", col = 28, row = 6, w = 2, h = 1 },
    { type = "spike", col = 31, row = 9 },
    { type = "spike", col = 32, row = 9 },
    { type = "block", col = 35, row = 8, w = 4, h = 1 },
    { type = "coin", col = 37, row = 6 },
    { type = "orb", col = 40, row = 5 },
    { type = "block", col = 44, row = 7, w = 3, h = 1 },
    { type = "pad", col = 50, row = 9 },
    { type = "spike", col = 54, row = 9 },
    { type = "block", col = 57, row = 6, w = 2, h = 1 },
    { type = "coin", col = 58, row = 4 },
    { type = "orb", col = 61, row = 5 },
    { type = "block", col = 65, row = 8, w = 3, h = 1 },
    { type = "spike", col = 69, row = 9 }
  }
  return level
end

local app = {
  screen = "menu",
  status = "Lua port ready.",
  builtin = { makePulseRun() },
  user = {},
  published = {},
  editor = makeBlankLevel(),
  tool = "block",
  editorCameraCol = 0,
  session = nil,
  buttons = {}
}

local function replaceById(list, level)
  for index, item in ipairs(list) do
    if item.id == level.id then
      list[index] = clone(level)
      return
    end
  end
  table.insert(list, clone(level))
end

local function allLevels()
  local levels = {}
  for _, level in ipairs(app.builtin) do table.insert(levels, level) end
  for _, level in ipairs(app.user) do table.insert(levels, level) end
  return levels
end

local function startLevel(source)
  local level = clone(source)
  app.session = {
    level = level,
    player = {
      x = 3 * TILE,
      y = (level.groundRow - 1) * TILE - PLAYER_SIZE,
      vy = 0,
      onGround = true,
      alive = true,
      won = false,
      coins = 0
    },
    cameraX = 0,
    time = 0,
    usedOrbs = {},
    usedCoins = {}
  }
  app.screen = "play"
  app.status = "Playing " .. level.name
end

local function contains(list, value)
  for _, item in ipairs(list) do
    if item == value then return true end
  end
  return false
end

local function rectsIntersect(a, b)
  return a.x < b.x + b.w and a.x + a.w > b.x and a.y < b.y + b.h and a.y + a.h > b.y
end

local function queueJump()
  if not app.session then return end
  local player = app.session.player
  local level = app.session.level
  if not player.alive or player.won then return end
  if player.onGround then
    player.vy = -level.jumpVelocity
    player.onGround = false
    return
  end
  for index, obj in ipairs(level.objects) do
    if obj.type == "orb" and not contains(app.session.usedOrbs, index) then
      local dx = (obj.col * TILE + TILE / 2) - (player.x + PLAYER_SIZE / 2)
      local dy = (obj.row * TILE + TILE / 2) - (player.y + PLAYER_SIZE / 2)
      if dx * dx + dy * dy <= 58 * 58 then
        player.vy = -level.orbVelocity
        table.insert(app.session.usedOrbs, index)
        return
      end
    end
  end
end

local function updatePlay(dt)
  if not app.session then return end
  local level = app.session.level
  local player = app.session.player
  if not player.alive or player.won then return end

  local prevY = player.y
  player.x = player.x + level.speed * dt
  player.vy = player.vy + level.gravity * dt
  player.y = player.y + player.vy * dt
  player.onGround = false

  local solids = {
    { x = 0, y = level.groundRow * TILE, w = level.cols * TILE, h = (level.rows - level.groundRow) * TILE }
  }
  for _, obj in ipairs(level.objects) do
    if obj.type == "block" then
      table.insert(solids, { x = obj.col * TILE, y = obj.row * TILE, w = (obj.w or 1) * TILE, h = (obj.h or 1) * TILE })
    end
  end

  local playerRect = { x = player.x, y = player.y, w = PLAYER_SIZE, h = PLAYER_SIZE }
  for _, solid in ipairs(solids) do
    if rectsIntersect(playerRect, solid) then
      if player.vy >= 0 and prevY + PLAYER_SIZE <= solid.y + 8 then
        player.y = solid.y - PLAYER_SIZE
        player.vy = 0
        player.onGround = true
      else
        player.alive = false
        app.status = "Crashed."
        return
      end
      playerRect.y = player.y
    end
  end

  for index, obj in ipairs(level.objects) do
    local rect = { x = obj.col * TILE, y = obj.row * TILE, w = (obj.w or 1) * TILE, h = (obj.h or 1) * TILE }
    if obj.type == "spike" and rectsIntersect(playerRect, rect) then
      player.alive = false
      app.status = "Spike hit."
    end
    if obj.type == "pad" and rectsIntersect(playerRect, rect) and player.vy >= 0 and prevY + PLAYER_SIZE <= rect.y + 12 then
      player.y = rect.y - PLAYER_SIZE
      player.vy = -level.padVelocity
      player.onGround = false
    end
    if obj.type == "coin" and not contains(app.session.usedCoins, index) and rectsIntersect(playerRect, rect) then
      table.insert(app.session.usedCoins, index)
      player.coins = player.coins + 1
    end
  end

  if player.x >= level.goalCol * TILE then
    player.won = true
    app.status = "Cleared " .. level.name
  end
  app.session.cameraX = math.max(0, player.x - 220)
  app.session.time = app.session.time + dt
end

local function button(x, y, w, h, label, action, fill, text)
  fill = fill or { 1, 1, 1, 1 }
  text = text or { 0.08, 0.13, 0.24, 1 }
  love.graphics.setColor(fill)
  love.graphics.rectangle("fill", x, y, w, h, 14, 14)
  love.graphics.setColor(text)
  love.graphics.print(label, x + 14, y + 12)
  table.insert(app.buttons, { x = x, y = y, w = w, h = h, action = action })
end

local function drawWorld(level, cameraX, session)
  local stageWidth = WIDTH - SIDEBAR
  love.graphics.setColor(0.14, 0.22, 0.36, 1)
  for i = 0, level.cols do
    local x = i * TILE - cameraX
    love.graphics.line(x, 0, x, HEIGHT)
  end
  for row = 0, level.rows do
    love.graphics.line(0, row * TILE, stageWidth, row * TILE)
  end
  love.graphics.setColor(0.14, 0.22, 0.36, 1)
  love.graphics.rectangle("fill", 0, level.groundRow * TILE, stageWidth, HEIGHT - level.groundRow * TILE)
  love.graphics.setColor(0.44, 1.0, 0.91, 1)
  love.graphics.rectangle("fill", level.goalCol * TILE - cameraX, 100, 10, level.groundRow * TILE - 100)
  for index, obj in ipairs(level.objects) do
    local x = obj.col * TILE - cameraX
    local y = obj.row * TILE
    if obj.type == "block" then
      love.graphics.setColor(0.48, 0.87, 0.95, 1)
      love.graphics.rectangle("fill", x, y, (obj.w or 1) * TILE, (obj.h or 1) * TILE)
    elseif obj.type == "spike" then
      love.graphics.setColor(1.0, 0.3, 0.43, 1)
      love.graphics.polygon("fill", x, y + TILE, x + TILE / 2, y, x + TILE, y + TILE)
    elseif obj.type == "orb" then
      love.graphics.setColor(session and contains(session.usedOrbs, index) and { 0.8, 0.8, 0.8, 0.3 } or { 0.18, 0.77, 0.71, 1 })
      love.graphics.circle("fill", x + TILE / 2, y + TILE / 2, 14)
    elseif obj.type == "pad" then
      love.graphics.setColor(1.0, 0.75, 0.41, 1)
      love.graphics.rectangle("fill", x + 4, y + TILE - 12, TILE - 8, 12)
    elseif obj.type == "coin" and not (session and contains(session.usedCoins, index)) then
      love.graphics.setColor(1.0, 0.88, 0.4, 1)
      love.graphics.circle("fill", x + TILE / 2, y + TILE / 2, 9)
    end
  end
end

function love.load()
  love.window.setMode(WIDTH, HEIGHT, { resizable = false })
  love.window.setTitle("Geometry Jump - Lua")
  love.graphics.setFont(love.graphics.newFont(18))
end

function love.update(dt)
  if app.screen == "play" then
    updatePlay(dt)
  end
end

function love.draw()
  app.buttons = {}
  love.graphics.clear(0.08, 0.13, 0.24, 1)
  love.graphics.setColor(0.08, 0.13, 0.24, 1)
  love.graphics.rectangle("fill", 0, 0, WIDTH, HEIGHT)
  love.graphics.setColor(0.97, 0.5, 0.0, 0.4)
  love.graphics.circle("fill", WIDTH - 180, 150, 80)

  if app.screen == "play" and app.session then
    drawWorld(app.session.level, app.session.cameraX, app.session)
    love.graphics.setColor(1, 1, 1, 1)
    love.graphics.rectangle("fill", app.session.player.x - app.session.cameraX, app.session.player.y, PLAYER_SIZE, PLAYER_SIZE)
    button(330, 24, 110, 38, "Restart", "restart", { 1.0, 0.62, 0.11, 1 })
    button(450, 24, 110, 38, "Home", "home")
  elseif app.screen == "editor" then
    drawWorld(app.editor, app.editorCameraCol * TILE)
  else
    drawWorld(app.builtin[1], 320)
  end

  love.graphics.setColor(0.03, 0.07, 0.14, 0.94)
  love.graphics.rectangle("fill", WIDTH - SIDEBAR, 0, SIDEBAR, HEIGHT)
  love.graphics.setColor(1, 1, 1, 1)
  love.graphics.print("Status", WIDTH - SIDEBAR + 24, 34)
  love.graphics.print(app.status, WIDTH - SIDEBAR + 24, 70)

  if app.screen == "menu" then
    love.graphics.print("Geometry Jump", 100, 130)
    love.graphics.print("Lua Love2D starter with the same level and gameplay model.", 100, 170)
    button(100, 250, 220, 46, "Play Demo", "menu-play", { 1.0, 0.62, 0.11, 1 })
    button(100, 308, 220, 46, "Open Editor", "menu-editor", { 0.18, 0.77, 0.71, 1 })
    button(100, 366, 220, 46, "Community", "menu-community")
  elseif app.screen == "levels" then
    love.graphics.print("Level Select", 90, 100)
    local y = 150
    for index, level in ipairs(allLevels()) do
      love.graphics.setColor(0.03, 0.07, 0.14, 0.88)
      love.graphics.rectangle("fill", 90, y, 530, 88, 18, 18)
      love.graphics.setColor(1, 1, 1, 1)
      love.graphics.print(level.name, 110, y + 18)
      love.graphics.print("By " .. level.author, 110, y + 48)
      button(470, y + 18, 70, 36, "Play", "play:" .. index, { 1.0, 0.62, 0.11, 1 })
      button(550, y + 18, 70, 36, "Edit", "edit:" .. index)
      y = y + 98
    end
    button(WIDTH - SIDEBAR + 24, 140, 250, 38, "Home", "home")
  elseif app.screen == "community" then
    love.graphics.print("Community Mock", 90, 100)
    local levels = {}
    for _, level in ipairs(app.builtin) do table.insert(levels, level) end
    for _, level in ipairs(app.published) do table.insert(levels, level) end
    local y = 150
    for index, level in ipairs(levels) do
      love.graphics.setColor(0.03, 0.07, 0.14, 0.88)
      love.graphics.rectangle("fill", 90, y, 530, 88, 18, 18)
      love.graphics.setColor(1, 1, 1, 1)
      love.graphics.print(level.name, 110, y + 18)
      love.graphics.print("By " .. level.author, 110, y + 48)
      button(470, y + 18, 150, 36, "Import", "community:" .. index)
      y = y + 98
    end
    button(WIDTH - SIDEBAR + 24, 140, 250, 38, "Home", "home")
  elseif app.screen == "editor" then
    love.graphics.print("Editor", WIDTH - SIDEBAR + 24, 120)
    love.graphics.print(app.editor.name, WIDTH - SIDEBAR + 24, 160)
    love.graphics.print("Keys 1-5 swap tools, A/D scroll.", WIDTH - SIDEBAR + 24, 194)
    button(WIDTH - SIDEBAR + 24, 250, 120, 36, "Playtest", "playtest", { 1.0, 0.62, 0.11, 1 })
    button(WIDTH - SIDEBAR + 154, 250, 120, 36, "Save", "save", { 0.18, 0.77, 0.71, 1 })
    button(WIDTH - SIDEBAR + 24, 298, 250, 36, "Publish", "publish")
    button(WIDTH - SIDEBAR + 24, 346, 250, 36, "New Blank", "new")
    button(WIDTH - SIDEBAR + 24, 394, 250, 36, "Home", "home")
  end
end

function love.mousepressed(x, y, mouseButton)
  for _, info in ipairs(app.buttons) do
    if x >= info.x and x <= info.x + info.w and y >= info.y and y <= info.y + info.h then
      local action = info.action
      if action == "menu-play" then app.screen = "levels"
      elseif action == "menu-editor" then app.screen = "editor"
      elseif action == "menu-community" then app.screen = "community"
      elseif action == "home" then app.screen = "menu"
      elseif action == "restart" and app.session then startLevel(app.session.level)
      elseif action == "save" then replaceById(app.user, app.editor)
      elseif action == "publish" then replaceById(app.published, app.editor)
      elseif action == "new" then app.editor = makeBlankLevel()
      elseif action == "playtest" then startLevel(app.editor)
      elseif action:match("^play:") then startLevel(allLevels()[tonumber(action:sub(6))])
      elseif action:match("^edit:") then app.editor = clone(allLevels()[tonumber(action:sub(6))]); app.screen = "editor"
      elseif action:match("^community:") then
        local levels = {}
        for _, level in ipairs(app.builtin) do table.insert(levels, level) end
        for _, level in ipairs(app.published) do table.insert(levels, level) end
        app.editor = clone(levels[tonumber(action:sub(11))])
        app.screen = "editor"
      end
      return
    end
  end

  if app.screen == "play" and mouseButton == 1 then
    queueJump()
  elseif app.screen == "editor" and x < WIDTH - SIDEBAR then
    local col = math.floor(x / TILE) + app.editorCameraCol
    local row = math.floor(y / TILE)
    if row < app.editor.groundRow and col >= 0 and col < app.editor.cols then
      local kept = {}
      for _, obj in ipairs(app.editor.objects) do
        if not (obj.col == col and obj.row == row) then
          table.insert(kept, obj)
        end
      end
      app.editor.objects = kept
      if mouseButton ~= 2 and app.tool ~= "erase" then
        table.insert(app.editor.objects, { type = app.tool, col = col, row = row, w = 1, h = 1 })
      end
    end
  end
end

function love.keypressed(key)
  if app.screen == "play" and (key == "space" or key == "w" or key == "up") then
    queueJump()
  end
  if app.screen == "editor" then
    if key == "1" then app.tool = "block" end
    if key == "2" then app.tool = "spike" end
    if key == "3" then app.tool = "orb" end
    if key == "4" then app.tool = "pad" end
    if key == "5" then app.tool = "coin" end
    if key == "delete" or key == "backspace" then app.tool = "erase" end
    if key == "a" then app.editorCameraCol = math.max(0, app.editorCameraCol - 2) end
    if key == "d" then app.editorCameraCol = math.min(app.editor.cols - 24, app.editorCameraCol + 2) end
  end
end
