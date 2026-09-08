pipeline {
    agent any

    parameters {
        choice(
            name: 'SUITE',
            choices: ['smoke', 'sanity', 'negative', 'customization', 'regression', 'e2e'],
            description: 'Which TestNG suite to run. Use smoke for every commit. e2e opens Chrome for Telr.'
        )
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        TELR_HEADLESS = 'true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify tools') {
            steps {
                sh 'java -version'
                sh 'mvn -v'
            }
        }

        stage('API tests') {
            when {
                expression { params.SUITE != 'e2e' }
            }
            steps {
                sh "mvn -B test -P${params.SUITE}"
            }
        }

        stage('E2E with Telr') {
            when {
                expression { params.SUITE == 'e2e' }
            }
            steps {
                script {
                    // Secret text credentials with these IDs. If they are missing,
                    // QrOrderFlowTest still runs and skips the Telr card page.
                    def startedE2e = false
                    try {
                        withCredentials([
                            string(credentialsId: 'TELR_CARD_NUMBER', variable: 'TELR_CARD_NUMBER'),
                            string(credentialsId: 'TELR_CVV', variable: 'TELR_CVV'),
                            string(credentialsId: 'TELR_EXP_MONTH', variable: 'TELR_EXP_MONTH'),
                            string(credentialsId: 'TELR_EXP_YEAR', variable: 'TELR_EXP_YEAR')
                        ]) {
                            startedE2e = true
                            sh 'mvn -B test -Pe2e'
                        }
                    } catch (err) {
                        if (startedE2e) {
                            throw err
                        }
                        echo "Telr credentials not found. Running E2E without card fill."
                        sh 'mvn -B test -Pe2e'
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'reports/extent-report.html', allowEmptyArchive: true
            publishHTML([
                allowMissing         : true,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'reports',
                reportFiles          : 'extent-report.html',
                reportName           : 'Extent Report'
            ])
        }
    }
}
