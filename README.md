# LearnHub

LearnHub is a modular-monolith learning platform built with Java 17, Spring Boot 3.5.6, React, Gradle 8.10.2, and H2. Students can browse and attempt learning material, teachers can publish and manage their own content, and administrators can manage users and all published content.

## Features

- Public, newest-first dashboard for posts, documents, images, and quizzes
- Student and teacher registration, login, temporary-password recovery, and password changes
- Role-based access for students, teachers, and the single administrator account
- Editable user profiles with cropped, size-optimized profile pictures
- Teacher publishing for text posts, images, PDF/Word/Excel/TXT documents, and quizzes
- Manual quiz builder and schema-validated JSON quiz import
- Timed multiple-choice quiz attempts, scores, solutions, re-attempts, best score, and unique-student attempt counts
- Teacher-owned content management from **My Profile**
- Administrator user and content management with filtering and pagination
- Persistent local H2 storage and deployment configurations for Render and Netlify

## Project structure

```text
src/main/java/       Spring Boot modular-monolith backend
src/main/resources/  Application configuration and bundled React build
frontend/             React and Bootstrap user interface
resources/quizzes/    Importable quiz samples and JSON schema
gradle/wrapper/       Gradle 8.10.2 wrapper
```

## Requirements

- Java 17
- Node.js 20 or newer with npm
- Windows for the supplied start and stop scripts

No system-wide Gradle installation is required; use the included wrapper.

## Start locally

On Windows, run:

```bat
start.bat
```

The script installs frontend dependencies when necessary, starts the combined application, waits for port `8080`, and opens `http://localhost:8080` in the default browser.

Stop it with:

```bat
stop.bat
```

The local H2 database is stored under `data/`. The application updates its schema automatically and keeps uploaded files in the database.

## Build and test

Run the complete frontend build and backend test suite:

```bat
gradlew.bat test
```

Build the deployable Spring Boot JAR:

```bat
gradlew.bat clean bootJar
```

The Gradle build runs the React production build and copies it into Spring Boot's static resources. To compile only the backend when troubleshooting, add `-PskipFrontend`.

## Run the frontend separately

For frontend development with hot reload:

```bat
cd frontend
npm install
npm run dev
```

The development UI runs on `http://localhost:5173`; keep the Spring Boot API running on `http://localhost:8080`.

## Quiz JSON imports

Ready-to-import Java, Spring Boot, and React quizzes are available in [`resources/quizzes`](./resources/quizzes). The reusable format is documented by [`quiz.schema.json`](./resources/quizzes/schema/quiz.schema.json).

From a teacher account, open **Publish content**, choose **Quiz**, select **Import JSON**, and upload one of the samples. Invalid files are rejected with a schema-related validation message.

## Configuration

The most useful environment variables are:

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | Spring Boot HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/lms;AUTO_SERVER=TRUE` | H2 database location |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | empty | Database password |
| `APP_CORS_ALLOWED_ORIGINS` | local frontend origins | Comma-separated allowed UI origins |
| `VITE_API_BASE_URL` | empty | Public backend URL embedded in a Netlify frontend build |

Do not expose development credentials or unchanged seeded passwords on a public deployment. Replace the initial administrator password before using the platform with real users.

## Deployment

The repository includes:

- [`render.yaml`](./render.yaml) and [`Dockerfile`](./Dockerfile) for the Spring Boot API and persistent H2 disk on Render
- [`netlify.toml`](./netlify.toml) for the React frontend on Netlify
- [`build-deployment.bat`](./build-deployment.bat) for reproducing deployment builds locally

Follow [`DEPLOYMENT.md`](./DEPLOYMENT.md) for the full deployment procedure and persistence checks.

## Notes

- H2 is appropriate for local learning and small single-instance deployments. Use a managed database before scaling to multiple backend instances.
- Uploaded images are normalized by the backend. Profile photos are cropped in the browser and stored below 50 KB.
- Quiz attempt counts represent distinct students, not the total number of re-attempts.
