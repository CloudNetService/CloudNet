pipeline {
  agent any
  tools {
    gradle 'Gradle6'
    jdk 'Java11'
  }
  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }
  stages {
    stage('Clean') {
      steps {
        sh 'gradle clean';
      }
    }
    stage('Test') {
      steps {
        sh 'gradle test';
        junit '**/build/test-results/test/*.xml';
      }
    }
    stage('Build') {
      steps {
        sh 'gradle jar';
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
    stage('Maven Publish') {
      when {
        anyOf {
          branch 'master';
          branch 'development';
          branch 'apache-archiva';
        }
      }
      steps {
        echo 'Publishing artifacts to Apache Archiva...';
        configFileProvider([configFile(fileId: "e94f788c-1d9c-48d4-b9a9-8286ff68275e", targetLocation: 'gradle.properties')]) {
          sh 'gradle publish';
        }
      }
    }
    stage('Javadoc') {
        steps {
          echo 'Creating javadoc...';
          sh 'gradle allJavadoc';
          zip archive: true, dir: 'build/javadoc', glob: '', zipFile: 'Javadoc.zip';
        }
    }
    stage('Archive') {
      steps {
        archiveArtifacts artifacts: '**/build/libs/*.jar';
        archiveArtifacts artifacts: '**/build/libs/*.cnl';
      }
    }
  }
  post {
    always {
      withCredentials([string(credentialsId: 'cloudnet-discord-ci-webhook', variable: 'url')]) {
        discordSend description: 'New build for CloudNet v3!', footer: 'New build!', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: JOB_NAME, webhookURL: url
      }
    }
  }
}
