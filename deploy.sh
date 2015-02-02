#!/bin/bash
set -e
export JAVA_OPTS="-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled"
cp -a ./actor-restapi/public ./src/main/resources/
sbt clean dist
rm -rf ./src/main/resources/public
# ansible-galaxy install -r ./ansible/requirements.yml
echo "\n!!!\nFor install requirements: ansible-galaxy install -r ./ansible/requirements.yml\n!!!\n"
ansible-playbook -vvv -i ./ansible/hosts ./ansible/deploy.yml
echo "Deployed successfully."
