pipeline {
  agent any
  tools {
    jdk 'Java11'
  }
  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }
  stages {
    stage('Clean') {
      steps {
        sh 'chmod +x ./gradlew'
        sh './gradlew clean'
      }
    }
    stage('Test') {
      steps {
        sh './gradlew test'
        junit '**/build/test-results/test/*.xml'
      }
    }
    stage('Build') {
      steps {
        sh './gradlew jar'
      }
    }
    stage('Release ZIP') {
      steps {
        sh '''mkdir -p temp;
        cp -r .template/* temp/;
        cp -r cloudnet-examples/src/main/java/de/dytanic/cloudnet/examples/* temp/dev/;
        cp cloudnet-plugins/cloudnet-simplenametags/build/libs/*.jar temp/plugins/;
        cp cloudnet-plugins/cloudnet-chat/build/libs/*.jar temp/plugins/;
        cp cloudnet-launcher/build/libs/*.jar temp/launcher.jar;
        zip archive: true, dir: 'temp', glob: '', zipFile: 'CloudNet.zip'
        sh 'rm -r temp/';
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts artifacts: '**/build/libs/*.jar'
      }
    }
  }
}
