pipeline {
  agent any
  options {
    ansiColor('xterm')
    timestamps()
  }
  stages {
    stage('build') {
      environment {
        BUILD_CACHE_USER = vault path: '/secret/streamsets-ci/gradle-build-cache-node', key: 'username'
        BUILD_CACHE_PASSWORD = vault path: '/secret/streamsets-ci/gradle-build-cache-node', key: 'password'
        ORG_GRADLE_PROJECT_nexusUsername = vault path: '/secret/streamsets-ci/artifactory', key: 'username'
        ORG_GRADLE_PROJECT_nexusPassword = vault path: '/secret/streamsets-ci/artifactory', key: 'token'
      }
      steps {
        configFileProvider([configFile(fileId: 'a1872c47-705b-4380-b708-620adf63c914', targetLocation: 'init.gradle')]) {
          sh './gradlew --init-script init.gradle --continue --build-cache --parallel uploadArchives'
          // if tests were not re-run touch the files so jenkins doesn't complain about the timestamps
          sh 'find .  -type f  -name \'*.xml\' -exec touch {} +'
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }
      }
    }
  }
}
