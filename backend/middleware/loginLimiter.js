const attempts = new Map();
const WINDOW_MS = 15 * 60 * 1000;
const MAX_ATTEMPTS = 10;

module.exports = function loginLimiter(req, res, next) {
  const now = Date.now();
  const username = String(req.body && req.body.username || "").trim().toLowerCase();
  const key = `${req.ip}:${username}`;
  res.on("finish", () => {
    if (res.statusCode < 400) attempts.delete(key);
  });
  if (attempts.size > 10000) {
    for (const [entryKey, entry] of attempts) {
      if (entry.resetAt <= now) attempts.delete(entryKey);
    }
  }
  const current = attempts.get(key);
  if (!current || current.resetAt <= now) {
    attempts.set(key, { count: 1, resetAt: now + WINDOW_MS });
    return next();
  }
  current.count += 1;
  if (current.count > MAX_ATTEMPTS) {
    res.set("Retry-After", String(Math.ceil((current.resetAt - now) / 1000)));
    return res.status(429).json({ error: "Demasiados intentos. Intente nuevamente mas tarde" });
  }
  next();
};
