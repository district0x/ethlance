# Makefile for generating Requirements, Designs, and Specifications
# with emacs org-mode, and PlantUML
.PHONY: help all clean clean-all
.PHONY: generate-svgs generate-pngs generate-html publish-ipfs
.DEFAULT_GOAL := all
.SUFFIXES:
.SUFFIXES: .plant .svg .png .org .html


#Current Working Directory
CWD := $(shell pwd)


# Location of the plantuml jar file
PLANTUML_JAR         := ./bin/plantuml.jar
HTML_GEN_SCRIPT      := ./bin/generate-html.el

# java executable to use (default)
JAVA_CMD             := java

# Plantuml Execution Command
PLANTUML_EXEC        := $(JAVA_CMD) -jar $(PLANTUML_JAR)

# emacs executable to use (default)
EMACS_CMD            := emacs

# HTML Generation Command
HTML_GEN_EXEC        := $(EMACS_CMD) --quick --script $(HTML_GEN_SCRIPT)

# Location of initial source files
PLANT_FOLDER         := ./plant
ORG_FOLDER           := ./org

# Generated file locations
IMAGE_FOLDER         := ./org/images
HTML_FOLDER          := ./public/html
HTML_IMAGE_FOLDER    := ./public/html/images


# Retrieve all of the plantuml files
PLANT_FILES    := $(wildcard $(PLANT_FOLDER)/*.plant)

# Retrieve the index org file
ORG_FILES      := $(wildcard $(ORG_FOLDER)/*.org)


help:
	@echo "Makefile for generating requirement, design, and spec documents"
	@echo "  all                                - Generate SVGs and PNGs from ./plant folder"
	@echo "  generate-pngs                      - Generate PNGs from Plant Files"
	@echo "  generate-svgs                      - Generate SVGs from Plant Files"
	@echo "  generate-html                      - Generate HTML from ORG Files"
	@echo "  clean                              - Does Nothing"
	@echo "  clean-all                          - Clean out public folder"
	@echo "  publish-ipfs                       - Publish an IPFS website"
	@echo "  help                               - Show this help message"

all: generate-pngs generate-svgs generate-html


# Generate PNG files from plant files
PNG_FILES=$(notdir $(PLANT_FILES:.plant=.png))
PNG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(PNG_FILES))
generate-pngs: $(PNG_PATHS)


# Define how to create PNGs
$(IMAGE_FOLDER)/%.png: $(PLANT_FOLDER)/%.plant
	$(PLANTUML_EXEC) $< -o "$(CWD)/$(IMAGE_FOLDER)"


# Generate SVG files from plant files
SVG_FILES=$(notdir $(PLANT_FILES:.plant=.svg))
SVG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(SVG_FILES))
generate-svgs: $(SVG_PATHS)


# Define how to create SVGs
$(IMAGE_FOLDER)/%.svg: $(PLANT_FOLDER)/%.plant
	$(PLANTUML_EXEC) -svg $< -o "$(CWD)/$(IMAGE_FOLDER)"


# Generate HTML files from org files
HTML_FILES=$(notdir $(ORG_FILES:.org=.html))
HTML_PATHS=$(addprefix $(HTML_FOLDER)/, $(HTML_FILES))
generate-html: $(HTML_PATHS)
	@cp -r $(IMAGE_FOLDER) $(HTML_FOLDER)


# Make sure our index.html file is regenerated when the residual org files change
$(HTML_FOLDER)/index.html: $(ORG_FILES)


# Define how to create HTML files from org files
$(HTML_FOLDER)/%.html: $(ORG_FOLDER)/%.org
	mkdir -p $(HTML_FOLDER)
	$(HTML_GEN_EXEC) -- $<
	mv $(ORG_FOLDER)/$*.html $(HTML_FOLDER)/


clean:
	@echo "Nothing to clean..."


IPFS_PUBLISH_SCRIPT=./bin/publish-ipfs.sh
publish-ipfs: all
	@bash $(IPFS_PUBLISH_SCRIPT) $(HTML_FOLDER)


clean-all:
	rm -rf ./public/html/*.html
	rm -rf ./public/html/images
