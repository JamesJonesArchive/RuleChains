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
  stage('Stash Deploy Related') {
    sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all keystash=${env.USF_ANSIBLE_VAULT_KEY}' -t keystash"
    stash name: 'keystash', includes: "rpms/ansible-vault-usf*.rpm"
    stash name: 'ansible', includes: "ansible/**/*"
  }
  stage('Test') {
      // Run the maven test
      // sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'test'"
  }

  stage('Build') {
      // Run the maven build
      // sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'build'"
      sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t 'build'"
      dir('target') {
         // archiveArtifacts artifacts: 'RuleChains*.rpm'
         stash name: "rulechainsrpm", includes: "RuleChains*.rpm"
      }


  }
}
node("rulechains") {
  env.ANSIBLE_HOME = tool 'ansible2.2.0'
  env.JAVA_HOME = tool 'jdk8'
  // env.PATH = "${env.JENKINS_HOME}/bin:${env.ANSIBLE_HOME}/bin:${env.PATH}"
  env.PATH = "${env.JENKINS_HOME}/bin:${env.PATH}"
  stage('Unstash the rpms') {
    sh 'rm -rf rpms'
    unstash 'keystash'
    dir('rpms') {
      unstash 'rulechainsrpm'
    }
  }
  stage('Install Ansible') {
    def distVer = sh script: 'python -c "import platform;print(platform.linux_distribution()[1])"', returnStdout: true
    def missingEpel = sh script: 'rpm -q --quiet epel-release', returnStatus: true
    def missingAnsible = sh script: 'rpm -q --quiet ansible', returnStatus: true
    if (missingEpel) {
      echo "Epel to be installed"
      if(distVer.toFloat().trunc() == 7) {
        echo "Detected version 7"
        sh "rpm -iUvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm || exit 0"
      }
      if(distVer.toFloat().trunc() == 6) {
        echo "Detected version 6"
        sh "rpm -iUvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm || exit 0"
      }
      if(distVer.toFloat().trunc() == 5) {
        echo "Detected version 5"
        sh "rpm -iUvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-5.noarch.rpm || exit 0"
      }
      sh "yum -y update || exit 0"
    } else {
      echo "Already installed"
    }
    if(missingAnsible) {
      sh "yum -y install ansible || exit 0"
    }
    sh 'yum -y install rpms/ansible-vault-usf*.rpm || exit 0'
    unstash 'ansible'
  }
  // stage('Deploy RuleChains') {
  //   sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/playbook.yml --extra-vars 'target_hosts=all java_home=${env.JAVA_HOME} deploy_env=${env.DEPLOY_ENV} package_revision=${env.BUILD_NUMBER}' -t deploy"
  // }
}
node('master') {
  stage('Build RPM artifacts') {
    sh 'rm -rf rpms'
    dir('rpms') {
      unstash 'rulechainsrpm'
      archiveArtifacts artifacts: 'RuleChains*.rpm'
    }
  }
}