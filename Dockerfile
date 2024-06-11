FROM maven
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get -Dartifact=org.meyvn:meyvn:1.8.0
RUN echo 'java -Dmaven.home=/usr/share/maven -jar ~/.m2/repository/org/meyvn/meyvn/1.8.0/meyvn-1.8.0.jar "$@"' >> /usr/local/bin/myvn
RUN chmod +x /usr/local/bin/myvn
RUN curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
RUN chmod +x linux-install.sh
RUN ./linux-install.sh
CMD ["myvn"]
