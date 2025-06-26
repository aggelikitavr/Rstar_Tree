public class MBR {
    double[] min;
    double[] max;

    public MBR(double[] min, double[] max) {
        if (min.length != max.length) {
            throw new IllegalArgumentException("Min and max must have same dimensions.");
        }
        this.min = min.clone();
        this.max = max.clone();
    }

    public boolean intersects(MBR other) {
        for (int i = 0; i < min.length; i++) {
            if (this.max[i] < other.min[i] || this.min[i] > other.max[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(double[] coordinates) {
    if (coordinates.length != min.length) {
        throw new IllegalArgumentException("Coordinates must have same dimension as MBR.");
    }
    for (int i = 0; i < min.length; i++) {
        if (coordinates[i] < min[i] || coordinates[i] > max[i]) {
            return false;
        }
    }
    return true;
    }

    public boolean contains(Record record) {
        return contains(record.coordinates);
    }

    public double area() {
        double areaProduct = 1.0;
        for (int i = 0; i < min.length; i++) {
            if (min[i] > max[i]) {
                throw new IllegalArgumentException("min must be <= max for all dimensions");
            }
            areaProduct *= (max[i] - min[i]);
        }
        return areaProduct;
    }

    public double overlap(MBR other) {
        double overlapProduct = 1.0;
        for (int i = 0; i < min.length; i++) {
            double overlapMin = Math.max(this.min[i], other.min[i]);
            double overlapMax = Math.min(this.max[i], other.max[i]);

            if (overlapMax <= overlapMin) {
                return 0.0; // No overlap
            }
            overlapProduct *= (overlapMax - overlapMin);
        }
        return overlapProduct;
    }

    public MBR expandToInclude(MBR other) {
        double[] newMin = new double[min.length];
        double[] newMax = new double[max.length];
        for (int i = 0; i < min.length; i++) {
            newMin[i] = Math.min(this.min[i], other.min[i]);
            newMax[i] = Math.max(this.max[i], other.max[i]);
        }
        return new MBR(newMin, newMax);
    }

    public MBR expandToInclude(double[] coordinates) {
    if (coordinates.length != min.length) {
        throw new IllegalArgumentException("Point must have same dimension as MBR.");
    }
    double[] newMin = min.clone();
    double[] newMax = max.clone();

    for (int i = 0; i < min.length; i++) {
        if (coordinates[i] < newMin[i]) newMin[i] = coordinates[i];
        if (coordinates[i] > newMax[i]) newMax[i] = coordinates[i];
    }

    return new MBR(newMin, newMax);
    }

    public MBR expandToInclude(Record record) {
        return expandToInclude(record.coordinates);
    }


    @Override
    public String toString() {
        return "MBR{" + "min=" + java.util.Arrays.toString(min) +
               ", max=" + java.util.Arrays.toString(max) + '}';
    }
}
