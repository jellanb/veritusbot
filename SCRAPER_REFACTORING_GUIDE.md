# Scraper Refactoring Documentation

## Overview

The scraper has been refactored from a monolithic `PjudScraper.java` into a modular, testeable architecture following SOLID principles.

## New Architecture Structure

```
service/scraper/
├── ScraperOrchestrator.java      ← Main entry point
├── browser/
│   └── BrowserManager.java        ← Manages Playwright browser
├── form/
│   ├── FormFiller.java            ← Fills search forms
│   └── TribunalSelector.java      ← Selects tribunals from dropdown
├── parser/
│   └── ResultParser.java          ← Parses HTML results
├── phases/
│   ├── Phase.java                 ← Interface for search phases
│   ├── Phase1Scraper.java         ← Search Santiago tribunals (1-30)
│   └── Phase2Scraper.java         ← Search other tribunals (31+)
└── config/
    └── ScraperConfig.java         ← Centralized configuration
```

## Key Components

### 1. ScraperOrchestrator
**Responsibility:** Orchestrates the entire scraping process

```java
public List<ResultDTO> scrapePeople(List<PersonaDTO> people)
```

- Launches browser
- Navigates to PJUD website
- Executes Phase 1 for Santiago tribunals
- Executes Phase 2 for other tribunals
- Manages cleanup and error handling

### 2. BrowserManager
**Responsibility:** Manages Playwright browser lifecycle

Methods:
- `launchBrowser()` - Start Chromium browser
- `navigateTo(Page, String)` - Navigate to URL
- `closeBrowser()` - Cleanup resources
- `isBrowserActive()` - Check browser status

### 3. FormFiller
**Responsibility:** Fills form fields and submits searches

Methods:
- `fillSearchForm(Frame, String, int)` - Fill person name and year
- `fillTribunal(Frame, String)` - Fill tribunal field
- `selectCompetence(Frame, String)` - Select competence dropdown
- `submitForm(Frame)` - Submit search form

### 4. TribunalSelector
**Responsibility:** Manages tribunal dropdown operations

Methods:
- `openDropdown(Frame)` - Open tribunal dropdown menu
- `loadAllTribunals(Frame)` - Load all tribunals from dropdown
- `selectTribunal(Frame, String, int)` - Select specific tribunal by index
- `closeDropdown(Frame)` - Close dropdown menu

### 5. ResultParser
**Responsibility:** Parses HTML and extracts results

Methods:
- `parseResults(String, String, int, String)` - Parse HTML into ResultDTO list
- `hasResults(String)` - Check if results exist
- `extractAllResults(String)` - Extract all result data as arrays

### 6. Phase Interface & Implementations
**Responsibility:** Define search strategy for each phase

```java
public interface Phase {
    List<ResultDTO> execute(Page page, String personName, int startYear, int endYear);
    String getPhaseName();
}
```

**Phase1Scraper:** Searches Santiago tribunals (1º - 30º Juzgado Civil)
**Phase2Scraper:** Searches all other tribunals

### 7. ScraperConfig
**Responsibility:** Centralized configuration constants

Includes:
- Timeouts and delays
- CSS selectors
- URLs and form field identifiers
- Phase definitions

### 8. ResultDTO
**Responsibility:** Structured data representation for results

Fields:
- `personName` - Name of searched person
- `tribunal` - Tribunal where case was found
- `year` - Year of the case
- `resolution` - Case resolution number
- `details` - Additional details

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
Each class has ONE clear responsibility:
- `BrowserManager` - Only manages browser
- `FormFiller` - Only fills forms
- `TribunalSelector` - Only selects tribunals
- `ResultParser` - Only parses results

### Open/Closed Principle (OCP)
Easy to add new phases without modifying existing code:
```java
// New phase implementation just needs to implement Phase interface
public class Phase3Scraper implements Phase { ... }
```

### Liskov Substitution Principle (LSP)
Phase implementations are interchangeable:
```java
Phase phase = new Phase1Scraper(...);
phase.execute(page, name, startYear, endYear);
```

### Interface Segregation Principle (ISP)
Each component has focused interfaces:
- `Phase` - Minimal, focused interface
- `BrowserManager` - Only browser-related methods
- `FormFiller` - Only form-related methods

### Dependency Inversion Principle (DIP)
High-level modules depend on abstractions, not concrete implementations:
```java
@Component
public class ScraperOrchestrator {
    public ScraperOrchestrator(BrowserManager browserManager,
                               Phase1Scraper phase1,
                               Phase2Scraper phase2) { ... }
}
```

## Usage Example

```java
@Autowired
private ScraperOrchestrator orchestrator;

public void startScrap() {
    List<PersonaDTO> people = excelService.readClientFromCSV("personas.csv");
    List<ResultDTO> results = orchestrator.scrapePeople(people);
    
    for (ResultDTO result : results) {
        System.out.println(result.getPersonName() + " - " + result.getTribunal());
    }
}
```

## Testing Benefits

Now each component can be tested independently:

```java
@Test
public void testFormFiller() {
    FormFiller filler = new FormFiller();
    Frame mockFrame = mock(Frame.class);
    
    filler.fillSearchForm(mockFrame, "John Doe", 2024);
    
    verify(mockFrame).fill("input[name='nomNombre']", "John Doe");
}
```

## Migration from Old Code

The original `PjudScraper.java` methods should be distributed to:

1. **Form interactions** → `FormFiller`
2. **Tribunal dropdown** → `TribunalSelector`
3. **Result parsing** → `ResultParser`
4. **Browser management** → `BrowserManager`
5. **Phase logic** → `Phase1Scraper` / `Phase2Scraper`

## Next Steps

1. Move remaining logic from `PjudScraper.java` to respective components
2. Implement actual search logic in `Phase1Scraper.execute()`
3. Implement actual search logic in `Phase2Scraper.execute()`
4. Add unit tests for each component
5. Consider caching strategy for tribunal data
6. Add retry logic with exponential backoff

## Configuration

Modify settings in `ScraperConfig.java`:
- Timeouts (default: 30 seconds)
- Delays between searches (default: 2 seconds)
- CSS selectors
- URL configuration

All timeouts and delays are in milliseconds.

