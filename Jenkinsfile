// Pipeline multibranch de CommandBuilder.
//
// Seuls master et les pull requests sont construits ; les autres branches sont
// ignorées sans réserver d'agent (build « Not built »).
// - Compilation et tests (avec couverture JaCoCo) : sur master et sur les pull requests.
// - Qualité : analyse SonarQube, sur master seulement (l'édition Community de
//   SonarQube n'analyse qu'une branche par projet, une pull request écraserait master).
// - Déploiement : sur master seulement. Le jar est publié avec la version du pom
//   (ex. 3.0-SNAPSHOT) dans le dépôt Maven DEPLOY_REPOSITORY_URL, par défaut
//   /srv/artifacts/maven sur la machine Jenkins, servi par https://maven.tesseract.onl/.
//
// Prérequis Jenkins :
// - un JDK 25 sur l'agent ;
// - Maven : l'installation Jenkins nommée MAVEN_TOOL (Administrer Jenkins › Tools),
//   ou à défaut le mvn du PATH de l'agent ;
// - le plugin « SonarQube Scanner » avec un serveur SonarQube déclaré sous le nom
//   SONAR_SERVER (Administrer Jenkins › System).

pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
        timestamps()
    }

    parameters {
        string(name: 'DEPLOY_REPOSITORY_URL', defaultValue: 'file:///srv/artifacts/maven',
               description: 'Dépôt Maven où publier le jar de master.')
        string(name: 'MAVEN_TOOL', defaultValue: '',
               description: 'Nom de l'installation Maven déclarée dans Jenkins. Vide : mvn du PATH de l'agent.')
        string(name: 'SONAR_SERVER', defaultValue: 'SonarQube',
               description: 'Nom du serveur SonarQube déclaré dans Jenkins.')
        booleanParam(name: 'WAIT_QUALITY_GATE', defaultValue: false,
                     description: 'Attendre le Quality Gate et bloquer le déploiement s\'il échoue (webhook SonarQube vers Jenkins requis).')
    }

    environment {
        JACOCO_PLUGIN = 'org.jacoco:jacoco-maven-plugin:0.8.14'
        SONAR_PLUGIN = 'org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988'
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
                        runMaven("${env.JACOCO_PLUGIN}:prepare-agent test ${env.JACOCO_PLUGIN}:report")
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Qualité') {
                    when { branch 'master' }
                    steps {
                        withSonarQubeEnv(params.SONAR_SERVER) {
                            runMaven("${env.SONAR_PLUGIN}:sonar -Dsonar.projectName=CommandBuilder " +
                                     '-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml')
                        }
                        script {
                            if (params.WAIT_QUALITY_GATE) {
                                timeout(time: 10, unit: 'MINUTES') {
                                    waitForQualityGate abortPipeline: true
                                }
                            }
                        }
                    }
                }

                stage('Déploiement') {
                    when { branch 'master' }
                    steps {
                        runMaven("deploy -DskipTests -DaltDeploymentRepository=tesseract-artifacts::${params.DEPLOY_REPOSITORY_URL}")
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

// Lance le Maven de Jenkins : l'installation MAVEN_TOOL si elle est renseignée,
// sinon le mvn du PATH de l'agent.
def runMaven(String arguments) {
    def mavenTool = params.MAVEN_TOOL?.trim()
    def pathEntries = mavenTool ? ["PATH+MAVEN=${tool(mavenTool)}/bin"] : []
    withEnv(pathEntries) {
        if (isUnix()) {
            sh "mvn -B ${arguments}"
        } else {
            bat "mvn -B ${arguments}"
        }
    }
}
