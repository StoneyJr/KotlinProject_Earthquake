# EarthquakeProjectKotlin

## Overview
This Student Project is a simple desktop application developed using Kotlin and TornadoFX, designed to display earthquake data fetched in real-time from the USGS Earthquake API. Users can view earthquake events' magnitude, location, time (UTC), and type. The app allows searches by date, including the option to quickly search for today's earthquakes.

## Features
- Fetch and display real-time earthquake data.
- Search for earthquakes by specific dates.
- Display earthquake events' details, including magnitude, location, time, and type.
- Continuous update mechanism to refresh earthquake data periodically.
- User-friendly interface with date picker and search functionalities.

## Requirements
- JavaFX for UI components.
- Kotlin serialization for JSON processing.
- TornadoFX as the Kotlin framework for JavaFX.

## Getting Started
To run EarthquakeApp, you need to have Java and Kotlin set up on your machine.

1. Clone the repository to your local machine.
2. Ensure JavaFX is configured in your development environment.
3. Build the application using your IDE or command line tools.
4. Run the `main` function in `EarthquakeApp.kt` to start the application.

## Usage
Upon launching the app, you'll be presented with a simple UI where you can select a start date for your earthquake search or click "Search today" to fetch earthquake events for the current day. The earthquake data will be displayed in a table format, and the application will automatically update this data every few seconds.
