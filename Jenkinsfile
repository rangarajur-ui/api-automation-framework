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
                // Telr cards are Jenkins Secret text. OMS tokens stay on this machine
                // at $JENKINS_HOME/oms/test.properties and staging.properties — not in Git.
                withCredentials([
                        string(credentialsId: 'TELR_CARD_NUMBER', variable: 'TELR_CARD_NUMBER'),
                        string(credentialsId: 'TELR_CVV', variable: 'TELR_CVV'),
                        string(credentialsId: 'TELR_EXP_MONTH', variable: 'TELR_EXP_MONTH'),
                        string(credentialsId: 'TELR_EXP_YEAR', variable: 'TELR_EXP_YEAR')
                ]) {
                    sh """
                        set -e
                        OMS_SRC="\$JENKINS_HOME/oms/${params.ENV}.properties"
                        if [ ! -f "\$OMS_SRC" ]; then
                            echo "Missing OMS file: \$OMS_SRC"
                            echo "Add testdeploy tokens to \$JENKINS_HOME/oms/test.properties"
                            echo "Add sprint205 tokens to \$JENKINS_HOME/oms/staging.properties"
                            exit 1
                        fi
                        if [ "${params.ENV}" = "staging" ]; then
                            cp "\$OMS_SRC" src/test/resources/oms.staging.local.properties
                        else
                            cp "\$OMS_SRC" src/test/resources/oms.local.properties
                        fi
                        mvn -B test -P${params.SUITE} -Denv=${params.ENV}
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f src/test/resources/oms.local.properties src/test/resources/oms.staging.local.properties'
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
