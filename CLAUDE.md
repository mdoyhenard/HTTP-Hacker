# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
HTTP Hacker (formerly HTTP Raider) is a Burp Suite extension for advanced HTTP connection management. It provides direct control over persistent TCP/TLS connections with HTTP servers and proxies, offering complete visibility of HTTP messages without automatic parsing.

## Build Commands
```bash
# Build the extension JAR (creates a fat JAR with all dependencies)
./gradlew shadowJar

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "ClassName"

# Clean and rebuild
./gradlew clean build

# Build without tests
./gradlew assemble
```

## Architecture

### Package Structure
- `extension/` - Burp Suite extension entry points
  - `HTTPRaiderExtension.java` - Main BurpExtension implementation, initializes the ApplicationController
  - `HTTPRaiderContextMenu.java` - Context menu integration for sending requests to HTTP Hacker
  - `ToolsManager.java` - Manages extension tools and UI components

- `httpraider/` - Core application logic
  - `controller/` - Application control flow and business logic
    - `ApplicationController.java` - Main controller, manages sessions and UI initialization
    - `engines/` - JavaScript engine for template processing
  - `model/` - Data models for connections, sessions, and HTTP messages
  - `view/` - UI components for sessions, streams, and network visualization
  - `parser/` - HTTP parsing logic with parser chain architecture
  - `utils/` - Utility classes including proxy export functionality

- `proxyFinder/` - Proxy discovery and management functionality

### Key Architectural Patterns
1. **MVC Architecture**: Clear separation between model (data), view (UI), and controller (logic)
2. **Session-based organization**: Each session represents a network target with multiple stream tabs
3. **Parser Chain**: Modular HTTP parsing with chainable parser components
4. **Template Engine**: JavaScript-based template processing using Rhino engine for dynamic request manipulation

### Burp Suite Integration
- Uses Montoya API v2025.5 for Burp Suite integration
- Implements BurpExtension interface with AI_FEATURES enhanced capability
- Context menu integration for sending requests from Burp to HTTP Hacker sessions

## Dependencies
- Montoya API 2025.5 - Burp Suite extension API
- Rhino 1.7.14 - JavaScript engine for template processing
- Apache Commons Text 1.13.1 - Text processing utilities
- Gson 2.11.0 - JSON parsing
- JUnit Jupiter 5.10.0 - Testing framework

## Testing
Tests are organized by component with comprehensive coverage for:
- Parser edge cases and real-world scenarios
- JavaScript engine floating-point handling
- Request parsing with various line endings
- Header manipulation and folding

Run tests before committing changes to ensure parser integrity and engine stability.