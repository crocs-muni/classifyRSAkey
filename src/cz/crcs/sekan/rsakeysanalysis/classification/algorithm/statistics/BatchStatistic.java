package cz.crcs.sekan.rsakeysanalysis.classification.algorithm.statistics;

import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationContainer;
import cz.crcs.sekan.rsakeysanalysis.classification.table.ClassificationRow;
import cz.crcs.sekan.rsakeysanalysis.common.Range;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * Holds statistics for all batches of a certain size.
 * Can be positive or negative classification result.
 * Keys can be unique or with duplicities.
 * The advantage of batches of same size is, that each contributes with same number of keys.
 * Statistics for a range of batch size can be reconstructed.
 *
 * @author xnemec1
 * @version 2/24/17.
 */
public class BatchStatistic {
    public enum ClassificationType {
        POSITIVE,
        NEGATIVE
    }

    public enum DuplicityType {
        UNIQUE,
        DUPLICATE
    }

    private Range<Long> batchSize;
    private Long keyCount;
    private ClassificationRow commonClassification;

    private ClassificationType classificationType;
    private DuplicityType duplicityType;
    private List<String> sourceNames;

    public BatchStatistic(long batchSize, ClassificationType classificationType, DuplicityType duplicityType, List<String> sourceNames) {
        this(new Range<Long>(batchSize, batchSize), classificationType, duplicityType, sourceNames);
    }

    public BatchStatistic(Range<Long> batchSize, ClassificationType classificationType, DuplicityType duplicityType, List<String> sourceNames) {
        this.batchSize = batchSize;
        this.keyCount = 0L;
        this.classificationType = classificationType;
        this.duplicityType = duplicityType;
        this.sourceNames = sourceNames;
        String[] groupNamesArray = sourceNames.toArray(new String[sourceNames.size()]);
        commonClassification = new ClassificationRow(groupNamesArray);
    }

    public BatchStatistic(Range<Long> batchSize, ClassificationType classificationType, DuplicityType duplicityType, List<String> sourceNames, ClassificationRow row) {
        this.batchSize = batchSize;
        this.keyCount = 0L;
        this.classificationType = classificationType;
        this.duplicityType = duplicityType;
        this.commonClassification = row.normalizedCopy();
        this.sourceNames = sourceNames;
    }

    public void addBatchStatistics(ClassificationContainer container) {
        if (!batchSize.isPoint()) {
            throw new UnsupportedOperationException("Cannot add batch of specific size to a ranged BatchStatistic");
        }
        if (DuplicityType.UNIQUE.equals(duplicityType) && container.getNumOfUniqueKeys() != batchSize.getLow()) {
            throw new IllegalArgumentException("Incorrect size of ClassificationContainer for this BatchStatistic");
        }
        if (DuplicityType.DUPLICATE.equals(duplicityType) && container.getNumOfAllKeys() != batchSize.getLow()) {
            throw new IllegalArgumentException("Incorrect size of ClassificationContainer for this BatchStatistic");
        }

        if (ClassificationType.POSITIVE.equals(classificationType)) {
            commonClassification = commonClassification.sumRowsNoNormalize(container.getRow());
        } else {
            commonClassification = commonClassification.sumRowsNegativeResults(container.getRow(), sourceNames);
        }

        keyCount += batchSize.getLow();
    }

    public static BatchStatistic combineBatches(List<BatchStatistic> statistics, List<String> sourceNames) {
        Long minSize = Long.MAX_VALUE;
        Long maxSize = Long.MIN_VALUE;
        ClassificationType type = null;
        DuplicityType duplicityType = null;
        for (BatchStatistic batchStatistic : statistics) {
            if (!batchStatistic.batchSize.isPoint()) {
                throw new IllegalArgumentException("Only statistics of fixed size can be merged, not combinations");
            }
            minSize = Long.min(batchStatistic.batchSize.getLow(), minSize);
            maxSize = Long.max(batchStatistic.batchSize.getHigh(), maxSize);
            if (type == null) {
                type = batchStatistic.classificationType;
            } else if (!type.equals(batchStatistic.classificationType)) {
                throw new IllegalArgumentException("Cannot merge two statistics of different ClassificationType");
            }
            if (duplicityType == null) {
                duplicityType = batchStatistic.duplicityType;
            } else if (!duplicityType.equals(batchStatistic.duplicityType)) {
                throw new IllegalArgumentException("Cannot merge two statistics of different DuplicityType");
            }
        }
        BatchStatistic combination = new BatchStatistic(new Range<Long>(minSize, maxSize), type, duplicityType, sourceNames);
        for (BatchStatistic batchStatistic : statistics) {
            combination.commonClassification = combination.commonClassification.sumRowsNoNormalize(
                    batchStatistic.commonClassification.multipleByConstant(batchStatistic.batchSize.getLow()));
            combination.keyCount += batchStatistic.keyCount;
        }
        if (ClassificationType.POSITIVE.equals(type)) {
            combination.commonClassification.normalize();
        }
        return combination;
    }

    public static String rowStatisticHeader(List<String> groupNames, String separator) {
        return String.format("classification"+"%s"+"batch"+"%s"+"batch_size"+"%s"+"key_count"+"%s%s",
                separator, separator, separator, separator, ClassificationRow.groupNamesToFormattedString(groupNames, separator));
    }

    public String toRowStatistic(List<String> groupNamesOrdered, DecimalFormat formatter, String separator) {
        ClassificationRow transformedRow = commonClassification;
        if (batchSize.isPoint() && ClassificationType.NEGATIVE.equals(classificationType)) {
            transformedRow = transformedRow.multipleByConstant(batchSize.getLow());
        }
        return String.format("%s%s%s%s%s%s%s%s%s", classificationType, separator, duplicityType, separator, batchSize, separator,
                keyCount, separator, transformedRow.toFormattedString(groupNamesOrdered, formatter, separator));
    }

    public BatchStatistic normalizedCopy() {
        ClassificationRow normalizedRow = commonClassification.normalizedCopy();
        BatchStatistic copy = new BatchStatistic(new Range<Long>(batchSize.getLow(), batchSize.getHigh()),
                classificationType, duplicityType, sourceNames, normalizedRow);
        copy.keyCount = keyCount;
        return copy;
    }

    public boolean isPositive() {
        return ClassificationType.POSITIVE.equals(classificationType);
    }
}
