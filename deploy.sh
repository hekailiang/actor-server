#!/bin/bash
set -e
export JAVA_OPTS="-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M"
cp -a ./actor-restapi/public ./src/main/resources/
sbt clean dist
rm -rf ./src/main/resources/public
ansible-playbook -vvv -i ./ansible/hosts ./ansible/deploy.yml
echo "Deployed successfully."
