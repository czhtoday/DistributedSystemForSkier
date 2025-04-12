# pre-install docker on ec2 (Amazon Linux 2, x86 arch)

#sudo yum install docker -y
#sudo service docker start
#sudo systemctl enable docker
#sudo usermod -a -G docker ec2-user


#exit # to enable group permission
#ssh again

# verify installation
#docker --version


# basic image
FROM tomcat:9.0.99-jdk17

# copy war file into webapps directory
COPY skiersServer_war.war /usr/local/tomcat/webapps/server.war

# CMD ["catalina.sh", "run"] # automatically run tomcat already

#docker build -t skiers-server .
#docker run -d -p 8080:8080 \
#   -v ~/.aws/credentials:/root/.aws/credentials \
#   --name skiers-app skiers-server

# Test Postman: http://44.247.55.73:8080/server/skiers/7/seasons/2025/days/1/skiers/99052