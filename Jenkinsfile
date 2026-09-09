pipeline {
    agent any

    parameters {
        choice(
            name: 'SUITE',
            choices: ['smoke', 'sanity', 'negative', 'customization', 'regression', 'e2e'],
            description: 'Which TestNG suite to run. Use smoke for every commit. sanity/customization/regression/e2e complete Telr payment for positive checkout tests.'
        )
        choice(
            name: 'ENV',
            choices: ['test', 'staging'],
            description: 'Target server. test = testdeploy channel. staging = sprint205. Telr cards are shared. OMS tokens are per account.'
        )
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        JAVA_HOME = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        TELR_HEADLESS = 'true'
    }

    stages {

        stage('Verify tools') {
            steps {
                script {
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${params.ENV} ${params.SUITE}"
                }
                sh """
                    echo "SUITE=${params.SUITE} ENV=${params.ENV}"
                    echo "JAVA_HOME=\$JAVA_HOME"
                    which java
                    java -version
                    which mvn
                    mvn -v
                """
            }
        }

        stage('API tests') {
            when {
                expression { params.SUITE in ['smoke', 'negative'] }
            }
            steps {
                sh "mvn -B test -P${params.SUITE} -Denv=${params.ENV}"
            }
        }

        stage('Checkout tests with Telr') {
            when {
                expression { params.SUITE in ['sanity', 'customization', 'regression', 'e2e'] }
            }
            steps {
                script {
                    // Same Telr cards for both servers. OMS login is per account.
                    // test  → testdeploychannel  (OMS_*)
                    // staging → sprint205         (OMS_STAGING_*)
                    def oms = params.ENV == 'staging' ? [
                            auth         : 'OMS_STAGING_AUTH_TOKEN',
                            aa           : 'OMS_STAGING_AA_TOKEN',
                            switchToken  : 'OMS_STAGING_SWITCH_TOKEN',
                            switchedUser : 'OMS_STAGING_SWITCHED_USER_TOKEN',
                            tenant       : 'OMS_STAGING_TENANT_TOKEN',
                            esSession    : 'OMS_STAGING_ES_SESSION_ID',
                            counterShift : 'OMS_STAGING_COUNTER_SHIFT_ID'
                    ] : [
                            auth         : 'OMS_AUTH_TOKEN',
                            aa           : 'OMS_AA_TOKEN',
                            switchToken  : 'OMS_SWITCH_TOKEN',
                            switchedUser : 'OMS_SWITCHED_USER_TOKEN',
                            tenant       : 'OMS_TENANT_TOKEN',
                            esSession    : 'OMS_ES_SESSION_ID',
                            counterShift : 'OMS_COUNTER_SHIFT_ID'
                    ]

                    withCredentials([
                            string(credentialsId: 'TELR_CARD_NUMBER', variable: 'TELR_CARD_NUMBER'),
                            string(credentialsId: 'TELR_CVV', variable: 'TELR_CVV'),
                            string(credentialsId: 'TELR_EXP_MONTH', variable: 'TELR_EXP_MONTH'),
                            string(credentialsId: 'TELR_EXP_YEAR', variable: 'TELR_EXP_YEAR'),
                            string(credentialsId: oms.auth, variable: 'OMS_AUTH_TOKEN'),
                            string(credentialsId: oms.aa, variable: 'OMS_AA_TOKEN'),
                            string(credentialsId: oms.switchToken, variable: 'OMS_SWITCH_TOKEN'),
                            string(credentialsId: oms.switchedUser, variable: 'OMS_SWITCHED_USER_TOKEN'),
                            string(credentialsId: oms.tenant, variable: 'OMS_TENANT_TOKEN'),
                            string(credentialsId: oms.esSession, variable: 'OMS_ES_SESSION_ID'),
                            string(credentialsId: oms.counterShift, variable: 'OMS_COUNTER_SHIFT_ID')
                    ]) {
                        sh "mvn -B test -P${params.SUITE} -Denv=${params.ENV}"
                    }
                }
            }
        }
    }

    post {
        always {
            junit(
                allowEmptyResults: true,
                testResults: 'target/surefire-reports/*.xml'
            )

            archiveArtifacts(
                artifacts: 'reports/**',
                allowEmptyArchive: true
            )

            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'reports',
                reportFiles: 'extent-report.html',
                reportName: 'Extent Report'
            ])
        }
    }
}
