# HTML to Text Scraper
A lightweight Java utility designed to fetch web pages, strip away the "noise" (like ads, scripts, and navigation), and convert the core content into markdown files suited for RAG applications.

## Features
Multi-URL Support: Batch process multiple links into a single output file.

Structural Parsing: Maintains hierarchy by preserving headings (h1-h5), paragraphs, code blocks (pre), and even converting HTML tables into readable text grids.

Decoupled Logic: The scraping engine is isolated from the CLI logic, making it easy to integrate into larger Java projects.

## Prerequisites
Java 11 or higher

Maven (for building and dependency management)

## Installation & Building
To compile the project and package it into an executable JAR run:

mvn -q clean package

This will generate a JAR containing all dependencies

## Usage
Run the application using the following command:

Bash

java -jar target/html-to-text-1.0.0.jar

## Workflow:
Enter URLs: The app will prompt you for a link. You can keep adding links until you choose to stop.

Define Output: Provide a name for your file (e.g., research_notes). The app automatically ensures it has a .md extension.

Extraction: The tool fetches the HTML, cleans it, and saves the formatted text to your local directory.

## License
This project is open-source and free to use.
