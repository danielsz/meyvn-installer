FROM maven
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get -Dartifact=org.meyvn:meyvn:1.7.8
RUN echo 'java -Dmaven.home=/usr/share/maven -jar ~/.m2/repository/org/meyvn/meyvn/1.7.8/meyvn-1.7.8.jar "$@"' >> /usr/local/bin/myvn
RUN chmod +x /usr/local/bin/myvn
CMD ["sh"]
