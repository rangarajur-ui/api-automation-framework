package tests.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import config.ConfigReader;
import config.TestDataReader;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import tests.support.CheckoutStats;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a self-contained reports/extent-report.html after each suite.
 * Offline mode keeps CSS/JS next to the HTML so Jenkins HTML Publisher can render it.
 */
public class ExtentReportListener implements ITestListener, ISuiteListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    @Override
    public void onStart(ISuite suite) {
        CheckoutStats.reset();
        Path report = Path.of("reports", "extent-report.html");
        ExtentSparkReporter spark = new ExtentSparkReporter(report.toString());
        spark.config().setDocumentTitle("QR API Automation");
        spark.config().setReportName("Paytm QR API — " + suite.getName());
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setOfflineMode(true);
        spark.config().setEncoding("utf-8");
        spark.config().setCss(reportCss());
        spark.config().setTimeStampFormat("dd MMM yyyy, HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Tester Name", ConfigReader.getReportTesterName());
        extent.setSystemInfo("Environment", ConfigReader.getReportEnvironment());
        extent.setSystemInfo("Active env", ConfigReader.getActiveEnvironment());
        extent.setSystemInfo("Base URL", ConfigReader.getBaseUrl());
        extent.setSystemInfo("Frontend build", ConfigReader.getFrontendBuildVersion());
        extent.setSystemInfo("QR code", TestDataReader.getQrCode());
        extent.setSystemInfo("Account", TestDataReader.getExpectedAccountName());
        extent.setSystemInfo("Suite", suite.getName());
        extent.setSystemInfo("Java", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
    }

    @Override
    public void onFinish(ISuite suite) {
        printExecutionSummary(suite);
        if (extent != null) {
            extent.flush();
        }
    }

    private static void printExecutionSummary(ISuite suite) {
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        List<String> failedTests = new ArrayList<>();
        for (ISuiteResult suiteResult : suite.getResults().values()) {
            ITestContext context = suiteResult.getTestContext();
            passed += context.getPassedTests().size();
            failed += context.getFailedTests().size();
            skipped += context.getSkippedTests().size();
            for (ITestResult result : context.getFailedTests().getAllResults()) {
                String reason = result.getThrowable() == null
                        ? "failed"
                        : firstLine(result.getThrowable().getMessage());
                failedTests.add(result.getMethod().getMethodName() + " : " + reason);
            }
        }
        int total = passed + failed + skipped;

        System.out.println("==================================================");
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("==================================================");
        System.out.println();
        System.out.println("Total Tests       : " + total);
        System.out.println("Passed            : " + passed);
        System.out.println("Failed            : " + failed);
        System.out.println("Skipped           : " + skipped);
        System.out.println();
        System.out.println("Orders Created    : " + CheckoutStats.ordersCreated());
        System.out.println("Payments Success  : " + CheckoutStats.paymentsSuccess());
        System.out.println("Payments Failed   : " + CheckoutStats.paymentsFailed());
        System.out.println();
        System.out.println("Business Flows:");
        System.out.println("Payment Completion      : " + CheckoutStats.paymentsSuccess());
        System.out.println("Order Creation          : " + CheckoutStats.ordersCreated());
        System.out.println("Customization Orders    : " + CheckoutStats.customizationOrders());
        System.out.println("Instruction Orders      : " + CheckoutStats.instructionOrders());
        System.out.println("Cooking Detail Orders   : " + CheckoutStats.cookingOrders());
        if (!failedTests.isEmpty()) {
            System.out.println();
            System.out.println("FAILED TESTS");
            for (String line : failedTests) {
                System.out.println("- " + line);
            }
        }
        System.out.println();
        System.out.println("==================================================");
    }

    private static String firstLine(String message) {
        if (message == null || message.isBlank()) {
            return "failed";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    @Override
    public void onTestStart(ITestResult result) {
        TestInfo info = TestCatalog.info(result);
        ExtentTest test = extent.createTest(info.title(), info.purpose());
        test.assignAuthor(ConfigReader.getReportTesterName());
        test.assignDevice(ConfigReader.getReportEnvironment());
        test.assignCategory(info.flow());

        String className = result.getTestClass().getName();
        test.assignCategory(className.substring(className.lastIndexOf('.') + 1));
        for (String group : result.getMethod().getGroups()) {
            test.assignCategory(group);
        }

        CURRENT.set(test);
        TestReporter.attach(test, info);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TestReporter.finish(result);
        CURRENT.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        TestReporter.recordListenerFailure(result);
        TestReporter.finish(result);
        CURRENT.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = CURRENT.get();
        if (test != null) {
            String reason = result.getThrowable() == null
                    ? "Skipped"
                    : result.getThrowable().getMessage();
            test.log(Status.SKIP, reason == null ? "Skipped" : reason);
        }
        System.out.println("Result           : Skipped");
        TestReporter.finish(result);
        CURRENT.remove();
    }

    private static String reportCss() {
        return """
                .card-header { background: #1f4e79 !important; }
                .badge-primary { background: #1f4e79 !important; }
                table { font-size: 13px; }
                """;
    }
}
