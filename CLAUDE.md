# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HTTP-Raider is a Burp Suite extension for analyzing HTTP proxy chains and discovering network infrastructure. It provides advanced HTTP request parsing, proxy chain analysis, and request mutation capabilities for security testing.

## Build System

This project uses Gradle with the Shadow plugin for JAR creation:

- **Build**: `./gradlew build` - Compiles, tests, and assembles the project
- **Create JAR**: `./gradlew shadowJar` - Creates a fat JAR with dependencies included
- **Clean**: `./gradlew clean` - Removes build artifacts
- **Test**: `./gradlew test` - Runs the test suite
- **Quick build**: `./gradlew assemble` - Compiles and assembles without tests

## Architecture

### Core Components

1. **Extension Layer** (`extension/`)
   - `HTTPRaiderExtension` - Main Burp extension entry point
   - `HTTPRaiderContextMenu` - Context menu integration
   - `ToolsManager` - Manages extension tools

2. **MVC Architecture** (`httpraider/`)
   - **Controllers** (`controller/`) - Handle UI events and business logic
     - `ApplicationController` - Main application controller with session management
     - `NetworkController` - Manages proxy networks and connections
     - `HttpParserController` - Controls HTTP parsing logic
     - `ProxyController` - Handles proxy configuration
     - `SessionController` - Manages user sessions
   - **Models** (`model/`) - Data structures and business logic
     - `network/` - Network topology, proxy models, connection models
     - `SessionModel` - Session data persistence
     - `PersistenceManager` - Handles data storage/retrieval
   - **Views** (`view/`) - UI components and panels
     - `panels/` - Main UI panels (Application, Network, Session, etc.)
     - `components/` - Reusable UI components
     - `menuBars/` - Menu bar implementations

3. **HTTP Parser System** (`httpraider/parser/`)
   - `ParserChainRunner` - Core parsing logic for request chains
   - `ParserUtils` - Utility functions for HTTP parsing
   - `ParserResult` - Parser result data structures
   - Advanced parsing features: header folding, line ending handling, JS transformations

4. **Proxy Discovery Engine** (`proxyFinder/`)
   - `engine/` - Core mutation and clustering algorithms
     - `MutationRunner` - Executes request mutations
     - `Clusterer` - Groups similar responses
     - `BoundaryInferer` - Infers proxy boundaries
   - `mutations/` - HTTP request mutation strategies
     - Various mutation classes for different HTTP anomalies
     - `RequestMutationStrategy` - Base mutation interface

### Key Design Patterns

- **MVC Pattern**: Controllers handle user interaction, models manage data, views render UI
- **Strategy Pattern**: Mutation strategies for different HTTP attacks
- **Chain of Responsibility**: Parser chain processes requests through multiple stages
- **Observer Pattern**: UI updates respond to model changes

## Development Guidelines

### Session Management
- Sessions are persisted automatically using `PersistenceManager`
- Each session contains network models, proxy configurations, and parser settings
- Session controllers manage UI state and data synchronization

### HTTP Parsing
- The parser system handles malformed HTTP requests and responses
- Supports custom line endings, header folding, and JavaScript transformations
- Load balancing rules determine request routing through proxy chains

### Network Topology
- `NetworkModel` manages proxy relationships and connections
- `ProxyModel` represents individual proxies with configuration
- `ConnectionModel` defines links between proxies

### UI Components
- Custom UI components extend standard Swing components
- Tabbed interfaces for session and network management
- Highlighting and syntax support for HTTP content

## Dependencies

- **Burp Montoya API** (2025.5) - Burp Suite extension API
- **Mozilla Rhino** (1.7.14) - JavaScript engine for transformations
- **Apache Commons Text** (1.13.1) - Text processing utilities

## Common Development Tasks

### Adding New Mutations
1. Create new mutation class extending `RequestMutationStrategy`
2. Implement mutation logic in `proxyFinder/mutations/`
3. Register mutation in `MutationRunner`

### Extending Parser Functionality
1. Modify `ParserChainRunner` for new parsing rules
2. Update `ParserUtils` for utility functions
3. Extend `HttpParserModel` for new configuration options

### Adding UI Components
1. Create new panel/component in appropriate `view/` subdirectory
2. Follow existing naming conventions and UI patterns
3. Integrate with corresponding controller

## Testing

Run tests with `./gradlew test`. Test files are located in `src/test/java/`.

## JAR Distribution

The extension JAR is built with `./gradlew shadowJar` and can be loaded directly into Burp Suite.