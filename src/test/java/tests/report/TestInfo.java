package tests.report;

/**
 * Business-facing labels for a TestNG method. Used by console and Extent output.
 */
public record TestInfo(String title, String purpose, String flow, String passResult) {
}
