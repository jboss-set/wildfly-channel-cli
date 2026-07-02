# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

**Wildfly Channel CLI** is a command-line tool for manipulating and generating reports from [Wildfly Channel](https://github.com/wildfly-extras/wildfly-channel) metadata files. It provides utilities for comparing channels, finding upgrades, merging manifests, and creating channel/manifest files.

### Technology Stack

- **Language**: Java 17
- **Build Tool**: Maven 3.6.3+
- **CLI Framework**: Picocli 4.7.5
- **Core Dependencies**:
  - `wildfly-channel` (1.2.2.Final) - Core channel manipulation
  - Maven Resolver (1.9.17) - Artifact resolution
  - JBoss Logging (3.5.3.Final) - Logging facade
  - j2html (1.6.0) - HTML report generation
- **Testing**: JUnit 5 with AssertJ assertions

### Architecture

The application follows a command-based architecture using Picocli:

- **Main Entry Point**: `org.jboss.set.channel.cli.Main`
- **Base Command**: `MainCommand` - Root command that delegates to subcommands
- **Subcommands**: Each command is a separate class (e.g., `FindUpgradesCommand`, `CompareChannelsCommand`)
- **Shared Logic**: `MavenBasedCommand` provides common Maven/repository functionality

Key architectural patterns:
- Command pattern via Picocli annotations
- Maven Resolver integration for artifact resolution
- Listener pattern for upgrade discovery (`UpgradeDiscoveryListener`)
- Report generation using builder pattern (`FormattingReportBuilder`)

## Building and Running

### Build the Project

```bash
# Clean build with tests
mvn clean package

# Skip tests
mvn clean package -DskipTests

# The fat JAR will be created at:
# target/wildfly-channel-cli-*-jar-with-dependencies.jar
```

### Run the CLI

```bash
# Show help
java -jar target/wildfly-channel-cli-*-jar-with-dependencies.jar --help

# Example: Find upgrades
java -jar target/wildfly-channel-cli-*-jar-with-dependencies.jar \
  find-upgrades file:base-channel.yaml \
  --repositories mrrc::https://maven.repository.redhat.com/ga/ \
  --exclude-pattern "[.-]fuse-" \
  --include-pattern "[.-]redhat-"
```

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FindUpgradesCommandTestCase
```

## Available Commands

The CLI provides the following subcommands:

1. **find-upgrades** - Discovers possible upgrades for channel streams by querying Maven repositories
2. **compare-channels** - Compares two channels and highlights version differences
3. **compare-manifests** - Compares two manifest files
4. **create-manifest-from-repo** - Scans a local Maven repository and creates a manifest
5. **create-channel** - Creates a new channel file
6. **update-channel** - Updates an existing channel
7. **merge-manifests** - Merges two manifest files (second overrides first)
8. **extract-repositories** - Extracts repository URLs from a channel
9. **extract-manifest-url** - Extracts manifest URL from a channel
10. **generate-dependency-groups** - Generates dependency groups for manifest building
11. **sort-versions** - Reads version strings from stdin and outputs them sorted using VersionMatcher.COMPARATOR

## Development Conventions

### Code Organization

- **Commands**: Located in `src/main/java/org/jboss/set/channel/cli/`
- **Utilities**: Helper classes in `utils/` package
- **Report Generation**: Classes in `report/` package
- **Manifest Building**: Specialized commands in `manifestbuilder/` package

### Key Implementation Details

1. **Version Comparison**: The `VersionUtils` class provides utilities for parsing and comparing versions, including handling qualifiers (e.g., `.redhat-00001`)

2. **Upgrade Discovery**: `FindUpgradesCommand` uses a listener pattern to collect upgrades and generate multiple outputs:
   - HTML report (`report.html`)
   - Diff manifest (`diff-manifest.yaml`) - only upgraded streams
   - Upgraded manifest (`upgraded-manifest.yaml`) - all streams with upgrades applied

3. **Blocklists**: The tool supports blocklists to exclude specific versions from consideration during upgrade discovery

4. **Repository Configuration**: Repositories can be specified as:
   - Simple URLs: `URL1,URL2,...`
   - With IDs: `ID1::URL1,ID2::URL2,...`

### Testing Practices

- Tests use JUnit 5 (`@Test` annotations)
- AssertJ for fluent assertions
- Test classes follow naming convention: `*TestCase.java`
- Tests focus on core logic (e.g., version comparison algorithms)

### Logging

- Uses JBoss Logging facade
- Configuration in `src/main/resources/logback.xml`
- Logger instances typically named `logger` in command classes

### Maven Assembly

The project uses `maven-assembly-plugin` with a custom descriptor (`src/assembly/jar.xml`) to create a fat JAR with all dependencies. The main class is set to `org.jboss.set.channel.cli.Main`.

## Working with the Codebase

### Adding a New Command

1. Create a new class extending `MavenBasedCommand` (if Maven functionality needed) or implementing `Callable<Integer>`
2. Annotate with `@CommandLine.Command` specifying name and description
3. Add command-specific options using `@CommandLine.Option` or `@CommandLine.Parameters`
4. Implement the `call()` method with command logic
5. Register the command in `Main.java` using `commandLine.addSubcommand()`

### Modifying Version Logic

Version parsing and comparison logic is centralized in `VersionUtils`. When modifying:
- Ensure backward compatibility with existing version formats
- Add tests to `VersionUtilsTestCase`
- Consider impact on upgrade discovery in `FindUpgradesCommand.findPossibleUpgrades()`

### Report Generation

HTML reports are generated using j2html. The `FormattingReportBuilder` provides a fluent API for building reports. When modifying reports:
- Keep the builder pattern consistent
- Ensure reports are readable in browsers
- Consider adding CSS for better formatting
