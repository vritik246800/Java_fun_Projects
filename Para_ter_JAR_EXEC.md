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
# Para ter uma biblioteca externa e ligar no VSCode
```
MeuProjeto/
 ├── lib/
 │    └── [Nome da biblioteca].jar
 ├── src/
 │    └── App.java
 └── bin/

```
## Comandos
```
javac -cp "lib/[Nome da biblioteca].jar" -d bin src/*.java
java -cp "bin:lib/[Nome da biblioteca].jar" App


```
# Para mudar o JDK no Linux
Comandos
```
sudo update-alternatives --config java
sudo update-alternatives --config javac
```
Vai dar a tabela a baixo
```
Existem 3 escolhas para a alternativa java (disponibiliza /usr/bin/java).

  Selecção   Caminho                                      Prioridade Estado
------------------------------------------------------------
* 0            /usr/lib/jvm/jdk-24.0.1-oracle-x64/bin/java   402661376 modo automático
  1            /usr/lib/jvm/java-21-openjdk-amd64/bin/java   2111      modo manual
  2            /usr/lib/jvm/jdk-24.0.1-oracle-x64/bin/java   402661376 modo manual
  3            /usr/lib/jvm/jdk-25/bin/java                  1000      modo manual
Pressione <enter> para manter a escolha actual[*], ou digite o número da selecção:


  Selecção   Caminho                                       Prioridade Estado
------------------------------------------------------------
* 0            /usr/lib/jvm/jdk-24.0.1-oracle-x64/bin/javac   402661376 modo automático
  1            /usr/lib/jvm/jdk-24.0.1-oracle-x64/bin/javac   402661376 modo manual
  2            /usr/lib/jvm/jdk-25/bin/javac                  1000      modo manual

Pressione <enter> para manter a escolha actual[*], ou digite o número da selecção: 2

```
seleciona o vercao com numero
Comandos seguido
```
sudo update-alternatives --config java
sudo update-alternatives --config javac
```
Para verisicar
```
java --version
javac --version
```


