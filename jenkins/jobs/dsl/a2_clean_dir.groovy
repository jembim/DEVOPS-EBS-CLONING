def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning"
def createJob = freeStyleJob(containerFolder + '/Clean_Install_Directories')

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
        stringParam('TOLL_USERNAME', '', '')
        stringParam('TOLL_PASSWORD', '', '')
        stringParam('TOLL_PROXY', '', '')
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

        copyArtifacts('Set_Cloning_Parameters') {
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
echo "# =====================> CLEANING INSTALL DIRECTORIES <============================ #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root \
-e "target=ebs db_name=${DATABASE_NAME} app_run_dir=${APP_RUN_EDITION_DIR} app_patch_dir=${APP_PATCH_EDITION_DIR}" \
--tags "clean" site.yml

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root \
-e "target=ebs toll_user=${TOLL_USERNAME} toll_password=${TOLL_PASSWORD} toll_proxy=${TOLL_PROXY}" \
--tags "create" site.yml

		''')
	}

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Download_DB_Artifacts,Download_APP_Artifacts') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}