# Smart Study Planner

A Java application that generates personalized study plans for students using Google's Gemini API. The application takes user inputs like subjects, topics, and exam dates to create daily study schedules with balanced distribution of study topics.

## Features

- Create a personalized study plan based on your subjects, topics, and exam date
- Choose between balanced and intensive study strategies
- View study plans with daily breakdowns of subjects, topics, and recommended hours
- JavaFX user interface for ease of use

## Design Patterns Implemented

1. **Singleton Pattern**: Used for the `GeminiClientSingleton` class to ensure a single instance of the Gemini API client.
2. **Factory Pattern**: Implemented in `StudyPlanStrategyFactory` to create different study plan generation strategies.
3. **Strategy Pattern**: Used with `StudyPlanStrategy` interface and concrete implementations like `BalancedStudyPlanStrategy` and `IntensiveStudyPlanStrategy`.
4. **Observer Pattern**: Implemented with `StudyPlanObserver` interface and the observable `StudyPlanGenerator` class to notify UI components about study plan generation events.

## MVC Architecture

The application follows the Model-View-Controller (MVC) architecture:

- **Model**: Data structures like `User`, `Subject`, `StudyPlan`, and `DailyStudyItem`.
- **View**: JavaFX UI components in the `view` package.
- **Controller**: Logic for processing inputs and API interaction in the `controller` package.

## Prerequisites

- Java 11 or higher
- Maven

## Setup

1. Clone the repository:

```bash
git clone https://github.com/yourusername/smart-study-planner.git
cd smart-study-planner
```

2. Update the API key in `GeminiClient.java` with your own Gemini API key if needed.

3. Build the project with Maven:

```bash
mvn clean package
```

## Running the Application

Run the application using Maven:

```bash
mvn javafx:run
```

Or, after building:

```bash
java -jar target/gemini_client-1.0-SNAPSHOT.jar
```

## How to Use

1. Enter your name and select your exam date.
2. Add subjects and their corresponding topics.
3. Choose a study plan strategy (Balanced or Intensive).
4. Click "Generate Study Plan" to create your personalized study plan.
5. View your study plan in the "Study Plan" tab.

## Screenshots

(Add screenshots here)

## Notes for Developers

- The Gemini API client is implemented in `GeminiClient.java`.
- Study plan generation strategies are located in the `service/strategy` package.
- UI components are in the `view` package.
- The main application entry point is `SmartStudyPlannerApp.java`.

## License

MIT 