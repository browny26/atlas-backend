import axios from "axios";

export const generateItinerary = async (city, days, interests) => {
  const response = await axios.post("http://localhost:8000/generate", {
    start,
    destination,
    days,
    budget,
  });
  return response.data.itinerary;
};
