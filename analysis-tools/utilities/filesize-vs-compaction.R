# -------------------------------------------------
# Multi-series line plot of filesize vs. compaction
# -------------------------------------------------
# Parameters:

#   1) df:  is a melted dataframe of results
#   2) baseline:  value will become x-axis variable e.g. to plot plaintext
#      XML size on the x-axis, pass "xml". In real world terms, the baseline
#      should reflect the encoding that's already passing over the network.
#   3) series:  which encodings to display as series on the plot 
#   4) x.range:  The maximum and minimum values for the x-axis. Useful for
#      narrowing down to interesting parts of plots, like the small end of
#      the scale. Options are:
#        - [min, max]: A number array of length 2, indicating the minimum and
#                      maximum values for the x-axis
#        - "log":      Show full range of dataset on x-axis, using a base-10
#                      logarithmic scale
#        - "auto":     The default. Shows full range of dataset on x-axis,
#                      using a continuous scale.


filesize.vs.compaction <- function(df, baseline, series,  x.range="auto"){

  # Cast data and evaluate sizes as percentage of specified encoding
  dfc <- dcast(df, file ~ variable)
  dfcp <- data.frame(c(dfc[1], dfc[-1] / dfc[,baseline]))
  dfcp$base.size <- dfc[,baseline]
  
  # Melt the data frame back to 'long' form so ggplot2 likes it better.
  dfcp <- melt(dfcp[c(series,"base.size")], id.vars="base.size")
  
  # Generate labels for the x and y axes using baseline value
  ylabel <- paste("Compaction (%", baseline, "size)")
  xlabel <- paste("Original", baseline, "size (bytes)")
  
  # Plot the data and core aesthetics
  p <- ggplot(data = dfcp, aes(x=base.size, y=value, color=variable)) +
    geom_point(size=0.7, shape=3)  +
    geom_line(size=0.25) + # , aes(linetype=variable)
    guides(colour = guide_legend("Encoding"),
           shape = guide_legend("Encoding"),
           linetype= guide_legend("Encoding"))
  
  # Adjustments to x-scale
  if(length(x.range) == 2){
    p <- p + scale_x_continuous(xlabel, labels = comma, limits=range)
  } else if(x.range == "log") {
    p <- p + scale_x_log10(xlabel, labels = comma)
  } else {
    p <- p + scale_x_continuous(xlabel, labels = comma)
  }
  
  # Adjustments to y-scale
  p <- p + scale_y_continuous(ylabel, labels=percent_format()) 
  p <- p + coord_cartesian(ylim=c(-0.05, max(dfcp$value)*1.05))
  if(max(dfcp$value) > 1){ p <- p + geom_hline(yintercept=1) }
  
  return(p)
}