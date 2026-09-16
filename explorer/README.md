# QR Automation Explorer

Interactive map of **this** TestNG / Rest Assured / Selenium framework. Counts, paths, and traces come from the Java sources in this repository. It is not a generic demo.

## How to run

From the repository root:

```bash
python3 tools/analyze-framework.py
cd explorer
python3 -m http.server 8765
```

Open http://localhost:8765

`file://` will not load `model.json` in most browsers. Use the local server.

This folder is separate from Maven. Existing tests are unchanged.

## What you can do

- **Business / Developer / Architect** change how much detail is shown.
- **Test Explorer** lists tests by API vs checkout (Telr + OMS) and by TestNG group.
- Click a test, then **Trace this test** or **Play** to step the static story path.
- Search with Command-K or `/` for class, method, test, endpoint, or locator.
- **Dependencies** shows import edges between types in this repo.
- **Insights** lists heuristics only (large files, Thread.sleep, coupling). Not confirmed defects.
- Export the current graph as JSON or Mermaid.

## How the repository is analyzed

`tools/analyze-framework.py` walks `src/main/java` and `src/test/java`, plus TestNG XML and committed property files.

It records packages, files, classes, methods, imports, extends/implements, TestNG `@Test` groups and `@DataProvider`, Rest Assured `.post("/…")` / `.get("/…")` paths, Selenium `By.*` locators, and calls of the form `OtherClass.method(`.

Relationships are tagged `HIGH` / `MEDIUM` / `LOW`. The UI labels uncertain links.

It does **not** use `javac`. Method bodies are scanned for about 80 lines, so later calls in a long method can be missed.

## Known vs inferred

| Shown as | Meaning |
|---|---|
| Detected from usage | Annotation, import, HTTP string, or a call whose target exists in this repo |
| Inferred from naming | Package or class name only |
| Story path | Same as the full walk, with TestDataReader getters and TestReporter calls hidden so a QA lead can read the guest story |
| Potential issue | Heuristic (large file, many inbound links, Thread.sleep). Not a confirmed defect |

No JaCoCo (or other) coverage file is in this repo. The UI says so and shows static test links instead.

TestNG lifecycle in this tree is only `@Test` and `@DataProvider`. There is no `@BeforeMethod` and no `IRetryAnalyzer`. Browser setup lives in `TelrHostedPage`.

## Secrets

Property values that look like tokens, cards, passwords, or mobiles are stored masked (`********` or last four digits). Do not paste live OMS or Telr values into the explorer.

## Refresh after code changes

```bash
python3 tools/analyze-framework.py
```

Reload the browser.

## Another language later

Keep the same `model.json` shape (`classes`, `tests`, `relationships`, `storyTraces`, `traces`). Point a new parser at the other tree and write the same keys. The UI does not parse Java itself.
