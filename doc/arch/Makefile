#LATEX=latex
LATEX=pdflatex
BASENAME=aquarium
TEXFILE=$(BASENAME).tex
PDFFILE=$(patsubst %.tex,%.pdf,${TEXFILE})
BIB=$(patsubst %.tex,%,${TEXFILE})

all: compile

bib:
	pybcompact.py $(BASENAME).aux ~/Documents/biblists/ |egrep -vi "Bdsk|Url"> $(BIB) 

compile: 
	${LATEX} ${TEXFILE}
	bibtex ${BIB}
	${LATEX} ${TEXFILE}
	${LATEX} ${TEXFILE}
#	dvipdf lr-softeng.dvi

clean:
	-rm -f *.aux
	-rm -f ${PDFFILE} 
	-rm -f *.spl *.bbl *.blg *.log *.out *.dvi *~
	-find . -type f|grep .DS_Store|xargs rm
	-find . -type f|grep .class|xargs rm

