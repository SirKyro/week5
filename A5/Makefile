test:
	UMLANG_VM=0 mvn test && UMLANG_VM=1 mvn test

compile:
	mvn compile

clean:
	mvn clean

run: compile
	java -cp target/classes umlang.Main

.PHONY: test compile clean run
