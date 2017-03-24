node('master') {
  // stage 'Build and Test'
  def mvnHome = tool 'maven3'
  env.JAVA_HOME = tool 'jdk7'
  env.GRAILS_HOME = tool 'grails2.2.4'

  env.PATH = "${mvnHome}/bin:${env.GRAILS_HOME}/bin:${env.JENKINS_HOME}/bin:./:${env.PATH}"
  checkout scm

  stage('fpm') {
      sh "gem install fpm"
  }
  
  stage('Get Ansible Roles') {
    sh 'ansible-galaxy install -r ansible/requirements.yml -p ansible/roles/ -f'
  } 

  stage('Test') {
      // Run the maven test
      // sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'test'"
  }

  stage('Build') {
      // Run the maven build
      // sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'build'"
      sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'build'"
      dir('target') {
         archiveArtifacts artifacts: 'RuleChains.war'
         // stash name: "rulechainsrpm", includes: "RuleChains.war"
      }


  }
}
