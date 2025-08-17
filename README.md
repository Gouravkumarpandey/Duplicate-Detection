# Duplicate Detection Project

This repository contains a full-stack application for detecting duplicate files and managing file categorization. The project is divided into two main parts:

## Architecture Diagram

### High-Level System Architecture
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Client Layer (Browser)                           │
│  ┌───────────────────┐  ┌──────────────────┐  ┌───────────────────────────┐ │
│  │ Directory Scanner │  │  Duplicate View  │  │      Log Viewer          │ │
│  │ - File Selection  │  │ - Duplicate List │  │ - System Logs            │ │
│  │ - Scan Progress   │  │ - File Details   │  │ - Error Tracking         │ │
│  │ - Filters        │  │ - Category View  │  │ - Performance Metrics    │ │
│  └───────────────────┘  └──────────────────┘  └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                           HTTP/REST API (Port: 8080)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                       Spring Boot Controllers                          │ │
│  │  ┌─────────────────┐              ┌─────────────────┐                  │ │
│  │  │ FileController  │              │  LogController  │                  │ │
│  │  │ - POST /scan    │              │ - GET /logs     │                  │ │
│  │  │ - GET /files    │              │ - POST /logs    │                  │ │
│  │  │ - GET /duplicates│             │ - DELETE /logs  │                  │ │
│  │  │ - DELETE /file  │              │                 │                  │ │
│  │  └─────────────────┘              └─────────────────┘                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Business Layer                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                           Core Services                                │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │ │
│  │  │   FileService   │  │ LoggingService  │  │  RuleEngineService      │ │ │
│  │  │ - File Scanning │  │ - Event Logging │  │ - Rule Processing       │ │ │
│  │  │ - Hash Computing│  │ - Log Retrieval │  │ - Category Assignment   │ │ │
│  │  │ - Duplicate Det.│  │ - Log Filtering │  │ - Pattern Matching      │ │ │
│  │  │ - File Deletion │  │ - Performance   │  │ - JSON Rule Parsing     │ │ │
│  │  │ - Metadata Ext. │  │   Monitoring    │  │                         │ │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Data Access Layer                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                     Spring Data Repositories                           │ │
│  │  ┌─────────────────┐              ┌─────────────────┐                  │ │
│  │  │ FileRepository  │              │ LogRepository   │                  │ │
│  │  │ - CRUD Ops      │              │ - CRUD Ops      │                  │ │
│  │  │ - Query Methods │              │ - Query Methods │                  │ │
│  │  │ - Aggregation   │              │ - Log Search    │                  │ │
│  │  │ - Indexing      │              │ - Time Queries  │                  │ │
│  │  └─────────────────┘              └─────────────────┘                  │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Data Layer                                      │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          MongoDB Database                              │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │ │
│  │  │ files Collection│  │ logs Collection │  │   Configuration         │ │ │
│  │  │                 │  │                 │  │                         │ │ │
│  │  │ Document Schema:│  │ Document Schema:│  │ Files:                  │ │ │
│  │  │ - _id (ObjectId)│  │ - _id (ObjectId)│  │ - application.properties│ │ │
│  │  │ - fileName      │  │ - timestamp     │  │ - categorization-rules  │ │ │
│  │  │ - filePath      │  │ - level         │  │   .json                 │ │ │
│  │  │ - fileSize      │  │ - message       │  │                         │ │ │
│  │  │ - hashValue     │  │ - component     │  │ Indexes:                │ │ │
│  │  │ - createdDate   │  │ - details       │  │ - hashValue (unique)    │ │ │
│  │  │ - modifiedDate  │  │ - userId        │  │ - fileName (text)       │ │ │
│  │  │ - category      │  │                 │  │ - timestamp (desc)      │ │ │
│  │  │ - isDuplicate   │  │ Indexes:        │  │                         │ │ │
│  │  │ - duplicateOf   │  │ - timestamp     │  │                         │ │ │
│  │  └─────────────────┘  │ - level         │  └─────────────────────────┘ │ │
│  │                       │ - component     │                              │ │
│  │                       └─────────────────┘                              │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Detailed Data Flow & Processing Pipeline

```
File Upload/Scan Request Flow:
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Browser   │───▶│ FileControl │───▶│FileService  │───▶│FileRepositry│
│             │    │ /scan       │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                              │                   │
                                              ▼                   ▼
                                    ┌─────────────┐    ┌─────────────┐
                                    │RuleEngine   │    │  MongoDB    │
                                    │Service      │    │  Database   │
                                    └─────────────┘    └─────────────┘

Processing Steps:
1. Directory Traversal → 2. File Metadata Extraction → 3. Hash Generation
                    ↓
4. Duplicate Detection → 5. Rule Engine Processing → 6. Database Storage
                    ↓
7. Log Generation → 8. Response Formation → 9. Client Update
```

### Technology Stack Details

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | React 18 + TypeScript | UI Components & Type Safety |
| | Vite | Build Tool & Development Server |
| | Tailwind CSS | Styling Framework |
| | Axios/Fetch | HTTP Client |
| **Backend** | Spring Boot 3.x | Application Framework |
| | Spring Web | REST API Development |
| | Spring Data MongoDB | Database Integration |
| | Maven | Build & Dependency Management |
| | Java 17+ | Programming Language |
| **Database** | MongoDB 6.x | Document-based NoSQL Database |
| **Configuration** | application.properties | Spring Boot Configuration |
| | categorization-rules.json | Business Rules Definition |

## Backend
- **Framework:** Java Spring Boot
- **Location:** `Backend/`
- **Features:**
  - REST API for file scanning, duplicate detection, and logging
  - MongoDB integration for data persistence
  - Rule engine for file categorization
  - Configurable CORS and MongoDB settings
- **Main files:**
  - `FileDedupApplication.java`: Main application entry point
  - `controller/`: REST controllers for file and log operations
  - `service/`: Business logic for file scanning, logging, and rule engine
  - `model/`: Data models for files and logs
  - `repository/`: Spring Data repositories
  - `resources/application.properties`: Application configuration
  - `resources/rules/categorization-rules.json`: Categorization rules

## Frontend
- **Framework:** React + TypeScript + Vite
- **Location:** `Frontend/`
- **Features:**
  - UI for scanning directories, viewing duplicates, and logs
  - API integration with backend
  - Tailwind CSS for styling
- **Main files:**
  - `App.tsx`: Main application component
  - `components/`: UI components (DirectoryScanner, DuplicateList, LogViewer, etc.)
  - `api/`: API service and mock API
  - `config/rules.json`: Frontend rules config
  - `types/`: TypeScript types
  - `utils/`: Utility functions


## How to Start the Java Server (Backend)

### Prerequisites
- Java 17 or above
- Maven
- MongoDB (running locally or remotely)

### Steps
1. Open a terminal and navigate to the `Backend/` directory:
  ```powershell
  cd Backend
  ```
2. Build the project using Maven:
  ```powershell
  mvn clean install
  ```
3. Start the Spring Boot server:
  ```powershell
  mvn spring-boot:run
  ```
  Alternatively, you can run the generated JAR file:
  ```powershell
  java -jar target/FileDedupApplication.jar
  ```
4. Make sure MongoDB is running and the connection details are set in `src/main/resources/application.properties`.

### Tools & Technologies Used
- **Java 17+**: Programming language for backend logic
- **Spring Boot**: Framework for building RESTful APIs
- **Maven**: Build and dependency management
- **MongoDB**: NoSQL database for storing file and log data
- **Spring Data MongoDB**: Integration between Spring Boot and MongoDB
- **Spring Web**: For REST API development
- **Spring Boot Test**: For unit and integration testing

---

## Getting Started

### Backend
1. Navigate to the `Backend/` directory.
2. Build and run the Spring Boot application:
  ```powershell
  mvn clean install; mvn spring-boot:run
  ```
3. Ensure MongoDB is running and configured in `application.properties`.

### Frontend
1. Navigate to the `Frontend/` directory.
2. Install dependencies:
   ```powershell
   npm install
   ```
3. Start the development server:
   ```powershell
   npm run dev
   ```

## Project Structure
```
Backend/
  src/main/java/com/yourname/filededup/...
  src/main/resources/...
Frontend/
  src/...
```

## License
This project is licensed under the MIT License.
