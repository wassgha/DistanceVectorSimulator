compile:
	javac $$(find . -name '*.java')
clean:
	rm -r *.class
cc:
	make clean; make compile
run:
	java Router config1.txt
run2:
	java Router config2.txt
run3:
	java Router config3.txt
