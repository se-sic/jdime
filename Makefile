PREFIX = $(HOME)/opt

jdime:
	./gradlew distTar

.PHONY: install
install: jdime
	tar -C $(DESTDIR)$(PREFIX) -xf build/distributions/JDime.tar

.PHONY: uninstall
uninstall:
	rm -rf $(DESTDIR)$(PREFIX)/JDime

