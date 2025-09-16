import axios from "axios";

export const generateItinerary = async (
  destination,
  days,
  budget,
  interests
) => {
  const response = await axios.post(
    "http://localhost:8000/generate-itinerary",
    {
      destination,
      days,
      budget,
      interests,
    }
  );
  return response.data;
};
