---
name: gen-javadocs
description: Generate Javadoc documentation for Java code based on the project's documentation and coding conventions.
---

# Generate Javadocs

Generate javadoc documentation for Java code based on the project's documentation and coding conventions.
Explore all java files in the project and generate javadocs for all public methods, following the conventions outlined in `.agents/rules/*`.
The generated javadocs should be concise, informative, and consistent across the codebase. Use the provided examples in `.agents/rules/javadocs.md` as a reference for formatting and content.

## Usage

Javadocs should be generated under docs/javadocs/ with a file structure that mirrors the source code. For example, if there is a service class at `src/main/java/com/example/service/CategoryService.java`, the generated javadoc for that class should be placed at `docs/javadocs/com/example/service/CategoryService.md`. Each method's javadoc should be included in the markdown file for its containing class, organized under the method name as a subheading.
The format should be html, css and images.

## Steps

1. Parse the Java source files to identify all public classes and their public methods. For each method, extract the method signature, parameters, return type, and any existing comments or annotations that can inform the javadoc content.
2. Generate the javadoc content for each method based on the extracted information and the project's documentation conventions.
3. Write the generated javadoc content to the appropriate markdown files under `docs/javadocs/`, maintaining the directory structure that mirrors the source code.
