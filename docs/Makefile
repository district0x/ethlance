# Makefile for generating Requirements, Designs, and Specifications
# with emacs org-mode, and PlantUML
.PHONY: help all clean
.DEFAULT_GOAL := all
,SUFFIXES:
.SUFFIXES: .plant .svg .png

# Location of the plantuml jar file
PLANTUML_JAR := ./bin/plantuml.jar

# java executable to use (default)
JAVA_CMD := java

# Plantuml Execution command
PLANTUML_EXEC := $(JAVA_CMD) -jar $(PLANTUML_JAR)

# Location of the plant files
PLANT_FOLDER := ./diagrams


# Retrieve all of the plantuml files
PLANT_FILES := $(wildcard $(PLANT_FOLDER)/*.plant)


help:
	@echo "Makefile for generating requirement, design, and spec documents"


all: generate-pngs generate-svgs


# Generate PNG files from plant files
PNGS=$(PLANT_FILES:.plant=.png)
generate-pngs: $(PNGS)


# Define how to create PNGs
%.png: %.plant
	$(PLANTUML_EXEC) $<


# Generate SVG files from plant files
SVGS=$(PLANT_FILES:.plant=.svg)
generate-svgs: $(SVGS)


# Define how to create SVGs
%.svg: %.plant
	$(PLANTUML_EXEC) -svg $<


clean:
	rm -rf $(PLANT_FOLDER)/*.png
	rm -rf $(PLANT_FOLDER)/*.svg
