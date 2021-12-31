/*
 * Copyright 2019-2022 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
  agent any
  tools {
    jdk 'Java17'
  }

  options {
    buildDiscarder logRotator(numToKeepStr: '10')
  }

  stages {
    stage('Build lifecycle') {
      steps {
        configFileProvider([configFile(fileId: "e94f788c-1d9c-48d4-b9a9-8286ff68275e", targetLocation: 'gradle.properties')]) {
          sh './gradlew --full-stacktrace'
        }
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

    stage('Artifacts zip') {
      steps {
        sh 'mkdir -p temp/';
        sh 'mkdir -p temp/plugins';

        sh 'cp -r .template/* temp/';
        sh 'cp LICENSE temp/license.txt';
        sh 'cp launcher/java17/build/libs/launcher.jar temp/launcher.jar;'

        sh 'find plugins/ -type f -regex ".*/build/libs/.*\\.jar" ! -name "*-javadoc.jar" ! -name "*-sources.jar" -exec cp {} temp/plugins \\;'
        zip archive: true, dir: 'temp', glob: '', zipFile: 'CloudNet.zip'

        sh 'rm -r temp/'
      }
    }
  }
}
