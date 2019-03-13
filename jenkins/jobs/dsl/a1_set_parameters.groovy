def createJob = freeStyleJob("${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning/Set_Cloning_Parameters")
def scmProject = "git@gitlab:${WORKSPACE_NAME}/Oracle_Tech.git"
def scmSSH = "git@gitlab:SSH/SSH_KEYS_PEM.git"
def scmCredentialsId = "adop-jenkins-master"

folder("${PROJECT_NAME}/Cloud_Provision") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

folder("${PROJECT_NAME}/Cloud_Provision/IaaS") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

folder("${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

Closure passwordParam(String paramName, String paramDescription, String paramDefaultValue) {
    return { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'hudson.model.PasswordParameterDefinition' {
            'name'(paramName)
      		'description'(paramDescription)
        	'defaultValue'(paramDefaultValue)
        }
    }
}

createJob.with {
    description('')
    parameters {
		stringParam('IP_ADDRESS', '', 'Resolvable Public IP address of the target clone server.')
    stringParam('HOSTNAME', '', 'Hostname of the target clone server. (eg. ebsdev1)')
    stringParam('DOMAIN', '', 'Domain Name. (eg. compute-580502591.oraclecloud.internal, compute-500022479.au1.internal)')
    stringParam('SOURCE_DATABASE_NAME', '', 'The Source Database SID. This will be the Database SID of the backup artifacts.')
		stringParam('DATABASE_NAME', '', 'The New Database SID. ')
		stringParam('APP_RUN_EDITION_DIR', '/u01/install/APPS/fs1', 'The Run File System that will point to fs1 or fs2 directory.')
    stringParam('APP_PATCH_EDITION_DIR', '/u01/install/APPS/fs2', 'The Patch File System that will point fs1 or fs2 directory.')
    stringParam('DATA_TOP_ONE', '/u01/install/APPS/data', 'Path of one of the DATA TOP. (Note: If DB only has 1 Data Top. Put the SAME value.)')
    stringParam('DATA_TOP_TWO', '/u02/oradata/DBSID', 'Path of one of the DATA TOP. (Note: If DB only has 1 Data Top. Put the SAME value.)')
		stringParam('NEXUS_USERNAME', '', 'Nexus Account\'s username. (User must have access to the nexus repository)')
		stringParam('APP_URL', '', 'Download Url of Application artifacts in Nexus.')
		stringParam('DB_BIN_URL', '', 'Download Url of Database Artifacts in Nexus.')
		stringParam('DB_DATA_URL', '', 'Download Url of Database Data 1 in Nexus.')
		stringParam('DB_ORADATA_URL', '', 'Download Url of Database Data 2 in Nexus.')
		stringParam('DB_ARCHIVE_URL', '', 'Download Url of Database Archive Folder in Nexus.')
    stringParam('SSH_KEY', '', 'SSH key of the Target Instance. (eg. rsa-key-tollgftxxx.pem)')
    stringParam('DB_PORT', '1521', 'Database Port')
    stringParam('TOLL_USERNAME', '', 'Your Toll Account Username.')
    stringParam('TOLL_PROXY', '', 'Proxy Server to use for Internet Connection.')
    }
    configure passwordParam("TOLL_PASSWORD", "Your Toll Account Password", "")
		configure passwordParam("APPS_PASSWORD", "APPS Login Password.", "")
		configure passwordParam("WEBLOGIC_PASSWORD", "Weblogic Password.", "")
		configure passwordParam("NEXUS_PASSWORD", "Nexus account's password", "")

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('postgres')
    customWorkspace('/var/jenkins_home/workspace/cloning/${BUILD_TAG}')

  multiscm {
    git {
      remote {
        url(scmSSH)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('ssh')
      }
    }
    git {     
      remote {
        url(scmProject)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('ansible')
      }
    }
  }

    wrappers {
        preBuildCleanup() 
        colorizeOutput('css')
    }

    steps {
		shell('''#!/bin/bash

mv ${WORKSPACE}/ansible/Cloud_Provision/IaaS/ebsr12-cloning/* .
mv ${WORKSPACE}/ssh/${SSH_KEY} .

rm -rf ansible ssh

cat > target-host <<-EOF
[ebs]
${IP_ADDRESS}
EOF

echo "oc-${IP_ADDRESS}" > ${WORKSPACE}/publicIP
sed -i 's|\\.|-|g' ${WORKSPACE}/publicIP

export DATABASE_NAME_LOWER=`echo $DATABASE_NAME | tr '[:upper:]' '[:lower:]'`
export DOMAIN_IP=`cat ${WORKSPACE}/publicIP`

echo "CUSTOM_WORKSPACE=${WORKSPACE}" > props
echo "ANSIBLE_CFG=${WORKSPACE}/ssh_ansible.cfg" >> props
echo "DOMAIN_IP=${DOMAIN_IP}" >> props
echo "DATABASE_NAME_LOWER=${DATABASE_NAME_LOWER}" >> props

chmod 400 ${WORKSPACE}/${SSH_KEY}
		''')

        environmentVariables {
            propertiesFile('props')
        }
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Clean_Install_Directories') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                    propertiesFile('props', true)
                }
            }
        }
    }

}
