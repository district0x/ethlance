# Makefile for generating Requirements, Designs, and Specifications
# with emacs org-mode, and PlantUML
.PHONY: help all clean clean-all
.PHONY: generate-plant-svgs generate-plant-pngs generate-org-html
.PHONY: publish-ipfs publish-ipns
.DEFAULT_GOAL := all
.SUFFIXES:
.SUFFIXES: .plant .svg .png .org .html .dot .dia


#Current Working Directory
CWD := $(shell pwd)


# Location of the plantuml jar file
PLANTUML_JAR         := ./bin/plantuml.jar
HTML_GEN_SCRIPT      := ./bin/generate-org-html.el
HTML_HASH_FILE       := .site-hash

# java executable to use (default)
JAVA_CMD             := java

# Plantuml Execution Command
PLANTUML_EXEC        := $(JAVA_CMD) -jar $(PLANTUML_JAR)

# Emacs Executable to use (default)
EMACS_CMD            := emacs

# Dia Execution Command
DIA_CMD              := dia

# Dot (graphviz) Execution Command
DOT_CMD              := dot

# HTML Generation Command
HTML_GEN_EXEC        := $(EMACS_CMD) --quick --script $(HTML_GEN_SCRIPT)

# Base Config
PUBLIC_FOLDER        := ./public
BASE_FOLDER          := ./base
DATA_FOLDER          := ./diagrams

# Location of initial source files
DIA_FOLDER           := $(DATA_FOLDER)
DOT_FOLDER           := $(DATA_FOLDER)
PLANT_FOLDER         := $(DATA_FOLDER)
ORG_FOLDER           := $(BASE_FOLDER)

# Generated file locations
IMAGE_FOLDER         := $(BASE_FOLDER)/images
HTML_FOLDER          := $(PUBLIC_FOLDER)/html
HTML_IMAGE_FOLDER    := $(HTML_FOLDER)/images

# Retrieve all of the dot diagram files
DOT_FILES      := $(wildcard $(DOT_FOLDER)/*.dot)

# Retrieve all of the dia diagram files
DIA_FILES      := $(wildcard $(DIA_FOLDER)/*.dia)

# Retrieve all of the plantuml files
PLANT_FILES    := $(wildcard $(PLANT_FOLDER)/*.plant)

# Retrieve all of the org files
ORG_FILES      := $(wildcard $(ORG_FOLDER)/*.org)


help:
	@echo "Makefile for generating requirement, design, and spec documents"
	@echo ""
	@echo "  all                                - Generate SVGs and PNGs from ${DATA_FOLDER} folder (default)"
	@echo "  --"
	@echo "  generate-plant-pngs                - Generate PNGs from Plant Files (${PLANT_FOLDER}/*.plant)"
	@echo "  generate-plant-svgs                - Generate SVGs from Plant Files (${PLANT_FOLDER}/*.plant)"
	@echo "  --"
	@echo "  generate-dia-pngs                  - Generate PNGs from Dia Files (${DIA_FOLDER}/*.dia)"
	@echo "  generate-dia-svgs                  - Generate SVGs from Dia Files (${DIA_FOLDER}/*.dia)"
	@echo "  --"
	@echo "  generate-dot-pngs                  - Generate PNGs from Dot Files (${DOT_FOLDER}/*.dot)"
	@echo "  generate-dot-svgs                  - Generate SVGs from Dot Files (${DOT_FOLDER}/*.dot)"
	@echo "  --"
	@echo "  generate-org-html                  - Generate HTML from ORG Files (${ORG_FOLDER}/*.org)"
	@echo "  publish-ipfs                       - Publish an IPFS website"
	@echo "  publish-ipns                       - Publish an IPNS website"
	@echo "  --"
	@echo "  clean                              - Does Nothing"
	@echo "  clean-all                          - Clean out public folder"
	@echo "  --"
	@echo "  help                               - Show this help message"

all: generate-plant-pngs generate-plant-svgs generate-org-html generate-dia-pngs generate-dia-svgs generate-dot-pngs generate-dot-svgs

#
# PLANT
#

# Generate PNG files from plant files
PLANT_PNG_FILES=$(notdir $(PLANT_FILES:.plant=.png))
PLANT_PNG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(PLANT_PNG_FILES))
generate-plant-pngs: $(PLANT_PNG_PATHS)


# Define how to create PNGs
$(IMAGE_FOLDER)/%.png: $(PLANT_FOLDER)/%.plant
	$(PLANTUML_EXEC) $< -o "$(CWD)/$(IMAGE_FOLDER)"


# Generate SVG files from plant files
PLANT_SVG_FILES=$(notdir $(PLANT_FILES:.plant=.svg))
PLANT_SVG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(PLANT_SVG_FILES))
generate-plant-svgs: $(PLANT_SVG_PATHS)


# Define how to create SVGs
$(IMAGE_FOLDER)/%.svg: $(PLANT_FOLDER)/%.plant
	$(PLANTUML_EXEC) -svg $< -o "$(CWD)/$(IMAGE_FOLDER)"

#
# DIA
#

# Generate PNG files from dia files
DIA_PNG_FILES=$(notdir $(DIA_FILES:.dia=.png))
DIA_PNG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(DIA_PNG_FILES))
generate-dia-pngs: $(DIA_PNG_PATHS)


# Define how to create PNGs
$(IMAGE_FOLDER)/%.png: $(DIA_FOLDER)/%.dia
	$(DIA_CMD) --export="$(CWD)/$(IMAGE_FOLDER)/$*.png" -t png $<


# Generate SVG files from dia files
DIA_SVG_FILES=$(notdir $(DIA_FILES:.dia=.svg))
DIA_SVG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(DIA_SVG_FILES))
generate-dia-svgs: $(DIA_SVG_PATHS)


# Define how to create SVGs
$(IMAGE_FOLDER)/%.svg: $(DIA_FOLDER)/%.dia
	$(DIA_CMD) --export="$(CWD)/$(IMAGE_FOLDER)/$*.svg" -t svg $<

#
# DOT (graphviz)
#

# Generate PNG files from dot files
DOT_PNG_FILES=$(notdir $(DOT_FILES:.dot=.png))
DOT_PNG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(DOT_PNG_FILES))
generate-dot-pngs: $(DOT_PNG_PATHS)


# Define how to create PNGs
$(IMAGE_FOLDER)/%.png: $(DOT_FOLDER)/%.dot
	$(DOT_CMD) $< -o "$(CWD)/$(IMAGE_FOLDER)/$*.png" -Tpng


# Generate SVG files from dot files
DOT_SVG_FILES=$(notdir $(DOT_FILES:.dot=.svg))
DOT_SVG_PATHS=$(addprefix $(IMAGE_FOLDER)/, $(DOT_SVG_FILES))
generate-dot-svgs: $(DOT_SVG_PATHS)


# Define how to create SVGs
$(IMAGE_FOLDER)/%.svg: $(DOT_FOLDER)/%.dot
	$(DOT_CMD) $< -o "$(CWD)/$(IMAGE_FOLDER)/$*.svg" -Tsvg

#
# ORG
#


# Generate HTML files from org files
ORG_HTML_FILES=$(notdir $(ORG_FILES:.org=.html))
ORG_HTML_PATHS=$(addprefix $(HTML_FOLDER)/, $(ORG_HTML_FILES))
generate-org-html: $(ORG_HTML_PATHS)
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


IPNS_PUBLISH_SCRIPT=./bin/publish-ipns.sh
IPFS_HASH_FILE=.site-hash
publish-ipns: publish-ipfs
	@bash $(IPNS_PUBLISH_SCRIPT) $(shell cat $(IPFS_HASH_FILE))


clean-all:
	rm -rf $(HTML_FOLDER)/images
	rm -rf $(HTML_FOLDER)/*.html
