#!/bin/bash
echo "Starting courier tracking simulation..."

# Set fixed date for March 22, 2025
CURRENT_TIME="2025-03-22T10:00:00"

# Send courier to Ataşehir MMM Migros
echo "Sending courier to Ataşehir MMM Migros at $CURRENT_TIME"
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"$CURRENT_TIME\", \"courierId\": \"courier123\", \"latitude\": 40.9923307, \"longitude\": 29.1244229}"
echo ""

# Wait 5 seconds to simulate 1 hour
echo "Waiting for 1 hour (simulated as 5 seconds in this script)..."
sleep 5

# Set fixed date for 1 hour later
NEXT_TIME="2025-03-22T11:00:00"

# Send courier to Ortaköy MMM Migros
echo "Sending courier to Ortaköy MMM Migros at $NEXT_TIME"
curl -X POST http://localhost:8080/api/couriers/location -H "Content-Type: application/json" -d "{\"time\": \"$NEXT_TIME\", \"courierId\": \"courier123\", \"latitude\": 41.055783, \"longitude\": 29.0210292}"
echo ""

# Wait a moment for processing
echo "Waiting for server to process locations..."
sleep 2

# Check total distance
echo "Checking total distance traveled by courier:"
curl -X GET http://localhost:8080/api/couriers/courier123/distance
echo ""

echo "Simulation complete!"