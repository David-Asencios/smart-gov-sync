require("dotenv").config();
const fs = require("node:fs");
const path = require("node:path");
const pool = require("../db");

async function main() {
  const directory = path.join(__dirname, "migrations");
  const files = fs.readdirSync(directory).filter(name => name.endsWith(".sql")).sort();
  await pool.query(`create table if not exists schema_migrations (
    filename varchar(255) primary key,
    applied_at timestamptz not null default now()
  )`);
  for (const filename of files) {
    const applied = await pool.query("select 1 from schema_migrations where filename = $1", [filename]);
    if (applied.rowCount) continue;
    const sql = fs.readFileSync(path.join(directory, filename), "utf8");
    await pool.query(sql);
    await pool.query("insert into schema_migrations(filename) values ($1)", [filename]);
    console.log(`Migracion aplicada: ${filename}`);
  }
}

main().then(() => pool.end()).catch(async error => {
  console.error(error);
  await pool.end().catch(() => {});
  process.exit(1);
});
