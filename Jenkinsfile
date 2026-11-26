// Pipeline multibranch de CommandBuilder.
//
// Seuls master et les pull requests sont construits ; les autres branches sont
// ignorées sans réserver d'agent (build « Not built »).
// - Compilation, tests et qualité : sur master et sur les pull requests.
// - Déploiement : sur master seulement. Le jar est publié avec la version du pom
//   (ex. 3.0-SNAPSHOT) dans le dépôt Maven ARTIFACTS_REPOSITORY_URL, par défaut
//   /srv/artifacts/maven sur la machine Jenkins, où TesseractLib vient le chercher.
//
// La qualité passe par SpotBugs, PMD et CPD (copier-coller). Leurs rapports sont
// affichés par le plugin Jenkins « Warnings Next Generation », sans bloquer le build.

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    parameters {
        string(name: 'ARTIFACTS_REPOSITORY_URL', defaultValue: 'file:///srv/artifacts/maven',
               description: 'Dépôt Maven où publier le jar de master.')
    }

    environment {
        SPOTBUGS_PLUGIN = 'com.github.spotbugs:spotbugs-maven-plugin:4.9.8.1'
        PMD_PLUGIN = 'org.apache.maven.plugins:maven-pmd-plugin:3.28.0'
    }

    stages {
        stage('Master ou MR') {
            when {
                beforeAgent true
                anyOf {
                    branch 'master'
                    changeRequest()
                }
            }
            agent any
            stages {
                stage('Compilation') {
                    steps {
                        runMaven('clean test-compile')
                    }
                }

                stage('Tests') {
                    steps {
                        runMaven('test')
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Qualité') {
                    steps {
                        runMaven("verify -DskipTests ${env.SPOTBUGS_PLUGIN}:spotbugs ${env.PMD_PLUGIN}:pmd ${env.PMD_PLUGIN}:cpd")
                    }
                    post {
                        always {
                            recordIssues enabledForFailure: true,
                                         tools: [spotBugs(pattern: 'target/spotbugsXml.xml'),
                                                 pmdParser(pattern: 'target/pmd.xml'),
                                                 cpd(pattern: 'target/cpd.xml')]
                        }
                    }
                }

                stage('Déploiement') {
                    when { branch 'master' }
                    steps {
                        runMaven("deploy -DskipTests -DaltDeploymentRepository=tesseract-artifacts::${params.ARTIFACTS_REPOSITORY_URL}")
                    }
                    post {
                        success {
                            archiveArtifacts artifacts: 'target/commandbuilder-*.jar', fingerprint: true
                        }
                    }
                }
            }
        }
    }
}

// Lance Maven : le wrapper sous Unix, le Maven du PATH sous Windows (pas de mvnw.cmd).
def runMaven(String arguments) {
    if (isUnix()) {
        sh "./mvnw -B ${arguments}"
    } else {
        bat "mvn -B ${arguments}"
    }
}
