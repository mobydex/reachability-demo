CWD = $(shell pwd)

POM = -f pom.xml

# Maven Clean Install Skip ; skip tests, javadoc, scaladoc, etc
MVN = mvn
MS = $(MVN) -DskipTests -Dmaven.javadoc.skip=true -Dskip
MCCS = $(MS) clean compile
MCIS = $(MS) clean install

# Source: https://stackoverflow.com/questions/4219255/how-do-you-get-the-list-of-targets-in-a-makefile
.PHONY: help
help:  ## Show these help instructions
	@sed -rn 's/^([a-zA-Z_-]+):.*?## (.*)$$/"\1" "\2"/p' < $(MAKEFILE_LIST) | xargs printf "make %-20s# %s\n"

mcis: ## mvn skip clean install (minimal build of all modules) - Passing args:  make mcis ARGS="-X"
	$(MCIS) $(POM) $(ARGS)

vaadin-production: ## Build the project vaadin in production mode (must use prior to build-docker-*)
	$(MS) $(POM) $(ARGS) -Pproduction -pl :mobydex-demo-app -am clean install

deploy-docker: ## Run mvn jib:build for the web app (run after vaadin-production)
	$(MVN) $(POM) $(ARGS) -Pproduction -pl :mobydex-demo-pkg-docker -am clean install
	$(MVN) $(POM) $(ARGS) -pl :mobydex-demo-pkg-docker jib:build

build-docker-projver: ## Build vaadin docker image tagged with the project version
	$(MVN) $(ARGS) $(POM) -pl :mobydex-demo-pkg-docker jib:dockerBuild

build-docker-latest: ## Bulid vaadin docker image tagged with 'latest'
	$(MVN) $(ARGS) $(POM) -Ddocker.tag=latest -pl :mobydex-demo-pkg-docker jib:dockerBuild

run-spring-boot: ## Run with maven and spring boot
	$(MVN) $(POM) -pl :mobydex-demo-app spring-boot:run

run-docker-latest: ## Run the vaadin docker image
	docker run -it -p8000:8000 -v 'mobydex-cache:/root/.cache/mobydex' --network host aksw/mobydex

