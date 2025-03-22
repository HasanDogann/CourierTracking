# Courier Tracking Application

This is a RESTful web application built with Spring Boot that tracks courier locations and detects when they enter the vicinity of Migros stores.

## Features

- Track courier locations with time, courier ID, latitude, and longitude
- Detect when couriers enter a 100-meter radius of Migros stores
- Prevent duplicate entries for the same store within 1 minute
- Calculate total travel distance for each courier

## Technologies Used

- Java 17
- Spring Boot 2.7.16
- Uber H3 Geospatial Indexing Library
- Lombok
- Jackson for JSON processing

## Design Patterns Used

1. **Singleton Pattern**: Used via Spring's dependency injection for services and components
2. **Observer Pattern**: Implemented through the event-driven approach of processing courier locations
3. **Strategy Pattern**: Used in the distance calculation utilities

## How to Run

### Prerequisites
- Java 17 JDK
- Maven

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/courier-tracking.git
   cd courier-tracking
   ```

2. Build the application:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`.

## API Endpoints

### Register Courier Location
```
POST /api/couriers/location
```
Request body:
```json
{
  "time": "2025-03-22T12:00:00",
  "courierId": "courier123",
  "latitude": 40.9923307,
  "longitude": 29.1244229
}
```

### Get Total Travel Distance
```
GET /api/couriers/{courierId}/distance
```
Response:
```
1234.56 km
```
or
```
456.78 mt
```
(Distance in meters or kilometers, depending on the value)

## Testing

You can use tools like Postman or curl to test the API endpoints:

```bash
# Register a location
curl -X POST http://localhost:8080/api/couriers/location \
  -H "Content-Type: application/json" \
  -d '{"time":"2025-03-22T12:00:00", "courierId":"courier123", "latitude":40.9923307, "longitude":29.1244229}'

# Get total distance
curl http://localhost:8080/api/couriers/courier123/distance
```

## Testing Migros Store Visits

Below are curl commands to simulate a courier visiting each Migros store location and then checking the total travel distance:

```bash
# Ataşehir MMM Migros
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"2025-03-22T10:00:00\", \"courierId\": \"courier123\", \"latitude\": 40.9923307, \"longitude\": 29.1244229}"

# Novada MMM Migros
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"2025-03-22T11:00:00\", \"courierId\": \"courier123\", \"latitude\": 40.986106, \"longitude\": 29.1161293}"

# Beylikdüzü 5M Migros
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"2025-03-22T12:00:00\", \"courierId\": \"courier123\", \"latitude\": 41.0066851, \"longitude\": 28.6552262}"

# Ortaköy MMM Migros
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"2025-03-22T13:00:00\", \"courierId\": \"courier123\", \"latitude\": 41.055783, \"longitude\": 29.0210292}"

# Caddebostan MMM Migros
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"2025-03-22T14:00:00\", \"courierId\": \"courier123\", \"latitude\": 40.9632463, \"longitude\": 29.0630908}"

# Check total distance after visiting all stores
curl -X GET http://localhost:8080/api/couriers/courier123/distance
```
Note: You need to submit at least 2 location points for the same courier to calculate distance.

### Running a Simulation Script

A simple bash script (`courier_simulation.sh`) is provided to simulate a basic courier tracking scenario. Follow these steps to use it:

1.  **Find the script:** Ensure the following content is saved in a file named `courier_simulation.sh` in the root directory of your project:
2.  **Make the script executable:**

   * **macOS:** Open your terminal in the project root directory and run the following command:

       ```bash
       chmod +x courier_simulation.sh
       ```

   * **Windows:**
      * First, ensure that a terminal emulator like Git Bash is installed.
      * Open the terminal in the project root directory and run the following command:

          ```bash
          chmod +x courier_simulation.sh
          ```
        (If the `chmod` command does not work, you can run the script directly from Git Bash.)

3.  **Execute the script:**

   * **macOS:** After the Spring Boot application is running (`mvn spring-boot:run`), execute the script in the terminal using:

       ```bash
       ./courier_simulation.sh
       ```

   * **Windows:** After the Spring Boot application is running, open Git Bash in the project root directory and execute the script using:

       ```bash
       ./courier_simulation.sh
       ```
     (Or you can directly use the command `bash courier_simulation.sh`.)

The script will send two location updates to the application and then query the total distance 
traveled by `courier123`. You will see the output of the simulation in the terminal.
This section in your `readme.md` now provides clear instructions for both macOS and Windows
users on how to run the provided simulation script to test your application.