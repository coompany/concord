pandoc=pandoc -s
markdown=-t markdown_github
citations=--filter pandoc-citeproc
plantuml=--filter ./plantuml.py

all: readme specs

readme:
	${pandoc} README.pdoc ${markdown} -o README.md

specs:
	${pandoc} specs.pdoc ${citations} ${markdown} ${plantuml} -o specs.md

clean:
	rm ./*.md
