# client-cli (JDK 8 wsimport)

Client SOAP (JAX-WS) qui génère les stubs **au build** à partir du WSDL exposé par le serveur.

## Prérequis
- JDK **8** installé (pour `wsimport`)
- Maven
- Serveur lancé et exposant le WSDL (ex.: `http://localhost:8080/hotelservice?wsdl`)

## Générer et exécuter

```bash
# Optionnel : configurer un toolchain JDK 8 (~/.m2/toolchains.xml)
# Voir: https://maven.apache.org/plugins/maven-toolchains-plugin/

# Build (génère stubs + compile en Java 8)
mvn -DskipTests clean package -Dwsdl.url=http://localhost:8080/hotelservice?wsdl

# Exécuter (le classpath contiendra les classes générées compilées)
java -cp target/client-cli-1.0.0.jar org.examples.client.ClientMain -Dwsdl.url=http://localhost:8080/hotelservice?wsdl
```

> Remarque : le nom de la classe Service générée (`HotelServiceImplService`) dépend du serveur.
> Ajustez `ClientMain` si nécessaire pour coller aux noms générés par `wsimport`.
