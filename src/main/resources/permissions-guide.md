### Permissions and Authorization

Permissions in this project are managed through a combination of **Database-backed loading** and **Spring Security authorities**, rather than being stored directly inside the JWT.

#### 1. How Permissions are Defined
Permissions are defined as constants in the `Permission` enum (e.g., `shift:close`, `product:update-pricing`). These are grouped into `Role`s (OPERATOR, SUPERVISOR, ADMIN), but can also be assigned individually to users.

#### 2. JWT vs. Token Validation
The **JWT does not contain the full list of permissions**.
- When a user logs in, the JWT is generated with a `role` claim (e.g., `role: "ADMIN"`).
- On every API request, the `JwtFilter` validates the token and extracts the `username`.
- Crucially, the system then calls `UserService.loadUserByUsername(username)`, which fetches the latest permissions directly from the **database**.

This means if you change a user's permissions in the database, they take effect immediately (or on the next request) without needing to issue a new token.

#### 3. Usage in APIs
Permissions are used in `SecurityConfig.java` to protect specific endpoints. There are two ways they are checked:

*   **By Role**: Using `.hasAnyRole("ADMIN", "SUPERVISOR")`.
*   **By Granular Permission**: Using `.hasAuthority("shift:close")`.

For example, the shift closing endpoint is protected specifically by the permission string:
```java
.requestMatchers(HttpMethod.POST, "/api/shifts/close/**").hasAuthority("shift:close")
```

#### 4. Available APIs
There are endpoints to see available permissions and specific user permissions:
- **List all available system permissions**:
    - **Endpoint**: `GET /api/users/permissions`
    - **Requirement**: Must have `ADMIN` role.

---

### Sample Request and Response for User Permission APIs

Below are the details for the user permission endpoints. All requests require a valid JWT token in the `Authorization` header.

#### 1. Get Effective Permissions by User ID
Returns all permissions assigned to the user via their role and any individual permission overrides.

*   **HTTP Method**: `GET`
*   **URL**: `/api/users/{id}/permissions`
*   **Sample Request**:
    ```http
    GET /api/users/550e8400-e29b-41d4-a716-446655440000/permissions HTTP/1.1
    Host: localhost:8080
    Authorization: Bearer <your_jwt_token>
    ```
*   **Sample Response (200 OK)**:
    ```json
    [
      "shift:close",
      "product:update-pricing",
      "product:add-product"
    ]
    ```

#### 2. Get Effective Permissions by Username
Useful for checking permissions during a session or when the user ID is not readily available.

*   **HTTP Method**: `GET`
*   **URL**: `/api/users/by-username/{username}/permissions`
*   **Sample Request**:
    ```http
    GET /api/users/by-username/jdoe/permissions HTTP/1.1
    Host: localhost:8080
    Authorization: Bearer <your_jwt_token>
    ```
*   **Sample Response (200 OK)**:
    ```json
    [
      "shift:close"
    ]
    ```

---

#### Summary
- **JWT**: Stores only the `role`.
- **Validation**: `JwtFilter` triggers a DB lookup via `UserService` to populate granular permissions into the security context.
- **Enforcement**: Done in `SecurityConfig` via `hasAuthority()` or `hasRole()`.
