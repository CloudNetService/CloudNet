#!groovy

pipeline {
  agent any
  tools {
    jdk 'Java11'
  }

  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }

  stages {
    stage('Clean-Build') {
      steps {
        configFileProvider([configFile(fileId: "e94f788c-1d9c-48d4-b9a9-8286ff68275e", targetLocation: 'gradle.properties')]) {
          sh './gradlew clean build jar -x test -x allJavaDoc --full-stacktrace'
        }
      }
    }

    stage('Run unit tests') {
      steps {
        sh './gradlew test --full-stacktrace'
        junit '**/build/test-results/test/*.xml'
      }
    }

    stage('Build Javadocs') {
      when {
        anyOf {
          branch 'master'
          branch 'development'
        }
      }

      steps {
        sh './gradlew allJavadoc --full-stacktrace'
        zip archive: true, dir: 'build/javadoc', glob: '', zipFile: 'Javadoc.zip'
      }
    }

    stage('Publish to repository') {
      when {
        anyOf {
          branch 'master'
          branch 'development'
        }
      }

      steps {
        sh './gradlew publish --full-stacktrace'
      }
    }

    stage('Release ZIP') {
      steps {
        sh 'mkdir -p temp'
        sh 'cp -r .template/* temp/'

        sh 'cp license.txt temp/license.txt'

        sh 'mkdir temp/dev'
        sh 'mkdir temp/dev/examples'
        sh 'cp -r cloudnet-examples/src/main/java/de/dytanic/cloudnet/examples/* temp/dev/examples'

        sh 'mkdir temp/extras/plugins'
        sh 'cp cloudnet-plugins/**/build/libs/*.jar temp/extras/plugins/'

        sh 'mkdir temp/extras/modules'
        sh 'cp cloudnet-modules/cloudnet-labymod/build/libs/*.jar temp/extras/modules/'
        sh 'cp cloudnet-modules/cloudnet-npcs/build/libs/*.jar temp/extras/modules/'

        sh 'cp cloudnet-launcher/build/libs/launcher.jar temp/launcher.jar'
        zip archive: true, dir: 'temp', glob: '', zipFile: 'CloudNet.zip'

        sh 'rm -r temp/'
      }
    }

    stage('AutoUpdater ZIP') {
      steps {
        echo 'Creating AutoUpdater.zip file...'
        sh 'mkdir -p temp'

        sh 'cp -r cloudnet/build/libs/*.jar temp/'
        sh 'cp -r cloudnet-driver/build/libs/*.jar temp/'
        sh 'cp -r cloudnet-modules/**/build/libs/*.jar temp/'
        sh 'cp -r **/build/libs/*.cnl temp/'
        zip archive: true, dir: 'temp', glob: '', zipFile: 'AutoUpdater.zip'

        sh 'rm -r temp/'
      }
    }

    stage('Archive') {
      steps {
        archiveArtifacts artifacts: '**/build/libs/*.jar'
        archiveArtifacts artifacts: '**/build/libs/*.cnl'
      }
    }
  }

  post {
    always {
      withCredentials([string(credentialsId: 'cloudnet-discord-ci-webhook', variable: 'url')]) {
        discordSend(
          description: 'New build for CloudNet v3!',
          footer: 'New build!',
          link: env.BUILD_URL,
          successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'),
          title: JOB_NAME,
          webhookURL: url
        )
      }
    }
  }
}
