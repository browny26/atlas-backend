import express from "express";
import cors from "cors";
import travelRoutes from "./routes/travel.js";

const app = express();
app.use(cors());
app.use(express.json());

// Rotte
app.use("/api/travel", travelRoutes);

const PORT = 5000;
app.listen(PORT, () =>
  console.log(`🚀 Server running on http://localhost:${PORT}`)
);
