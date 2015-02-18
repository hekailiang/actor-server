secret-backend
==============

Backend for secret app


# Java Keystore

```
openssl pkcs12 -export -in ssl-unified.crt -inkey ssl.key -out server.p12 -name actor.im -CAfile ca.crt -caname root
keytool -importkeystore -destkeystore keystore.jks -srckeystore server.p12 -srcstoretype pkcs12 -alias actor.im

# StartSSL
wget http://www.startssl.com/certs/ca.crt
wget http://www.startssl.com/certs/sub.class1.server.ca.crt
keytool -import -alias startcom.ca -file ca.crt -trustcacerts -keystore keystore.jks
keytool -import -alias startcom.ca.sub -file sub.class1.server.ca.crt -trustcacerts -keystore keystore.jks
```

# Deploy

```
./deploy.sh
```

# Example application for H2 embedded database

```
actor-server {
  actor-system-name = "actor-server"

  sql {
    driverClassName = "org.h2.Driver"
    url = "jdbc:h2:/tmp/actor.h2"
    username = "sa"
    password = ""
    user = ${actor-server.sql.username}
    pass = ${actor-server.sql.password}
  }

  jdbc-connection = ${actor-server.sql}

  jdbc-journal {
    class = "akka.persistence.jdbc.journal.H2SyncWriteJournal"
  }

  jdbc-snapshot-store {
    class = "akka.persistence.jdbc.snapshot.H2SyncSnapshotStore"
  }
}
```
