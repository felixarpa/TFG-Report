clean:
	rm -f */*.aux
	rm -f *.aux
	rm -f *.blg
	rm -f *.dvi
	rm -f *.log
	rm -f *.lot
	rm -f *.lof
	rm -f *.toc
	rm -f *.gz
	rm -f *.out
	rm -f *.bbl
	rm -f *.bcf
	rm -f *.blg
	rm -f *-blx.aux
	rm -f *-blx.bib
	rm -f *.run.xml

build:
	pdflatex main
	bibtex main
	pdflatex main
	pdflatex main

open:
	open -a /Applications/Preview.app main.pdf

open-default:
	open main.pdf

latex:
	make build
	make clean
	open main.pdf
