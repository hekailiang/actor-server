set -e
export JAVA_OPTS="-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M"
sbt clean dist
rsync -avzc --delete -r target/dist/ --exclude bin/start --exclude config/application.conf --exclude apns ec2-user@172.31.14.18:~/dist
ssh -v ec2-user@172.31.14.18 'cp ~/application.conf ~/dist/config;cp -a ~/apns ~/dist/;cp ~/start ~/dist/bin/;chmod +x ~/dist/bin/start'
ssh -v ec2-user@172.31.14.18 'pkill java & nohup ~/dist/bin/start >> ~/server.log 2>>~/server-err.log &'
echo "Deployed successfully."
