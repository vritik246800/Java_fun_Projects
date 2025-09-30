# Primeiro ter todas .class
    javac -d bin src/*.java
# De seguida criar um .MF
    nano MANIFEST.MF
## Dentro escrever
    Manifest-Version: 1.0
    Main-Class: TodosNotas
# E por fim criar o .JAR
    jar cfm [Nome].jar MANIFEST.MF -C bin .

# Como Executar o .JAR
## No Mac e Windows o double click.
## No Linux 
   Abre termina e Executar:
```
java -jar [Nome].jar
```
