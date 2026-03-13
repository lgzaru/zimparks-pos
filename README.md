# ZimParks POS Backend

ZimParks POS (Point of Sale) Backend is a Spring Boot application designed to manage transactions, users, products, and reporting for ZimParks operations. It provides a secure REST API with JWT authentication and integrates with a PostgreSQL database.

## 🚀 Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 4.0.3
- **Security:** Spring Security with JWT (JSON Web Tokens)
- **Database:** PostgreSQL (Primary), H2 (Testing)
- **ORM:** Spring Data JPA / Hibernate
- **API Documentation:** SpringDoc OpenAPI (Swagger UI)
- **Build Tool:** Maven

## 📋 Requirements

Before running the application, ensure you have the following installed:
- **JDK 17** or higher
- **Maven 3.6+**
- **PostgreSQL 14+**

## 🛠️ Setup & Installation

### 1. Database Configuration
Create a PostgreSQL database named `zimparks_pos`:
```bash
psql -U postgres -c "CREATE DATABASE zimparks_pos;"
```

### 2. Configure Environment
Update `src/main/resources/application.properties` if your PostgreSQL credentials differ from the defaults:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Initialize Data (Optional)
The application is configured to automatically update the schema (`ddl-auto=update`). To seed initial data (users, roles, etc.), you can run the provided `data.sql` script:
```bash
psql -U postgres -d zimparks_pos -f src/main/resources/data.sql
```

## 🏃 Running the Application

To start the application using the Maven wrapper:
```bash
./mvnw spring-boot:run
```
The server will start on `http://localhost:8080`.

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation at:
- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 📜 Available Scripts

| Command | Description |
|---------|-------------|
| `./mvnw clean install` | Build the project and install dependencies. |
| `./mvnw spring-boot:run` | Run the application. |
| `./mvnw test` | Run all unit and integration tests. |
| `./mvnw package` | Package the application into a JAR file. |

## 📁 Project Structure

```text
src/main/java/com/tenten/zimparks/
├── auth/           # Authentication and JWT logic
├── config/         # Security, OpenAPI, and app configurations
├── creditnote/     # Credit note management
├── customer/       # Customer management
├── product/        # Product and inventory management
├── report/         # Reporting services
├── shift/          # Shift management (open/close)
├── station/        # Park stations management
├── transaction/    # Sales and transaction processing
├── user/           # User and role management
└── vat/            # VAT settings and configuration
```

## 🔐 Authentication

Most API endpoints are secured and require a Bearer Token.
1. **Login:** Send a `POST` to `/api/auth/login` with `username` and `password`.
2. **Usage:** Include the returned JWT in the `Authorization` header: `Authorization: Bearer <your_token>`.

## 🧪 Testing

To run the test suite:
```bash
./mvnw test
```
Testing uses an H2 in-memory database by default (configured in `src/test/resources/application.properties`).

## 📄 License

TODO: Add license information.
(This project currently has an empty license entry in `pom.xml`).
