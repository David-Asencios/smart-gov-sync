require("dotenv").config();
const bcrypt = require("bcryptjs");
const pool = require("./db");

async function main() {
  const passwordHash = await bcrypt.hash("admin123", 10);
  await pool.query(`
    insert into usuario (username, password_hash, updated_at)
    values ($1, $2, $3)
    on conflict (username)
    do update set password_hash = excluded.password_hash, updated_at = excluded.updated_at
  `, ["admin", passwordHash, Date.now()]);
  await pool.end();
}

main().catch(async error => {
  console.error(error);
  await pool.end();
  process.exit(1);
});
