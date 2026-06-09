# Java Testing Project

Minimal Maven Java project configured with JUnit 5.

## Requirements

- Java 17+
- Maven 3.9+

## Run tests

```bash
mvn test
```

## Project structure

- `src/main/java/com/example/App.java`: sample class under test
- `src/test/java/com/example/AppTest.java`: JUnit 5 tests

## Swisstopo Address Search Integration

This project includes a Swisstopo address autocomplete client:

- `src/main/java/com/example/SwisstopoAddressClient.java`
- `src/main/java/com/example/AddressSuggestion.java`

It calls the SearchServer endpoint described in the docs:

- https://docs.geo.admin.ch/access-data/search.html
- API endpoint used: `https://api3.geo.admin.ch/rest/services/api/SearchServer`

Request parameters for autocomplete:

- `type=locations`
- `origins=address`
- `searchText=<user-input>`
- `limit=<max-results>`

### egaid extraction

For each address suggestion, the client extracts `egaid` by:

1. Prefering the link with title `ch.swisstopo.amtliches-gebaeudeadressverzeichnis`
2. Reading the final numeric path segment as `egaid`
3. Falling back to numeric part of `attrs.featureId` when required

### Example usage

```java
SwisstopoAddressClient client = new SwisstopoAddressClient();
var suggestions = client.autocomplete("Bern Bahnhofstrasse 1", 10);

for (AddressSuggestion s : suggestions) {
	System.out.println(s.label() + " egaid=" + s.egaid());
}
```