# Atlas Backend

Backend for the Atlas project.

## Developer
Luisa Cerin Ogbeiwi

## Overview
Atlas Backend is a Spring Boot application that provides RESTful APIs for itinerary management, user authentication, and integration with external services such as Amadeus and Supabase.

## Features
- User registration and authentication (JWT, Google OAuth)
- Itinerary creation, update, and retrieval
- AI-powered itinerary suggestions
- Integration with Amadeus for travel data
- File storage via Supabase
- Password reset functionality
- CORS configuration

## Technologies
- Java 21+
- Spring Boot
- Maven
- Docker

## Getting Started

### Prerequisites
- Java 21 or higher
- Maven
- Docker (optional, for containerization)

### Build and Run
1. Clone the repository:
   ```bash
   git clone https://github.com/browny26/atlas-backend
   cd atlas-backend
   ```
2. Build the project:
   ```bash
   ./mvnw clean install
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Docker
To build and run the application with Docker:
```bash
docker build -t atlas-backend .
docker run -p 8080:8080 atlas-backend
```

## Configuration
Application properties are managed in `src/main/resources/application.properties`. Update this file with your environment-specific settings (e.g., database, JWT secret, external API keys).

## API Endpoints
- `/api/auth` - Authentication endpoints
- `/api/user` - User management
- `/api/itinerary` - Itinerary operations
- `/api/amadeus` - Amadeus integration
- `/api/storage` - File upload/download

## Testing
Run tests with:
```bash
./mvnw test
```

## License
This project is licensed under the MIT License.

---

For any questions, contact Luisa Cerin.