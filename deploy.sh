#!/bin/bash
set -e
export JAVA_OPTS="-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled"
sbt clean dist
# ansible-galaxy install -r ./ansible/requirements.yml
echo "!!! For install requirements: ansible-galaxy install -r ./ansible/requirements.yml !!!"
ansible-playbook -vvv -i ./ansible/hosts ./ansible/deploy.yml
echo "Deployed successfully."
