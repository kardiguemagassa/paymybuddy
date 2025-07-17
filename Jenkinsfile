pipeline {
  agent any

  environment {
    // Utilise les variables Sonar définies dans le .env ou Jenkins
    SONAR_PROJECT_KEY = "paymybuddy"
    SONAR_PROJECT_NAME = "PayMyBuddy"
    SONAR_HOST_URL = "http://sonarqube:9000"
    SONAR_LOGIN = credentials('sonar-token')  // ← définit dans Jenkins
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        echo '🔨 Compilation avec Maven...'
        sh 'mvn clean compile'
      }
    }

    stage('Tests') {
      steps {
        echo '🧪 Lancement des tests unitaires...'
        sh 'mvn test'
      }

      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('SonarQube') {
          echo 'Analyse SonarQube...'
          sh """
            mvn sonar:sonar \
              -Dsonar.projectKey=$SONAR_PROJECT_KEY \
              -Dsonar.projectName=$SONAR_PROJECT_NAME \
              -Dsonar.host.url=$SONAR_HOST_URL \
              -Dsonar.login=$SONAR_LOGIN
          """
        }
      }
    }

    stage("Quality Gate") {
      steps {
        timeout(time: 1, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }
  }

  post {
    success {
      echo '✅ Build terminé avec succès !'
    }
    failure {
      echo '❌ Échec du build !'
    }
  }
}
