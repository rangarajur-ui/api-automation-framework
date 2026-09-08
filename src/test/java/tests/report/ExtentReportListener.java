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
 * Writes reports/extent-report.html after each suite.
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
        String testName = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        if (params != null && params.length > 0 && params[0] != null) {
            testName = testName + " [" + params[0] + "]";
        }
        ExtentTest test = extent.createTest(testName);
        test.assignAuthor(ConfigReader.getReportTesterName());
        test.assignDevice(ConfigReader.getReportEnvironment());
        String className = result.getTestClass().getName();
        test.assignCategory(className.substring(className.lastIndexOf('.') + 1));
        for (String group : result.getMethod().getGroups()) {
            test.assignCategory(group);
        }
        CURRENT.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        CURRENT.get().log(Status.PASS, "Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        CURRENT.get().log(Status.FAIL, result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        CURRENT.get().log(Status.SKIP, "Skipped");
    }
}
