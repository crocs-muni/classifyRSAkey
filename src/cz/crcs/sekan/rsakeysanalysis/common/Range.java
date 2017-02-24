package cz.crcs.sekan.rsakeysanalysis.common;

/**
 * @author xnemec1
 * @version 2/24/17.
 */
public class Range<Type> {
    private Type low;
    private Type high;

    public Range(Type low, Type high) {
        this.low = low;
        this.high = high;
    }

    public boolean isPoint() {
        return low.equals(high);
    }

    public Type getLow() {
        return low;
    }

    public Type getHigh() {
        return high;
    }

    @Override
    public String toString() {
        if (isPoint()) return String.format("<%s>", low);
        return String.format("<%s-%s>", low, high);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range<?> range = (Range<?>) o;

        if (low != null ? !low.equals(range.low) : range.low != null) return false;
        return high != null ? high.equals(range.high) : range.high == null;

    }

    @Override
    public int hashCode() {
        int result = low != null ? low.hashCode() : 0;
        result = 31 * result + (high != null ? high.hashCode() : 0);
        return result;
    }
}
