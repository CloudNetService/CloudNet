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
        sh 'chmod +x ./gradlew';
        sh './gradlew clean';
      }
    }
    stage('Test') {
      steps {
        sh './gradlew test';
        junit '**/build/test-results/test/*.xml';
      }
    }
    stage('Build') {
      steps {
        sh './gradlew jar';
      }
    }
    stage('Release ZIP') {
      steps {
        echo 'Creating CloudNet.zip file...';
        sh 'mkdir -p temp';
        sh 'cp -r .template/* temp/';
        sh 'mkdir temp/dev';
        sh 'mkdir temp/dev/examples';
        sh 'cp -r cloudnet-examples/src/main/java/de/dytanic/cloudnet/examples/* temp/dev/examples';
        sh 'mkdir temp/plugins';
        sh 'cp cloudnet-plugins/**/build/libs/*.jar temp/plugins/';
        sh 'cp cloudnet-launcher/build/libs/launcher.jar temp/launcher.jar';
        zip archive: true, dir: 'temp', glob: '', zipFile: 'CloudNet.zip';
        sh 'rm -r temp/';
      }
    }
    stage('AutoUpdater ZIP') {
      steps {
        echo 'Creating AutoUpdater.zip file...';
        sh 'mkdir -p temp';
        sh 'cp -r cloudnet/build/libs/*.jar temp/';
        sh 'cp -r cloudnet-driver/build/libs/*.jar temp/';
        sh 'cp -r cloudnet-modules/**/build/libs/*.jar temp/';
        sh 'cp -r **/build/libs/*.cnl temp/';
        zip archive: true, dir: 'temp', glob: '', zipFile: 'AutoUpdater.zip';
        sh 'rm -r temp/';
      }
    }
    stage('Maven') {
      steps {
        echo 'Creating artifacts for the maven repo...';
        sh './gradlew sourceJar';
        sh './gradlew deploymentJar';
        sh './gradlew javadocJar';
      }
    }
    stage('Javadoc') {
        steps {
          echo 'Creating javadoc...';
          sh './gradlew allJavadoc';
          zip archive: true, dir: 'build/javadoc', glob: '', zipFile: 'Javadoc.zip';
        }
    }
    stage('Archive') {
      steps {
        archiveArtifacts artifacts: '**/build/libs/*.jar'
      }
    }
  }
}
