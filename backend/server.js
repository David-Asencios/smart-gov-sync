require("dotenv").config();
const express = require("express");
const cors = require("cors");
const auth = require("./middleware/auth");

const app = express();
app.use(cors());
app.use(express.json({ limit: "10mb" }));

app.get("/", (req, res) => res.json({ nombre: "Smart-Gov Sync" }));
app.get("/health", async (req, res) => {
  try {
    await require("./db").query("select 1");
    res.json({
      ok: true,
      database: true,
      jwt: Boolean(process.env.JWT_SECRET)
    });
  } catch (error) {
    res.status(500).json({
      ok: false,
      database: false,
      jwt: Boolean(process.env.JWT_SECRET),
      error: error.message
    });
  }
});
app.use("/", require("./routes/auth"));
app.use(auth);
app.use("/oficinas", require("./routes/oficinas"));
app.use("/tipos-documentos", require("./routes/tipos_documentos"));
app.use("/administrados", require("./routes/administrados"));
app.use("/personal", require("./routes/personal"));
app.use("/direcciones", require("./routes/direcciones"));
app.use("/expedientes", require("./routes/expedientes"));
app.use("/documentos", require("./routes/documentos"));
app.use("/hojas-ruta", require("./routes/hojas_ruta"));
app.use("/archivo-fisico", require("./routes/archivo_fisico"));
app.use("/actas", require("./routes/actas"));
app.use("/", require("./routes/sincronizacion"));

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`Servidor iniciado en ${port}`));
