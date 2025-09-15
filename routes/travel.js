import express from "express";
import { getTravelIdeas } from "../controllers/travelController.js";

const router = express.Router();

router.post("/", getTravelIdeas);

export default router;
