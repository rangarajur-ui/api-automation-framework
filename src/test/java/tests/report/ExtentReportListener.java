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
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Path;

/**
 * Writes a self-contained reports/extent-report.html after each suite.
 * Offline mode keeps CSS/JS next to the HTML so Jenkins HTML Publisher can render it.
 */
public class ExtentReportListener implements ITestListener, ISuiteListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    @Override
    public void onStart(ISuite suite) {
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
        if (extent != null) {
            extent.flush();
        }
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
