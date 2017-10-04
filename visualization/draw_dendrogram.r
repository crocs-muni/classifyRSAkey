library(grDevices)

readRawTable = function(tablePath) {
    rawTable = read.csv2(tablePath, sep = ",", header = T, stringsAsFactors = F, check.names = FALSE)
    rownames(rawTable) = rawTable[,1] # first column is the classification vector
    rawTable = rawTable[,2:ncol(rawTable)] # remove first column
    return(rawTable)
}

groupSources = function(rawTable, clusteringThreshold) {
    # normalize by columns, since key counts are different
    rawTable = as.matrix(rawTable)
    rawTable = rawTable/colSums(rawTable)[col(rawTable)]
    
    # cluster using hclust and Euclidean distances
    clustered = hclust(dist(t(rawTable)))
    # group according to empirically found clustering threshold
    grouped = cutree(clustered, h = clusteringThreshold)
    
    groups = unique(grouped)
    groupedTable = matrix(0, ncol = length(groups), nrow = nrow(rawTable))
    colnames(groupedTable) = paste0("Group ", groups)
    rownames(groupedTable) = rownames(rawTable)

    for (group in groups) {
        sourcesInGroup = names(grouped[which(grouped == group)])
        for (source in sourcesInGroup) {
            # add together distributions of all sources in the group
            groupedTable[,group] = groupedTable[,group] + rawTable[,source]
        }
        # normalize, possibly more sources in a group
        groupedTable[,group] = groupedTable[,group]/sum(groupedTable[,group])
    }
    return(list("table" = groupedTable, "grouping" = grouped, "clustering" = clustered))
}

colorAssignment = function(x, colors, grouped, useFixedColors) {
    if (is.leaf(x)) {
        a = attributes(x)
        groupID = a$label
        groupLabel = grouped[a$label]
        if (useFixedColors) {
            groupIDs = c(13, 12, 7, 5, 10, 4, 9, 3, 11, 1, 2, 6, 8)
            groupLabel = groupIDs[grouped[a$label]]
            labCol = colors[groupLabel]
        } else {
            labCol = colors[grouped[which(names(grouped) == a$label)]]
        }
        
        attr(x, "nodePar") = list(lab.cex = 1.65, lab.col = labCol, label = "test", lab.font=2, pch="o")  
        attr(x, "label") = paste0("[", groupLabel, "] ", groupID)
    }
    return(x)
}

drawGeneral = function(groupedTable, clustered, grouped, clusteringThreshold) {
    groupCount = ncol(groupedTable)
    par = par(mfrow = c(1, 1), mar = c(5, 1, 5, 40), cex = 0.7, 
              cex.axis = 2, cex.lab = 2, cex.main = 3)
    useFixedColors = groupCount == 13
    if (useFixedColors) {
        colors = c("#AEC7E8", "#FFBB78", "#98DF8A", "#FF9896", "#C5B0D5", 
                   "#C49C94", "#2CA02C", "#DBDB8D", "#E377C2", "#C7C7C7", 
                   "#17BECF", "#D62728", "#FF7F0E")
    } else {
        colors = sort(rainbow(groupCount, s = 1, v = 0.8))
    }

    dendrogram = dendrapply(as.dendrogram(clustered), 
                            function(x) {return(colorAssignment(x, colors, grouped, useFixedColors))})
    plot(dendrogram, main = "                                                  Clustering of sources", horiz = T, 
         xlab = "Euclidean distance") #clustered$dist.method
    
    abline(v = clusteringThreshold, untf = FALSE, col = "blue", lty = 2, lwd = 3)
    mtext("Clustering threshold = 0.02", side = 3, line = -2, outer = FALSE, col = "blue", at = 0.19)
}

processDirectory = function(rawTablePath,
                            clusteringThreshold = 0.02) {
    # table where all sources (libraries) have their own column
    rawTable = readRawTable(rawTablePath)
    
    grouping = groupSources(rawTable, clusteringThreshold)
    # table where each group has one column (1 group ay be more sources)
    groupedTable = data.frame(grouping$table)
    clustered = grouping$clustering # result of hclust
    grouped = grouping$grouping # result of cutree

    # draw dendrogram
    drawGeneral(groupedTable, clustered, grouped, clusteringThreshold)

}

args = commandArgs(TRUE)
if (length(args) != 1) {
    print("Usage: 1 argument - raw classification table (-er)")
} else {
    rawTablePath = args[1]
    
    pdf("dendrogram.pdf", width = 8, height = 12)
    processDirectory(rawTablePath)
    dev.off()
    embedFonts("dendrogram.pdf")
}
