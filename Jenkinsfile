pipeline {
  agent any
  tools {
    jdk 'Java8'
  }
  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }
  stages {
    stage('Clean') {
      steps {
        sh './gradlew clean'
      }
    }
    stage('Test') {
      steps {
        sh './gradlew test'
        junit 'build/test-results/test/*.xml'
      }
    }
    stage('Build') {
      steps {
        sh './gradlew jar'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts artifacts: 'build/libs/*.jar'
      }
    }
  }
}
