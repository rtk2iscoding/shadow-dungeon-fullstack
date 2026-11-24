const canvas = document.querySelector('#game');
const context = canvas.getContext('2d');
const overlay = document.querySelector('#overlay');
const status = document.querySelector('#status');
const retry = document.querySelector('#retry');
const room = document.querySelector('#room');

const keyCodes = ['KeyA', 'KeyD', 'KeyW', 'KeyS', 'KeyM', 'KeyR',
  'Space', 'Enter', 'KeyP', 'KeyE', 'KeyL', 'KeyK'];
const keyMasks = new Map(keyCodes.map((key, index) => [key, 1 << index]));
const roomNames = { prep: '准备室', A: '战斗室 A', B: '战斗室 B', end: '终点室' };
const sprites = new Map();
const tickDuration = 1000 / 60;

let session;
let held = 0;
let pressed = 0;
let firing = false;
let mouseX = canvas.width / 2;
let mouseY = canvas.height / 2;
let lastTick = 0;

function releaseInput() {
  held = 0;
  pressed = 0;
  firing = false;
}

canvas.addEventListener('keydown', event => {
  const mask = keyMasks.get(event.code);
  if (!mask || !session) return;
  event.preventDefault();
  if (!event.repeat) pressed |= mask;
  held |= mask;
});

window.addEventListener('keyup', event => {
  held &= ~(keyMasks.get(event.code) ?? 0);
});
canvas.addEventListener('blur', releaseInput);
window.addEventListener('blur', releaseInput);
document.addEventListener('visibilitychange', releaseInput);

function trackPointer(event) {
  const bounds = canvas.getBoundingClientRect();
  mouseX = Math.max(0, Math.min(canvas.width, (event.clientX - bounds.left) * canvas.width / bounds.width));
  mouseY = Math.max(0, Math.min(canvas.height, (event.clientY - bounds.top) * canvas.height / bounds.height));
}

canvas.addEventListener('pointermove', trackPointer);
canvas.addEventListener('pointerdown', event => {
  if (event.pointerType === 'touch' || event.button !== 0) return;
  canvas.focus({ preventScroll: true });
  trackPointer(event);
  firing = true;
});
window.addEventListener('pointerup', () => { firing = false; });
window.addEventListener('pointercancel', () => { firing = false; });

function loadSprite(path) {
  if (!sprites.has(path)) {
    const image = new Image();
    image.src = '/' + path;
    const loading = image.decode().then(() => image).catch(error => {
      sprites.delete(path);
      throw error;
    });
    sprites.set(path, loading);
  }
  return sprites.get(path);
}

async function drawFrame(frame) {
  const [currentRoom, ...rows] = frame.trimEnd().split('\n');
  const commands = rows.map(row => row.split('\t'));
  const images = await Promise.all(commands.map(command =>
    command[0] === 'image' ? loadSprite(command[3]) : null));
  context.clearRect(0, 0, canvas.width, canvas.height);
  commands.forEach(([type, x, y, value, centered, text], index) => {
    if (type === 'image') {
      const image = images[index];
      context.drawImage(image, Number(x) - image.width / 2, Number(y) - image.height / 2);
    } else if (type === 'text') {
      context.font = `${value}px Wheaton, sans-serif`;
      context.fillStyle = '#fff';
      context.textAlign = centered === 'true' ? 'center' : 'left';
      context.fillText(text, Number(x), Number(y));
    }
  });
  room.textContent = roomNames[currentRoom];
  room.hidden = false;
}

async function update(now) {
  if (!session) return;
  const elapsed = now - lastTick;
  if (document.hidden || elapsed + 0.5 < tickDuration) {
    if (document.hidden) lastTick = now;
    requestAnimationFrame(update);
    return;
  }

  const ticks = Math.max(1, Math.min(4, Math.floor((elapsed + 0.5) / tickDuration)));
  lastTick = elapsed > 4 * tickDuration ? now : lastTick + ticks * tickDuration;
  try {
    const body = [ticks, held, pressed, Number(firing), mouseX, mouseY].join(' ');
    pressed = 0;
    const response = await fetch('/api/frame', {
      method: 'POST',
      headers: { 'X-Game-Session': session },
      body,
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) throw new Error('Frame request failed');
    await drawFrame(await response.text());
    if (!overlay.hidden) {
      overlay.hidden = true;
      canvas.focus({ preventScroll: true });
    }
    requestAnimationFrame(update);
  } catch {
    showError('游戏连接中断，请重新开始。');
  }
}

function showError(message) {
  session = null;
  releaseInput();
  status.textContent = message;
  overlay.hidden = false;
  retry.hidden = false;
  room.hidden = true;
}

async function start() {
  releaseInput();
  retry.hidden = true;
  status.textContent = '连接 Java 后端…';
  overlay.hidden = false;
  try {
    const response = await fetch('/api/session', {
      method: 'POST',
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) throw new Error('Session request failed');
    const [id, width, height] = (await response.text()).split('\n');
    canvas.width = Number(width);
    canvas.height = Number(height);
    mouseX = canvas.width / 2;
    mouseY = canvas.height / 2;
    await document.fonts.load('24px Wheaton').catch(() => []);
    session = id;
    lastTick = performance.now() - tickDuration;
    requestAnimationFrame(update);
  } catch {
    showError('无法连接 Java 后端。请确认服务已经启动。');
  }
}

retry.addEventListener('click', start);
start();
