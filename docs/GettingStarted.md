# Getting Started

### Pre-requisites

To develop / deploy Slate you will need the following:

- JDK 15+ https://docs.aws.amazon.com/corretto/latest/corretto-15-ug/downloads-list.html
- MySQL 8 https://dev.mysql.com/downloads/mysql/
- Maven 3+ https://maven.apache.org/download.cgi
- Node 14 https://github.com/nvm-sh/nvm

Production deployments will additionally need:

- AWS S3
- Apache Kafka 2.3+ https://kafka.apache.org/downloads

### Compiling Slate

*Please make sure to export JAVA_HOME to the install location of your JDK*

```
git clone https://github.com/pinterest/slate.git
cd slate
mvn clean package -DskipTests
```


### Setting Up Database

Please note that the configuration below must match that of dev-config.yaml file; please change the credentials for production use and make sure to update the config file accordingly.

**Step 1:** Login to MySQL shell with root password and run (deploy/tables.sql)


**Step 2:** Now setup a user to grant access to Slate process (localuser.sql)

```
CREATE USER 'slateuser'@'localhost' IDENTIFIED BY 'slate1_passWord';
GRANT ALL PRIVILEGES ON slate.* TO 'slateuser'@'localhost';
```

### Running Server

Please run setup.sh before running the server using run_slate.sh

The setup.sh file only needs to be run once for a brand new instance.

```
deploy/setup.sh
deploy/run_slate.sh
```
