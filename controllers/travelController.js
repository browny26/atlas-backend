import { generateItinerary } from "../services/aiService.js";

export const getTravelIdeas = async (req, res) => {
  try {
    const { start, destination, days, budget } = req.body;
    const itinerary = await generateItinerary(start, destination, days, budget);
    res.json({ success: true, itinerary });
  } catch (error) {
    console.error(error);
    res
      .status(500)
      .json({ success: false, error: "Error generating itinerary" });
  }
};
