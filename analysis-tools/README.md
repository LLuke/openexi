[TOC]

OpenEXI Analysis Tools
==========================

Introduction
------------

The OpenEXI Analysis Tools are a set of scripts to help visualize results from compression tests. They are a series of R scripts that perform generic comparisons of various formats, crunch the numbers, and display results. Though not required, the R Markdown _templates_ offer R code chunks for a basic set of comparisons and outputting the results as HTML documents (via the `knitr` package) - rather useful for seeing results and notes all in one place. In general, the input file format (in CSV) is meant to be as simple as possible so that collecting datapoints for many different file encodings is easy, and R does what it's good at: manipulating numbers and tables and such.

R Package Dependencies
----------------------

The R scripts make use of the following R packages:

- [ggplot2](http://ggplot2.org) for graphing and data visualization.
- [reshape2](http://cran.r-project.org/web/packages/reshape2/index.html) for manipulating inputs in CSV format into R dataframes.
- [knitr](http://yihui.name/knitr/) for generating HTML/PDF reports with results.

Directory Layout
----------------

`templates` - Premade report templates for visualizing data using the knitr package or RStudio's Knit HTML functionality. The templates are made to be self-documenting, so reading the comments is a good starting point.

`utilities` - R functions to generate plots and do common number crunching tasks. ggplot2 allows for extensive customization of a plot's format and appearance. To keep them generic, avoid modifying these for the sake of pretty-printing a plot.

`sample-inputs` - examples of the CSV input format

Getting Started
---------------

_Note: These steps are aimed at users running RStudio._

1. If you don't have RStudio installed, grab it from the RStudio [site](http://www.rstudio.com/products/RStudio/)

2. Make sure you have the `ggplot2` and `reshape2` packages installed. 

3. Download/checkout the `analysis-tools` directory:

~~~
$ svn checkout svn://svn.code.sf.net/p/openexi/code/trunk/analysis-tools analysis-tools
~~~

4. Open the `xml_json.Rmd` file located in  `analysis-tools/templates/` in RStudio.

5. Run the 'Knit HTML' command to generate the report.

