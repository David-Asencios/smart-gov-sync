const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const pool = require("../db");

const router = express.Router();

router.post("/login", async (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ error: "Credenciales requeridas" });
  }
  const result = await pool.query("select * from usuario where username = $1", [username]);
  const user = result.rows[0];
  if (!user) {
    return res.status(401).json({ error: "Credenciales invalidas" });
  }
  const valid = await bcrypt.compare(password, user.password_hash);
  if (!valid) {
    return res.status(401).json({ error: "Credenciales invalidas" });
  }
  const token = jwt.sign({ id_usuario: user.id_usuario, username: user.username }, process.env.JWT_SECRET, { expiresIn: "8h" });
  res.json({ token });
});

module.exports = router;
