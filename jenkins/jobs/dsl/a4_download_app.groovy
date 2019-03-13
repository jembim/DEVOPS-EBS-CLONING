def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning"
def createJob = freeStyleJob(containerFolder + '/Download_APP_Artifacts')

createJob.with {
    description('')
    parameters {
        stringParam('IP_ADDRESS', '', '')
		stringParam('HOSTNAME', '', '')
        stringParam('DOMAIN', '', '')
        stringParam('SOURCE_DATABASE_NAME', '', '')
		stringParam('DATABASE_NAME', '', '')
		stringParam('APP_RUN_EDITION_DIR', '', '')
        stringParam('APP_PATCH_EDITION_DIR', '', '')
        stringParam('DATA_TOP_ONE', '', '')
        stringParam('DATA_TOP_TWO', '', '')
        stringParam('DATABASE_NAME_LOWER', '', '')
		stringParam('NEXUS_USERNAME', '', '')
		stringParam('APP_URL', '', '')
		stringParam('DB_BIN_URL', '', '')
		stringParam('DB_DATA_URL', '', '')
		stringParam('DB_ORADATA_URL', '', '')
		stringParam('DB_ARCHIVE_URL', '', '')
		stringParam('APPS_PASSWORD', '', '')
        stringParam('WEBLOGIC_PASSWORD', '', '')
		stringParam('NEXUS_PASSWORD', '', '')
		stringParam('DOMAIN_IP', '', '')
		stringParam('ANSIBLE_CFG', '', '')
		stringParam('CUSTOM_WORKSPACE', '', '')
        stringParam('SSH_KEY', '', '')
        stringParam('DB_PORT', '', '')
    }

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('postgres')
    customWorkspace('$CUSTOM_WORKSPACE')

    wrappers {
        colorizeOutput('css')
    }

    steps {

        copyArtifacts('Clean_Install_Directories') {
            includePatterns('**/*')
            fingerprintArtifacts(true)
            buildSelector {
                upstreamBuild(true)
                latestSuccessful(false)
            }
        }

        shell('''#!/bin/bash

export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

echo "#####################################################################################"
echo "# ===================> DOWNLOADING APPLICATION ARTIFACTS <========================= #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root -e "target=ebs repo_username=${NEXUS_USERNAME} repo_password=${NEXUS_PASSWORD} \
app_artifact_url=${APP_URL} app_run_dir=${APP_RUN_EDITION_DIR}" --tags "download_app" site.yml

		''')
	}

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Extract_APP_Archives') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}