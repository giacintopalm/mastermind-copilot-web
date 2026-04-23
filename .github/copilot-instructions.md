# GitHub Copilot Instructions - Mastermind Game

## Project Overview
This is a full-stack Mastermind game implementation with:
- **Frontend**: React + Vite + TypeScript
- **Backend**: Java Spring Boot REST API
- **Database**: PostgreSQL with Flyway migrations
- **Deployment**: Azure Static Web Apps (frontend) + Azure App Service (backend)

## Technology Stack

### Frontend
- React 18 with TypeScript
- Vite for build tooling
- CSS for styling (no framework)
- Fetch API for backend communication

### Backend
- Java 17+ with Spring Boot 3.x
- Spring Web for REST APIs
- Spring Data JPA for database access
- Flyway for database migrations
- PostgreSQL database
- Maven for build management

## Coding Conventions

### General Guidelines
- Write clear, self-documenting code with meaningful variable and function names
- Add comments only when the code's intent is not obvious
- Keep functions small and focused on a single responsibility
- Use TypeScript strict mode features (avoid `any` types)

### Frontend (TypeScript/React)
- Use functional components with hooks (no class components)
- Prefer `const` over `let`; avoid `var`
- Use arrow functions for component definitions
- Interface names should start with capital letter (e.g., `GameState`, `Player`)
- Keep React components in separate files
- Use CSS modules or scoped styles to avoid naming conflicts
- Handle errors gracefully and provide user feedback

### Backend (Java/Spring Boot)
- Follow standard Spring Boot project structure:
  - `controller`: REST endpoints
  - `service`: Business logic
  - `repository`: Data access
  - `model`: Entity classes
  - `dto`: Data Transfer Objects
  - `config`: Configuration classes
- Use constructor injection for dependencies (not field injection)
- Annotate REST controllers with `@RestController`
- Use proper HTTP status codes (200, 201, 404, 400, 500, etc.)
- Validate input with `@Valid` and bean validation annotations
- Use `@Transactional` for service methods that modify data
- Follow Java naming conventions (camelCase for methods/variables, PascalCase for classes)

## Architecture Guidelines

### API Design
- RESTful endpoints with clear resource names
- Use plural nouns for collections (e.g., `/api/games`, `/api/players`)
- Return appropriate DTOs, not entity objects directly
- Include CORS configuration for local development and production

### Database Schema
- Use Flyway migrations for all schema changes (in `backend/src/main/resources/db/migration`)
- Migration files follow pattern: `V{version}__{description}.sql`
- Always test migrations on a local database before committing
- Use appropriate indexes for frequently queried columns

### State Management
- Frontend maintains game state locally
- Backend validates all game logic (never trust the client)
- Store game results persistently for leaderboard functionality

## Testing Best Practices
- Write unit tests for business logic in services
- Test REST endpoints with Spring Boot Test
- Mock external dependencies in tests
- Aim for meaningful test coverage, not 100% coverage

## Development Workflow
1. Start backend: `java -jar backend/target/mastermind-backend-1.0.0.jar`
2. Start frontend: `npm run dev` (from root directory)
3. Backend runs on port 8080, frontend on port 5173
4. Use VS Code tasks for common operations

## Common Commands
- **Build backend**: `cd backend && mvn clean package`
- **Run tests**: `mvn test` (in backend directory)
- **Frontend dev**: `npm run dev`
- **Frontend build**: `npm run build`

## Error Handling
- Frontend: Display user-friendly error messages
- Backend: Return appropriate HTTP status codes with meaningful error messages
- Log errors appropriately for debugging

## Security Considerations
- Validate all user input on the backend
- Use parameterized queries (JPA handles this)
- Configure CORS properly for production
- Never expose sensitive information in error messages

## Performance Tips
- Minimize database queries (use appropriate fetch strategies)
- Cache static assets in production
- Use connection pooling for database connections
- Optimize React re-renders with proper dependency arrays

## When Making Changes
- Update this file if project structure or conventions change
- Document any new architectural decisions
- Update README.md if user-facing setup changes
- Ensure tests pass before committing
