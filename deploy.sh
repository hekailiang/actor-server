set -e
export JAVA_OPTS="-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M"
sbt clean dist
rsync -avzc --delete -r target/dist/ --exclude bin/start --exclude config/application.conf --exclude apns ec2-user@172.31.14.18:~/dist
ssh -v ec2-user@172.31.14.18 'pkill java & nohup ~/dist/bin/start >> ~/server.log 2>>~/server-err.log &'
echo "Deployed successfully."
